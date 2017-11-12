We've prepared a simple example application. We'll use this application to explore different ways
to encrypt data. Before we dive in, you will need to set up your environment.

# Prerequisites

Install and set up the required components:

* An AWS account
* [The AWS CLI](http://docs.aws.amazon.com/cli/latest/userguide/cli-chap-welcome.html)
* [JDK 1.8](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)
* [Maven 3](https://maven.apache.org/)
* [Git](https://git-scm.com/)
* Optional but recommended: A Java IDE, such as [Eclipse](http://www.eclipse.org/).

## AWS credentials

You'll need to configure the AWS CLI's default credentials to have
_Administrator_ access. Because we'll be deploying a lambda application, we'll
need to create an IAM role for it to use when talking to AWS services, and this
requires Administrator access - Power User is not sufficient. The roles created
are restricted to only having access to the specific resources created as part
of this demo.

For help setting credentials for the CLI, see [the CLI
documentation](http://docs.aws.amazon.com/cli/latest/userguide/cli-config-files.html).

# Deploying the example application

First, check out the application on your local computer:

    git clone https://github.com/aws-samples/reinvent-sid345-workshop-sample.git
    cd reinvent-sid345-workshop-sample

Next, to deploy the application, type:

    mvn deploy


Maven automatically builds the Java backend, uses AWS CloudFormation to deploy AWS resources, and
uploads the Java application as a Lambda function. The initial deployment typically takes 3-5
minutes to complete. You can monitor the progress of the deployment on the [CloudFormation
console](https://ca-central-1.console.aws.amazon.com/cloudformation/home?region=ca-central-1#/stacks?filter=active).

When the deployment completes, you'll see output like this.

    [INFO] Deployment successful.
    [INFO] Deployment URL: https://EXAMPLE.execute-api.ca-central-1.amazonaws.com/test/

To go to the sample application, open the URL in the output.

**WARNING**: This simple demo application does not authenticate its users. Anyone who accesses the application
endpoint can see your data in plaintext on the **Receive data** tab. Do not enter real data in this
application.

## Updating the application

Whenever you change the application, you can use `mvn deploy` to deploy the updates. The
deployment scripts will handle changes to the Java code, HTML, and CloudFormation templates
automatically.

## Cleaning up

When you're done with the workshop, you can shut down the application and clean
up its AWS resources by running:

    mvn deploy -Pdestroy

This command destroys all AWS resources related to the demo application except for the
CloudWatch Log groups that AWS Lambda generated. You can delete those log groups from [the CloudWatch
console](https://ca-central-1.console.aws.amazon.com/cloudwatch/home?region=ca-central-1#logs:).

# Exploring the example application

The application implements a simple order inquiry form that posts messages to
an SQS queue. Initially, these messages are unencrypted.

* Click the **Send data** tab. 
It opens a form that sends encrypted messages to the queue.
Enter some information and click **send**.

* Click the **Receive data** tab. 

  After you enable encryption, you can use this table to view the plaintext and ciphertext versions of
  the messages in the queue.

  * To get the messages that you sent, click the 'fetch messages' button. 
  * To toggle between the raw ciphertext and plaintext, click the radio buttons (all plaintext now). 

* Go to the **Log viewers** tab. This tab has links to useful CloudWatch
Logs. 

  To use this tab, log into the AWS console. Then come back to the tab and click the **show backend
logs in cloudwatch** button. The button opens the AWS CloudWatch console in the tab. You can view
the logs that your Java code generates.

* Click the **Show CloudTrail events for CMK** button. 

  This tab displays the AWS CloudTrail Log events for the KMS Customer Master Key (CMK) that the
  application uses. 

  Because we have not yet implemented encryption, there won't be any events in the log. We'll start
  seeing events after we add encryption. Keep in mind that CloudTrail data is delayed by about 10
  minutes.

# Change the Demo Application

To make sure you are set up correctly, try making some simple changes to the application and
deploying them.

We've created an `EncryptDecrypt` placeholder class for your encryption and data encoding logic. 
You'll see the class under the `src/example/encryption/EncryptDecrypt.java` 
that converts between plaintext and ciphertext.

Before we enable encryption, we're simply sending the JSON to SQS as a raw string. When we
start encrypting, the encryption process will generate random-looking
data that will be mangled if we attempt to pass it as a string. So, as a first step, let's Base64-encode the messages.

If you want to try it yourself, stop here. Otherwise, read the detailed instructions below.

## Detailed steps

Java 8 comes with a handy base64 encoder class that we can use to perform the
conversion. We've already added an import statement for it, so you'll just have
to add the code to use it.

First, in encrypt, change the code to first encode to a byte array instead of a string:

    byte[] plaintext = MAPPER.writeValueAsBytes(formValues);

Then, convert to base64:

    return Base64.getEncoder().encodeToString(plaintext);

Now, we'll do the same in `decrypt`. Decode to a byte array:

    byte[] ciphertextBytes = Base64.getDecoder().decode(ciphertext);

Then, decode the JSON:

    return MAPPER.readTree(ciphertextBytes);

After you've made the changes, use `mvn deploy` to deploy them. Then try sending
and receiving a sample message. Now, when you use the **Ciphertext** radio button on the **Receive data** tab, you
should see Base64-encoded ciphertext of the message.
