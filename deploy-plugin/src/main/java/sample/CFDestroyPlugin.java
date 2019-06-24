/*
 * Copyright 2017-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import java.util.List;
import java.util.stream.Collectors;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudformation.model.DeleteStackRequest;
import com.amazonaws.services.cloudformation.model.Stack;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.DeleteObjectsRequest;
import com.amazonaws.services.s3.model.DeleteObjectsResult;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.ListVersionsRequest;
import com.amazonaws.services.s3.model.MultiObjectDeleteException;
import com.amazonaws.services.s3.model.VersionListing;

@Mojo(name="destroy")
public class CFDestroyPlugin extends BasePlugin {
    @Override public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            super.execute();

            Stack stack = findStack();
            if (stack == null) {
                getLog().info("Stack does not exist; nothing to destroy.");
                return;
            }

            getLog().info("Waiting for any pending deployments to complete...");
            stack = waitUntilFinished(stack);

            if (!stack.getStackStatus().equals("DELETE_COMPLETE")) {
                getLog().info("Attempting to delete stack...");
                cfn.deleteStack(new DeleteStackRequest().withStackName(stack.getStackId()));
            }

            stack = waitUntilFinished(stack);

            if (stack.getStackStatus().equals("DELETE_FAILED")) {
                getLog().info("Stack deletion failed; purging S3 bucket and trying again");
                purgeBucket(stack);

                getLog().info("Attempting to delete stack...");
                cfn.deleteStack(new DeleteStackRequest().withStackName(stack.getStackId()));

                stack = waitUntilFinished(stack);
            }

            if (stack.getStackStatus().equals("DELETE_COMPLETE")) {
                getLog().info("Stack deleted successfully.");
            } else {
                throw new MojoFailureException("Failed to delete stack; stack is in state "
                                                       + stack.getStackStatus() + " (" + stack.getStackStatusReason() + ")");
            }
        } catch (RuntimeException | MojoFailureException e) {
            throw e;
        } catch (Exception e) {
            throw new MojoFailureException("Unexpected error", e);
        }
    }

    private void purgeBucket(final Stack stack) throws MojoFailureException {
        String bucketName = getBucketName(stack);

        ListVersionsRequest request = new ListVersionsRequest().withBucketName(bucketName);
        VersionListing result;

        do {
            result = s3.listVersions(request);
            request.setKeyMarker(result.getNextKeyMarker());
            request.setVersionIdMarker(result.getNextVersionIdMarker());

            if (!result.getVersionSummaries().isEmpty()) {
                List<DeleteObjectsRequest.KeyVersion> toDelete =
                        result.getVersionSummaries().stream().map(
                                summary -> new DeleteObjectsRequest.KeyVersion(summary.getKey(), summary.getVersionId())
                        ).collect(Collectors.toList());

                DeleteObjectsRequest dor = new DeleteObjectsRequest(bucketName);
                dor.setKeys(toDelete);
                dor.setQuiet(true);

                DeleteObjectsResult deleteResult = s3.deleteObjects(dor);;

                for (final DeleteObjectsResult.DeletedObject failedObject : deleteResult.getDeletedObjects()) {
                    getLog().error("Failed to delete: " + failedObject.getKey());
                }

                if (!deleteResult.getDeletedObjects().isEmpty()) {
                    throw new MojoFailureException("Failed to delete some objects from the S3 bucket");
                }
            }

        } while (result.isTruncated());

    }

    private Stack waitUntilFinished(Stack stack) throws InterruptedException {
        stack = getStack(stack.getStackId());

        while (!isFinished(stack.getStackStatus())) {
            Thread.sleep(1000);

            stack = getStack(stack.getStackId());
        }

        return stack;
    }
}
