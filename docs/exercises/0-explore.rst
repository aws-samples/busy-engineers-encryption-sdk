
.. _Exercise 0:

*******************
Exercise 0: Explore
*******************

We've prepared a simple example application. We'll use this application to explore different ways
to encrypt data. Before we dive in, you will need to set up your environment.

Check out the first application branch for the language of your choice:

.. tabs::

    .. group-tab:: Java

        .. code-block:: bash

            git checkout exercise-0-start

    .. group-tab:: Python

        .. code-block:: bash

            git checkout exercise-0-start-python

And deploy using the appropriate :ref:`Build tool commands`.

.. _Build tool commands:

Build tool commands
===================

These commands will be used throughout these exercises to build, deploy, update, and destroy
the example application.

.. tabs::

    .. group-tab:: Java

        To build locally and deploy, including any updates:

        .. code-block:: bash

            mvn deploy

    .. group-tab:: Python

        To build locally and deploy, including any updates:

        .. code-block:: bash

            tox -e deploy

        The actual build needs to happen on an Amazon Linux platform with Python 3.6.
        Everything else can be done on any host with ``tox``, ``bash``, and ``ssh``.

    .. tab:: Python (Bonus)

        If you want to run the build on another computer, you can use this build command:

        .. code-block:: bash

            tox -e deploy-remote-build -- {HOSTNAME} {SSH KEY FILE}

Our build tools automatically build the Lambda, use AWS CloudFormation to deploy AWS resources, and
uploads the built application as a Lambda function. The initial deployment typically takes 3-5
minutes to complete. You can monitor the progress of the deployment on the `CloudFormation console
<https://eu-west-1.console.aws.amazon.com/cloudformation/home?region=eu-west-1#/stacks?filter=active>`_.

When the deployment completes, you'll see output like this.

.. tabs::

    .. group-tab:: Java

        .. code-block:: bash

            [INFO] Deployment successful.
            [INFO] Deployment URL: https://EXAMPLE.execute-api.eu-west-1.amazonaws.com/test/

    .. group-tab:: Python

        .. code-block:: bash

            Endpoint available at: https://EXAMPLE.execute-api.eu-west-1.amazonaws.com/test/

To go to the sample application, open the URL in the output.

.. warning::

    This simple demo application does not authenticate its users. Anyone who accesses the application
    endpoint can see your data in plaintext on the **Receive data** tab. Do not enter real data in this
    application.

.. _Updating the example application:

Updating the example application
================================

Whenever you change the application, you can use the appropriate :ref:`Build tool commands` to deploy
the updates. The deployment scripts will handle changes to the code, HTML, and CloudFormation templates
automatically.

Cleaning up
-----------

When you're done with the workshop, you can shut down the application and clean
up its AWS resources using the instructions in the :ref:`Clean up` section.

This destroys all AWS resources related to the demo application except for the
CloudWatch Log groups that AWS Lambda generated. You can delete those log groups from
`the CloudWatch console <https://eu-west-1.console.aws.amazon.com/cloudwatch/home?region=eu-west-1#logs:>`_.

.. _Exploring the example application:

Exploring the example application
=================================

The example application is a simple order inquiry form. It allows posting order information summaries, and receiving
and viewing summaries that have already been posted.

Under the hood, this application uses SQS for message passing, API Gateway to provide a web API, and Lambda to handle
the actual request processing.

Initially, the order summaries are unencrypted. In this workshop, using AWS KMS, you will add encryption to protect the
confidentiality and integrity of these messages.

* Click the **Send data** tab.

  It opens a form that sends messages to the queue.
  Enter some information and click **send**.

* Click the **Receive data** tab.

  After you enable encryption, you can use this table to view the plaintext and ciphertext versions of
  the messages in the queue.

  * To get the messages that you sent, click the 'fetch messages' button.
  * To toggle between the raw ciphertext and plaintext, click the radio buttons (all plaintext now).

* Go to the **Log viewers** tab. This tab has links to useful CloudWatch Logs.

  To use this tab, log into the AWS console. Then come back to the tab and click the **Show Backend
  Logs in CloudWatch** button. The button opens the AWS CloudWatch console in the tab. You can view
  the logs that your Java code generates.

* Click the **Show CloudTrail events for CMK** button.

  This tab displays the AWS CloudTrail Log events for the KMS Customer Master Key (CMK) that the
  application uses.

  Because we have not yet implemented encryption, there won't be any events in the log. We'll start
  seeing events after we add encryption.

  Keep in mind that CloudTrail data is delayed by about 10 minutes.

Change the Example Application
==============================

To make sure you are set up correctly, try making some simple changes to the application and
deploying them.

.. tabs::

    .. group-tab:: Java

        We've created an ``EncryptDecrypt`` placeholder class for your encryption and data encoding logic.
        You'll see the class under ``webapp/src/main/java/example/encryption/EncryptDecrypt.java``.
        After you've worked through this workshop, this class will convert between plaintext and ciphertext.

    .. group-tab:: Python

        We've created an ``EncryptDecrypt`` placeholder class for your encryption and data encoding logic.
        You'll see the class under ``src/reinvent_sid345/encrypt_decrypt.py``.
        After you've worked through this workshop, this class will convert between plaintext and ciphertext.


Before we enable encryption, we're simply sending the JSON to SQS as a raw string. When we
start encrypting, the encryption process will generate random-looking
data that will be mangled if we attempt to pass it as a string. So, as a first step, let's Base64-encode the messages.

If you want to try it yourself, stop here. Otherwise, read the detailed instructions below.

Detailed steps
--------------

.. tabs::

    .. group-tab:: Java

        Java 8 comes with a handy base64 encoder class that we can use to perform the
        conversion. We've already added an import statement for it, so you'll just have
        to add the code to use it.

        First, in ``encrypt``, add code to first encode to a byte array instead of a string:

        .. code-block:: java
           :lineno-start: 68

           byte[] plaintext = MAPPER.writeValueAsBytes(formValues);

        Then, change the return to convert to base64:

        .. code-block:: java
           :lineno-start: 69

           return Base64.getEncoder().encodeToString(plaintext);

        Now, we'll do the same in ``decrypt``. Add a line to decode to a byte array:

        .. code-block:: java
           :lineno-start: 73

           byte[] ciphertextBytes = Base64.getDecoder().decode(ciphertext);

        Then, change the ``return`` to decode the JSON:

        .. code-block:: java
           :lineno-start: 74

           return MAPPER.readTree(ciphertextBytes);

    .. group-tab:: Python

        We'll use the builtin ``base64`` module.

        .. code-block:: python
           :lineno-start: 18

           import base64

        First, in ``encrypt``, add a line to encode the JSON string as bytes.

        .. code-block:: python
           :lineno-start: 37

           plaintext = json.dumps(data).encode("utf-8")

        Then, change the return to base64-encode the bytes and return the results decoded as a string.

        .. code-block:: python
           :lineno-start: 38

           return base64.b64encode(plaintext).decode("utf-8")

        Now, we'll do the reverse on ``decrypt``. Add a line to decode to bytes:

        .. code-block:: python
           :lineno-start: 45

            plaintext = base64.b64decode(data).decode("utf-8")

        Then change the ``return`` to parse the JSON.

        .. code-block:: python
           :lineno-start: 46

            return json.loads(plaintext)

After you've made the changes, use the appropriate :ref:`Build tool commands` to deploy them. Then try sending
and receiving a sample message. Now, when you use the **Ciphertext** radio button on the **Receive data** tab, you
should see the Base64-encoded message.

.. _The AWS CLI: https://docs.aws.amazon.com/cli/latest/userguide/cli-chap-welcome.html
.. _JDK 1.8: https://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html
.. _Maven 3: https://maven.apache.org/
.. _Git: https://git-scm.com/
.. _AWS CLI documentation: https://docs.aws.amazon.com/cli/latest/userguide/cli-config-files.html
