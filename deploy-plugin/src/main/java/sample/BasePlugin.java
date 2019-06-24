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

import java.util.Objects;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudformation.model.DescribeStackResourceRequest;
import com.amazonaws.services.cloudformation.model.DescribeStackResourceResult;
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.services.cloudformation.model.DescribeStacksResult;
import com.amazonaws.services.cloudformation.model.Stack;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;

@Mojo(name="base-plugin-do-not-invoke")
abstract class BasePlugin extends AbstractMojo {
    @Parameter(property="cfdeploy.stackName") protected String stackName;

    // The region to deploy to
    @Parameter(property="cfdeploy.region") protected String region;

    protected AmazonCloudFormation cfn;
    protected AmazonS3 s3;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        cfn = AmazonCloudFormationClient.builder()
                                        .withRegion(region)
                                        .build();
        s3 = AmazonS3Client.builder()
                           .withRegion(region)
                           .build();
    }

    protected boolean canUpdate(final String stackStatus) {
        switch (stackStatus) {
            case "CREATE_COMPLETE":
            case "ROLLBACK_COMPLETE":
            case "UPDATE_COMPLETE":
            case "UPDATE_ROLLBACK_COMPLETE":
                return true;
            default:
                return false;
        }
    }

    protected boolean isFinished(final String stackStatus) {
        return !stackStatus.endsWith("_IN_PROGRESS");
    }

    protected Stack findStack() {
        try {
            return getStack(stackName);
        } catch (AmazonServiceException e) {
            if (e.getErrorCode().equals("ValidationError")) {
                return null;
            } else {
                throw e;
            }
        }
    }

    protected Stack waitForUpdateComplete(final String stackId) throws InterruptedException, MojoFailureException {
        Stack state = getStack(stackId);

        while (!isFinished(state.getStackStatus())) {
            Thread.sleep(1000);

            Stack newState = getStack(stackId);
            if (!newState.getStackStatus().equals(state.getStackStatus())) {
                getLog().info("Stack state changed: " + newState.getStackStatus());
            }

            state = newState;
        }

        String status = state.getStackStatus();
        if (!Objects.equals(status, "CREATE_COMPLETE") && !Objects.equals(status, "UPDATE_COMPLETE")) {
            throw new MojoFailureException(String.format("Stack update failed: %s (%s)",
                                            state.getStackStatus(), state.getStackStatusReason()));
        }

        getLog().info("Stack update completed");

        return state;
    }

    protected Stack getStack(final String stackId) {
        DescribeStacksRequest describeStacksRequest = new DescribeStacksRequest().withStackName(stackId);
        DescribeStacksResult result = cfn.describeStacks(describeStacksRequest);

        Stack stack = result.getStacks().get(0);

        if (!stack.getStackName().equals(stackName)) {
            getLog().error("Got the wrong stack!");
            getLog().error("Request: " + describeStacksRequest);
            getLog().error("Result: " + result);
            throw new RuntimeException("got wrong stack");
        }

        return stack;
    }

    protected String getBucketName(final Stack stack) {
        // Find the S3 bucket name
        DescribeStackResourceResult dsrResult = cfn.describeStackResource(
                new DescribeStackResourceRequest()
                    .withStackName(stack.getStackId())
                    .withLogicalResourceId("S3Bucket")
        );

        String bucketName = dsrResult.getStackResourceDetail().getPhysicalResourceId();

        getLog().info("S3 bucket name: " + bucketName);

        return bucketName;
    }
}
