package sample;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClientBuilder;
import com.amazonaws.services.identitymanagement.model.Role;
import com.amazonaws.services.kms.AWSKMS;
import com.amazonaws.services.kms.AWSKMSClientBuilder;
import com.amazonaws.services.kms.model.CreateGrantRequest;
import com.amazonaws.services.kms.model.CreateGrantResult;
import com.amazonaws.services.kms.model.DescribeKeyRequest;
import com.amazonaws.services.kms.model.DescribeKeyResult;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;
import com.amazonaws.services.securitytoken.model.GetCallerIdentityRequest;
import com.amazonaws.services.securitytoken.model.GetCallerIdentityResult;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.List;
import java.util.stream.Collectors;


@Mojo(name="assign-grant")
public class AssignGrantPlugin extends AbstractMojo {
    @Parameter(property="grantId.path") protected String grantIdPath;
    
    @Override
    public void execute() throws MojoFailureException {
        try {
            File grantIdFile = new File(grantIdPath);
            if (grantIdFile.exists()) {
                getLog().warn("Grant has already been assigned (do you need to 'revoke-grant'?).");
                return;
            }

            // Set up clients
            AWSSecurityTokenService stsClient = AWSSecurityTokenServiceClientBuilder.defaultClient();
            AWSKMS kmsClient = AWSKMSClientBuilder.standard().withRegion(Regions.US_WEST_2).build();
            AmazonIdentityManagement iamClient = AmazonIdentityManagementClientBuilder.defaultClient();

            // Get the accountId we're running in
            GetCallerIdentityResult identityResult = stsClient.getCallerIdentity(new GetCallerIdentityRequest());
            String accountId = identityResult.getAccount();
            
            // Get the Lambda-associated role to be our grantee principal
            List<Role> allRoles = iamClient.listRoles().getRoles();
            List<Role> filteredRoles = allRoles.stream().filter(r -> r.getRoleName().contains("busy-engineers-ee-iam-LambdaRole-")).collect(Collectors.toList());
            if (filteredRoles.isEmpty()) {
                throw new MojoFailureException("Unable to find Lambda role ARN to assign grant.");
            }
            String lambdaRoleArn = filteredRoles.get(0).getArn();
            getLog().info("Lambda role ARN: " + lambdaRoleArn);
            
            // Map the alias to the real CMK ARN
            String aliasArn = "arn:aws:kms:us-west-2:" + accountId + ":alias/busy-engineers-workshop-us-west-2-key";
            DescribeKeyRequest descRequest = new DescribeKeyRequest()
                                                    .withKeyId(aliasArn);
            DescribeKeyResult descResult = kmsClient.describeKey(descRequest);
            String keyArn = descResult.getKeyMetadata().getArn();
            getLog().info("Key ARN " + keyArn + " for alias " + aliasArn);
            
            // Actually create the grant on the CMK
            CreateGrantRequest grantRequest = new CreateGrantRequest()
                                                    .withKeyId(keyArn)
                                                    .withOperations("Encrypt", "Decrypt")
                                                    .withGranteePrincipal(lambdaRoleArn);

            CreateGrantResult grantResult = kmsClient.createGrant(grantRequest);
            String grantId = grantResult.getGrantId();
            getLog().info("Created grant with ID " + grantId);
            
            //Save the grant to revoke later
            try (BufferedWriter grantIdWriter = new BufferedWriter(new FileWriter(grantIdFile))) {
                grantIdWriter.write(grantId);
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new MojoFailureException("Unexpected error", e);
        }
    }
}
