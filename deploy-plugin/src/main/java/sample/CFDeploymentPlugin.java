/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance with
 * the License. A copy of the License is located at
 *
 *     http://aws.amazon.com/apache2.0/
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */
package sample;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudformation.model.Capability;
import com.amazonaws.services.cloudformation.model.CreateStackRequest;
import com.amazonaws.services.cloudformation.model.DescribeStackResourceRequest;
import com.amazonaws.services.cloudformation.model.DescribeStackResourceResult;
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.services.cloudformation.model.DescribeStacksResult;
import com.amazonaws.services.cloudformation.model.Stack;
import com.amazonaws.services.cloudformation.model.UpdateStackRequest;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;

@Mojo(name="cfDeploy")
public class CFDeploymentPlugin extends BasePlugin {
    // The initial bootstrap Cloudformation template file, which is initially used to set up the S3 bucket in which
    // the lambda JAR and static assets will be deployed.
    // Because the JAR must be uploaded before creating the actual lambda function, we need to set this up before
    // creating the rest of the AWS resources.
    @Parameter(property="cfdeploy.bootstrapTemplate") protected String bootstrapTemplate;
    // The "real" CF template. Note that we need to make sure that the S3 bucket resource has the same cloudformation
    // resource name in this template file as well.
    @Parameter(property="cfdeploy.mainTemplate") protected String mainTemplate;

    @Parameter(property="cfdeploy.jarPath") protected String jarPath;

    @Parameter(property="cfdeploy.assetsDirectory")
    private String assetsDirectory;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        super.execute();

        try {
            Stack stack = findStack();

            if (stack == null) {
                bootstrap();
            } else if (!canUpdate(stack.getStackStatus())) {
                throw new MojoFailureException(
                        "The stack's current status (" + stack.getStackStatus() + ") does not allow updates");
            } else {
                updateStack(stack);
            }
        } catch (MojoFailureException | RuntimeException | Error e) {
            throw e;
        } catch (Exception e) {
            throw new MojoFailureException("Unexpected error", e);
        }
    }

    private void bootstrap() throws Exception {
        getLog().info("Performing initial bootstrap: Creating S3 bucket");

        // Create the stack using the bootstrap template initially
        String stackId = cfn.createStack(
                new CreateStackRequest()
                        .withTemplateBody(readFileAsString(bootstrapTemplate))
                        .withStackName(stackName)
        ).getStackId();

        Stack stack = waitForUpdateComplete(stackId);

        updateStack(stack);
    }

    private String readFileAsString(final String mainTemplate) throws IOException {
        byte[] buf = readFileAsBytes(mainTemplate);

        return new String(buf, StandardCharsets.UTF_8);
    }

    private byte[] readFileAsBytes(final String mainTemplate) throws IOException {
        byte[] buf = new byte[4096];

        try (FileInputStream is = new FileInputStream(mainTemplate)) {
            int offset = 0;
            int readBytes = 0;

            while (-1 != (readBytes = is.read(buf, offset, buf.length - offset))) {
                offset += readBytes;

                if (offset == buf.length) {
                    buf = Arrays.copyOf(buf, Math.toIntExact(((long)buf.length * 3) / 2));
                }
            }

            buf = Arrays.copyOf(buf, offset);
        }

        return buf;
    }

    private void updateStack(final Stack stack) throws Exception {
        String bucketName = getBucketName(stack);

        updateAssets(bucketName);

        String jarKey = uploadAsset(bucketName, jarPath);

        updateCloudformation(stack, jarKey);

        getLog().info("Deployment successful.");
        showEndpoint(stack);
    }

    private void showEndpoint(Stack stack) {
        String restAPIId = cfn.describeStackResource(
                new DescribeStackResourceRequest()
                    .withStackName(stack.getStackId())
                    .withLogicalResourceId("RestAPI")
        ).getStackResourceDetail().getPhysicalResourceId();

            String stage = cfn.describeStackResource(
                new DescribeStackResourceRequest()
                        .withStackName(stack.getStackId())
                        .withLogicalResourceId("ApiStage")
        ).getStackResourceDetail().getPhysicalResourceId();

        getLog().info(String.format(
                "Deployment URL: https://%s.execute-api.%s.amazonaws.com/%s/",
                restAPIId, region, stage
        ));
    }

    private void updateCloudformation(
            Stack stack,
            String jarKey
    ) throws Exception {
        String templateBody = readFileAsString(mainTemplate);

        // We change the logical ID of the stack deployment each time we update the stack; this forces API Gateway to
        // reflect any configuration changes
        templateBody = templateBody.replaceAll("%UNIQUE%",
                                               UUID.randomUUID().toString().replaceAll("-", ""));

        UpdateStackRequest request = new UpdateStackRequest()
                .withStackName(stack.getStackId())
                .withTemplateBody(templateBody)
                .withCapabilities(Capability.CAPABILITY_IAM, Capability.CAPABILITY_NAMED_IAM)
                .withParameters(
                        new com.amazonaws.services.cloudformation.model.Parameter()
                                .withParameterKey("CodeKey")
                                .withParameterValue(jarKey)
                );

        cfn.updateStack(request);

        waitForUpdateComplete(stack.getStackId());
    }

    private String uploadAsset(final String bucketName, String path) throws Exception {
        // Load the file and build an S3 key including its base64
        byte[] fileBytes = readFileAsBytes(path);

        // Generate a key for upload to the S3 bucket
        Path p = new File(path).toPath();
        String basename = p.getName(p.getNameCount() - 1).toString();
        String fileKey = basename + "." + Base64.getEncoder().encodeToString(
                MessageDigest.getInstance("SHA-384").digest(fileBytes)
        );

        ObjectMetadata objectMetadata = new ObjectMetadata();
        objectMetadata.setContentLength(fileBytes.length);
        s3.putObject(
                new PutObjectRequest(
                        bucketName,
                        fileKey,
                        new ByteArrayInputStream(fileBytes),
                        objectMetadata
                )
        );

        return fileKey;
    }

    private void updateAssets(String bucketName) throws IOException {
        Path basePath = new File(assetsDirectory).toPath();
        ExecutorService executorService = Executors.newCachedThreadPool(
                runnable -> {
                    Thread t = new Thread(runnable);
                    t.setDaemon(true);
                    return t;
                }
        );

        getLog().info("Uploading assets...");

        try {
            ArrayList<Future<Void>> futures = new ArrayList<>();

            Files.walk(basePath)
                 .parallel()
                 .forEach(
                         path -> {
                             if (!Files.isRegularFile(path)) {
                                 return;
                             }

                             Path relativePath = basePath.relativize(path);

                             // Skip hidden files/directories
                             for (int i = 0; i < relativePath.getNameCount(); i++) {
                                 if (relativePath.getName(i).toString().startsWith(".")) {
                                     return;
                                 }
                             }

                             futures.add(executorService.submit(() -> {
                                 uploadAsset(bucketName, path, relativePath);
                                 getLog().info("Uploaded asset: " + path);
                                 return null;
                             }));
                         }
                 );

            for (Future<Void> f : futures) {
                try {
                    f.get();
                } catch (ExecutionException e) {
                    throw e.getCause();
                }
            }
        } catch (Exception e) {
            if (e.getCause() instanceof IOException) {
                throw (IOException)e.getCause();
            } else if (e instanceof RuntimeException) {
                throw (RuntimeException)e;
            }
            throw new RuntimeException(e);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        } finally {
            executorService.shutdownNow();
        }
    }

    private void uploadAsset(final String bucketName, final Path path, final Path relativePath) {
        String contentType = guessContentType(relativePath.toString());

        try {
            try (FileInputStream fis = new FileInputStream(path.toFile())) {
                ObjectMetadata metadata = new ObjectMetadata();
                metadata.setContentLength(Files.size(path));
                metadata.setContentType(contentType);

                PutObjectRequest request = new PutObjectRequest(
                        bucketName, "static-assets/" + relativePath, fis, metadata
                );
                request.setCannedAcl(CannedAccessControlList.PublicRead);

                s3.putObject(request);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String guessContentType(final String filepath) {
        if (filepath.endsWith(".js")) {
            return "application/javascript";
        } else if (filepath.endsWith(".css")) {
            return "text/css";
        } else if (filepath.endsWith(".html")) {
            return "text/html";
        } else {
            // unknown file extension, let the browser guess
            return null;
        }
    }
}
