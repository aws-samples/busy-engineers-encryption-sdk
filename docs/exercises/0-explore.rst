
.. _Exercise 0:

*******************
Exercise 0: Explore
*******************

We've prepared a simple example application. We'll use this application to explore different ways
to encrypt data. Before we dive in, you will need to set up your environment.

.. note::

    Note for workshop attendees

    If you are working through these examples in a Cloud9 workshop setting, your environment is already configured.

You may skip the setup portions and proceed directly to :ref:`Deploying the example application`.

Prerequisites
=============

Install and set up the required components:

* An AWS account
* `The AWS CLI`_
* `JDK 1.8`_
* `Maven 3`_
* `Git`_

The :ref:`EC2 quickstart` section will walk you through setting these up.

AWS credentials
---------------

You'll need to configure the AWS CLI's default credentials to have *Administrator* access.

We'll be deploying a Lambda application, so we'll need to create an IAM role for it to use when talking to AWS
services, and this requires Administrator access - Power User is not sufficient.

The roles created are restricted to only having access to the specific resources created as part
of this demo.

For help setting credentials for the CLI, see the `AWS CLI documentation`_.

.. _EC2 quickstart:

EC2 quickstart
==============

If you'd like to set up an EC2 instance that is set up to use as a development
environment for this demo, here's some quickstart instructions to get you set
up.

If you've already got a working Java development environment, feel free to skip
ahead to the next section where we'll deploy the sample application.

Configuring and launching the instance
--------------------------------------

First, create an Administrator access role that the EC2 instance will use to
access your account.

**Note:** Because this is granting a high level of privileges to the instance, we recommend doing this in a test
account.

#. Log in to the AWS Console.
#. Go to `the IAM console's Roles section <https://console.aws.amazon.com/iam/home?region=ca-central-1#/roles>`_.
#. Click the "Create Role" button.
#. Under "Choose the service that will use this role", select "EC2",
   then select "EC2" for the use case and proceed to the next page.
#. Select ``AdministratorAccess``, and proceed to the next page.
#. Set some easy-to-remember name for the role such as "sid345-admin".

Now that you have a role created, we'll deploy a Linux instance to use as our
launching point.

Open `the EC2 console for ca-central-1
<https://ca-central-1.console.aws.amazon.com/ec2/v2/home?region=ca-central-1#Instances:sort=instanceId>`_.

If you've not launched any instances here before, you'll first need to either
`create a new key pair
<http://docs.aws.amazon.com/AWSEC2/latest/UserGuide/ec2-key-pairs.html#having-ec2-create-your-key-pair>`_
or `import an existing ssh key
<http://docs.aws.amazon.com/AWSEC2/latest/UserGuide/ec2-key-pairs.html#how-to-generate-your-own-key-and-import-it-to-aws>`_
using the instructions at those links.

Once you have the key pair set up, we can launch an instance.

#. Click the blue 'Launch Instance' button.
#. Select the 'Amazon Linux AMI 2018.03.0 (HVM), SSD Volume Type' AMI.
#. Click 'Configure Instance Details' and make sure 'Auto-assign Public IP' is **Enabled**.
#. **In 'IAM Role', select the role you created above.** ("sid345-admin", or your preferred name)
#. Click 'Review and Launch'.
#. Click 'Launch'.
#. In the provided dialog, select the keypair you just created or imported.
#. Click 'Launch Instances'.

Setting up the development environment
--------------------------------------

Once the instance launches, you'll see it in the `instance list
<https://ca-central-1.console.aws.amazon.com/ec2/v2/home?region=ca-central-1#Instances>`_.

Copy the public DNS hostname. You can then log into this instance using
username ``ec2-user`` and the keypair you created before.

If this is your first time using EC2, see the `EC2 getting started documentation
<http://docs.aws.amazon.com/AWSEC2/latest/UserGuide/EC2_GetStarted.html>`_ for more detail.

Once you're logged in, use ``yum`` to upgrade Java and install git:

.. code-block:: bash

    sudo yum install java-1.8.0-openjdk-devel git

Use ``alternatives`` to ensure your new Java version is the default as follows:

.. code-block:: bash

    sudo /usr/sbin/alternatives  --config java

For example:

.. code-block:: bash

    [ec2-user@ip-172-31-2-67 ~]$ sudo /usr/sbin/alternatives  --config java

    There is 1 program that provides 'java'.

    Selection    Command
    -----------------------------------------------
    *+ 1           java-1.8.0-openjdk.x86_64 (/usr/lib/jvm/java-1.8.0-openjdk-1.8.0.181-3.b13.amzn2.x86_64/jre/bin/java)

    Enter to keep the current selection[+], or type selection number

At the prompt select the number corresponding to 1.8.0 (``1`` here).

Next we'll fetch Maven:

.. code-block:: bash

    wget https://archive.apache.org/dist/maven/maven-3/3.5.2/binaries/apache-maven-3.5.2-bin.tar.gz
    wget https://archive.apache.org/dist/maven/maven-3/3.5.2/binaries/apache-maven-3.5.2-bin.tar.gz.sha1

Since Maven uses an unsecured connection to download the Maven binaries themselves, it's good practice to check the hash of the binaries:

.. code-block:: bash

    [ec2-user@ip-10-0-0-137 ~]$ sha1sum apache-maven-3.5.2-bin.tar.gz; cat apache-maven-3.5.2-bin.tar.gz.sha1; echo
    190dcebb8a080f983af4420cac4f3ece7a47dd64  apache-maven-3.5.2-bin.tar.gz
    190dcebb8a080f983af4420cac4f3ece7a47dd64

Make sure the two hashes match before proceeding.

Once you've verified the integrity of maven, we'll need to unpack it and add it to our path:

.. code-block:: bash

    tar xzvf apache-maven-3.5.2-bin.tar.gz
    PATH=$PWD/apache-maven-3.5.2/bin:$PATH
    echo "PATH=$PWD/apache-maven-3.5.2/bin:$PATH" >> ~/.bash_profile

At this point you should have a Linux system that can deploy the example application with the instructions below.

To edit files, the ``nano`` editor is built-in. You can also install or use another editor of your choice, such as ``vim``
or ``emacs``.

.. _Deploying the example application:

Deploying the example application
=================================

First, check out the application on your local computer:

.. code-block:: bash

    git clone https://github.com/aws-samples/reinvent-sid345-workshop-sample.git
    cd reinvent-sid345-workshop-sample

Next, to deploy the application, type:

    mvn deploy


Maven automatically builds the Java backend, uses AWS CloudFormation to deploy AWS resources, and
uploads the Java application as a Lambda function. The initial deployment typically takes 3-5
minutes to complete. You can monitor the progress of the deployment on the `CloudFormation console
<https://ca-central-1.console.aws.amazon.com/cloudformation/home?region=ca-central-1#/stacks?filter=active>`_.

When the deployment completes, you'll see output like this.

.. code-block:: bash

    [INFO] Deployment successful.
    [INFO] Deployment URL: https://EXAMPLE.execute-api.ca-central-1.amazonaws.com/test/

To go to the sample application, open the URL in the output.

.. warning::

    This simple demo application does not authenticate its users. Anyone who accesses the application
    endpoint can see your data in plaintext on the **Receive data** tab. Do not enter real data in this
    application.

Updating the application
------------------------

Whenever you change the application, you can use ``mvn deploy`` to deploy the updates. The
deployment scripts will handle changes to the Java code, HTML, and CloudFormation templates
automatically.

Cleaning up
-----------

When you're done with the workshop, you can shut down the application and clean
up its AWS resources by running:

.. code-block:: bash

    mvn deploy -Pdestroy

This command destroys all AWS resources related to the demo application except for the
CloudWatch Log groups that AWS Lambda generated. You can delete those log groups from
`the CloudWatch console <https://ca-central-1.console.aws.amazon.com/cloudwatch/home?region=ca-central-1#logs:>`_.

Exploring the example application
=================================

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

* Go to the **Log viewers** tab. This tab has links to useful CloudWatch Logs.

  To use this tab, log into the AWS console. Then come back to the tab and click the **show backend
  logs in cloudwatch** button. The button opens the AWS CloudWatch console in the tab. You can view
  the logs that your Java code generates.

* Click the **Show CloudTrail events for CMK** button.

  This tab displays the AWS CloudTrail Log events for the KMS Customer Master Key (CMK) that the
  application uses.

  Because we have not yet implemented encryption, there won't be any events in the log. We'll start
  seeing events after we add encryption. Keep in mind that CloudTrail data is delayed by about 10
  minutes.

Change the Demo Application
===========================

To make sure you are set up correctly, try making some simple changes to the application and
deploying them.

We've created an ``EncryptDecrypt`` placeholder class for your encryption and data encoding logic.
You'll see the class under the ``webapp/src/main/java/example/encryption/EncryptDecrypt.java``
that converts between plaintext and ciphertext.

Before we enable encryption, we're simply sending the JSON to SQS as a raw string. When we
start encrypting, the encryption process will generate random-looking
data that will be mangled if we attempt to pass it as a string. So, as a first step, let's Base64-encode the messages.

If you want to try it yourself, stop here. Otherwise, read the detailed instructions below.

Detailed steps
--------------

Java 8 comes with a handy base64 encoder class that we can use to perform the
conversion. We've already added an import statement for it, so you'll just have
to add the code to use it.

First, in encrypt, change the code to first encode to a byte array instead of a string:

.. code-block:: java

    byte[] plaintext = MAPPER.writeValueAsBytes(formValues);

Then, convert to base64:

.. code-block:: java

    return Base64.getEncoder().encodeToString(plaintext);

Now, we'll do the same in ``decrypt``. Decode to a byte array:

.. code-block:: java

    byte[] ciphertextBytes = Base64.getDecoder().decode(ciphertext);

Then, decode the JSON:

.. code-block:: java

    return MAPPER.readTree(ciphertextBytes);

After you've made the changes, use ``mvn deploy`` to deploy them. Then try sending
and receiving a sample message. Now, when you use the **Ciphertext** radio button on the **Receive data** tab, you
should see Base64-encoded ciphertext of the message.

.. _The AWS CLI: http://docs.aws.amazon.com/cli/latest/userguide/cli-chap-welcome.html
.. _JDK 1.8: http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html
.. _Maven 3: https://maven.apache.org/
.. _Git: https://git-scm.com/
.. _AWS CLI documentation: http://docs.aws.amazon.com/cli/latest/userguide/cli-config-files.html
