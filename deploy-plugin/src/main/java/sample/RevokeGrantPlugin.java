/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import com.amazonaws.regions.Regions;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClientBuilder;
import com.amazonaws.services.kms.AWSKMS;
import com.amazonaws.services.kms.AWSKMSClientBuilder;
import com.amazonaws.services.kms.model.DescribeKeyRequest;
import com.amazonaws.services.kms.model.DescribeKeyResult;
import com.amazonaws.services.kms.model.RevokeGrantRequest;
import com.amazonaws.services.kms.model.RevokeGrantResult;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;
import com.amazonaws.services.securitytoken.model.GetCallerIdentityRequest;
import com.amazonaws.services.securitytoken.model.GetCallerIdentityResult;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;


@Mojo(name="revoke-grant")
public class RevokeGrantPlugin extends AbstractMojo {
    @Parameter(property="grantId.path") protected String grantIdPath;
    
    @Override
    public void execute() throws MojoFailureException {
        try {
            File grantIdFile = new File(grantIdPath);
            if (!grantIdFile.exists()) {
                getLog().warn("Grant has not yet been assigned (do you need to 'assign-grant'?).");
                return;
            }
            
            String grantId;
            
            //Load the grant ID to revoke
            try (BufferedReader grantIdReader = new BufferedReader(new FileReader(grantIdFile))) {
                grantId = grantIdReader.readLine();
            }
            
            // Set up clients
            AWSSecurityTokenService stsClient = AWSSecurityTokenServiceClientBuilder.defaultClient();
            AWSKMS kmsClient = AWSKMSClientBuilder.standard().withRegion(Regions.US_WEST_2).build();
            AmazonIdentityManagement iamClient = AmazonIdentityManagementClientBuilder.defaultClient();
            
            // Get the accountId we're running in
            GetCallerIdentityResult identityResult = stsClient.getCallerIdentity(new GetCallerIdentityRequest());
            String accountId = identityResult.getAccount();
            
            // Map the alias to the real CMK ARN
            String aliasArn = "arn:aws:kms:us-west-2:" + accountId + ":alias/busy-engineers-workshop-us-west-2-key";
            DescribeKeyRequest descRequest = new DescribeKeyRequest()
                                                    .withKeyId(aliasArn);
            DescribeKeyResult descResult = kmsClient.describeKey(descRequest);
            String keyArn = descResult.getKeyMetadata().getArn();
            getLog().info("Key ARN " + keyArn + " for alias " + aliasArn);
            
            // Revoke the grant on the CMK
            RevokeGrantRequest grantRequest = new RevokeGrantRequest()
                                                    .withKeyId(keyArn)
                                                    .withGrantId(grantId);

            RevokeGrantResult grantResult = kmsClient.revokeGrant(grantRequest);

            getLog().info("Revoked grant " + grantId);
            
            grantIdFile.delete();
            getLog().info("Cleaned up grant ID file.");
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new MojoFailureException("Unexpected error", e);
        }
    }
}
