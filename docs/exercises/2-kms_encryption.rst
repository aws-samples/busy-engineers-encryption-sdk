
.. _Exercise 2:

***************************
Exercise 2: Introducing KMS
***************************

In our previous exercise, we updated the application to use base64 encoding in
preparation for actually encrypting data. Next, in this exercise we'll actually
encrypt by using KMS directly. We'll also see some of the limitations that
arise when using KMS directly as well.

Before we start
===============

We'll assume that you've completed the code changes in :ref:`Exercise 1`
first. If you haven't, you can use this git command to catch up:

.. tabs::

    .. group-tab:: Java

        .. code-block:: bash

            git checkout -f -B exercise-2 origin/exercise-2-start-java

    .. group-tab:: Python

        .. code-block:: bash

            git checkout -f -B exercise-2 origin/exercise-2-start-python

This will give you a codebase that already has the base64 changes applied.
Note that any uncommitted changes you've made already will be lost.

If you haven't done Exercise 1 at all, we encourage you to go through the
preparation and deployment steps in there at a minimum.

The :ref:`complete change<ex2-change>` is also available to help you view changes in context
and compare your work.

Using KMS directly
==================

In this exercise we'll use direct KMS
`Encrypt <https://docs.aws.amazon.com/kms/latest/APIReference/API_Encrypt.html>`_
and `Decrypt <https://docs.aws.amazon.com/kms/latest/APIReference/API_Decrypt.html>`_
calls to encrypt and decrypt data. We'll also set an appropriate Encryption Context,
and observe some of the subtle constraints when using KMS directly.

The KMS SDK API
---------------

The project is already configured to import the KMS SDK.

Here's a demonstration of client construction -- it's pretty simple. Since we're running in AWS Lambda,
the region and credentials are automatically configured for us.

Don't worry about adding this client yet, we'll add it in along with other functionality explained below.

.. tabs::

    .. group-tab:: Java

        .. code-block:: java

            AWSKMS kms = AWSKMSClient.builder().build();

    .. group-tab:: Python

        .. code-block:: python

            kms = boto3.client('kms')


.. tabs::

    .. group-tab:: Java

        Making KMS requests requires using ``EncryptRequest`` or ``DecryptRequest``
        objects with the data to encrypt or decrypt.

        These request objects use ``java.nio.ByteBuffer`` to transfer binary data.
        The APIs we looked at in the previous example used byte arrays (``byte[]``) instead,
        so we'll need to see how to convert between the two.

        Don't worry about changing anything yet, we'll walk through the necessary conversion
        with you below as we introduce using KMS.

        The pattern for converting from ``byte[]`` to ``ByteBuffer`` is easy:

        .. code-block:: java

            byte[] myArray = ...;
            ByteBuffer myBuffer = ByteBuffer.wrap(myArray);

        Converting from ``ByteBuffer`` to ``byte[]`` is a bit more complicated:

        .. code-block:: java

            ByteBuffer myBuffer = ...;

            byte[] myArray = new byte[myBuffer.remaining()];
            myBuffer.get(myArray);

        Note that invoking ``get`` changes the state of the ``ByteBuffer``; if you do this
        twice on the same buffer, you'll get an empty array as the second result.

        The KMS Client API uses ``ByteBuffer`` for all plaintext and ciphertext inputs
        and outputs, so keep this conversion pattern in mind as you work through the exercises.

    .. group-tab:: Python

        Enjoy some downtime while Java instructions explain how to do things that Python takes care of for you. ;)

Actually encrypting using KMS
=============================

Now let's try actually using KMS to encrypt and decrypt. If you'd like to try
putting it together on your own, you can refer to the `KMS SDK API documentation
<https://docs.aws.amazon.com/kms/latest/APIReference/API_Encrypt.html>`_
(`Java <https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/kms/AWSKMSClient.html>`_)
(`Python <https://boto3.amazonaws.com/v1/documentation/api/latest/reference/services/kms.html#KMS.Client.encrypt>`_)
and skip to :ref:`Using the Encryption Context` once you have it working; otherwise
we'll have specific directions below.

Step by step
------------

First, we'll want to set up a KMS client. It's good practice to construct the
client once and use that same instance throughout the life of your program, so
we'll do that here.

We'll also need to save the key ID we want to encrypt with. The sample code already
passes that key ID into the ``EncryptDecrypt`` class constructor, so we'll just save
it in a field for later reference.

.. tabs::

    .. group-tab:: Java

        We'll add to the top of our class a field definition for the client and key ID.

        .. code-block:: java
           :lineno-start: 46

            private static final Logger LOGGER = Logger.getLogger(EncryptDecrypt.class);
            private final AWSKMS kms; // <-- add this line
            private final String keyId; // <-- this one too

        Then, we'll initialize it in the constructor:

        .. code-block:: java
           :lineno-start: 58

            @Inject
            public EncryptDecrypt(@Named("keyIdEast") final String keyIdEast, @Named("keyIdWest") final String keyIdWest) {
                kms = AWSKMSClient.builder().build();
                this.keyId = keyIdEast;
            }

        In ``encrypt()``, we'll then build and issue the request:

        .. code-block:: java
           :lineno-start: 73

                EncryptRequest request = new EncryptRequest();
                request.setKeyId(keyId);
                request.setPlaintext(ByteBuffer.wrap(plaintext));

                EncryptResult result = kms.encrypt(request);

        We'll then need to convert the resulting ciphertext to a byte array before base64ing it:

        .. code-block:: java
           :lineno-start: 80

                // Convert to byte array
                byte[] ciphertext = new byte[result.getCiphertextBlob().remaining()];
                result.getCiphertextBlob().get(ciphertext);

                return Base64.getEncoder().encodeToString(ciphertext);

        At this point encryption should be working. What's left is decryption, which works very similarly:

        .. code-block:: java
           :lineno-start: 83

            public JsonNode decrypt(String ciphertext) throws IOException {
                byte[] ciphertextBytes = Base64.getDecoder().decode(ciphertext);

                DecryptRequest request = new DecryptRequest();
                request.setCiphertextBlob(ByteBuffer.wrap(ciphertextBytes));

                DecryptResult result = kms.decrypt(request);

                // Convert to byte array
                byte[] plaintext = new byte[result.getPlaintext().remaining()];
                result.getPlaintext().get(plaintext);

                return MAPPER.readTree(plaintext);
            }

    .. group-tab:: Python

        First we need to import the ``boto3`` library.

        .. code-block:: python
           :lineno-start: 20

            import boto3

        We'll need to add handlers to our ``__init__`` to create the KMS client.

        .. code-block:: python
           :lineno-start: 30

            self.kms = boto3.client("kms")

        In ``encrypt()`` we'll then call KMS and process the response.

        .. code-block:: python
           :lineno-start: 39

            response = self.kms.encrypt(KeyId=self.key_id, Plaintext=plaintext)
            ciphertext = response["CiphertextBlob"]
            return base64.b64encode(ciphertext).decode("utf-8")

        At this point encryption should be working. What's left is decryption, which works very similarly.  Insert the below lines into the ``decrypt()`` function:

        .. code-block:: python
           :lineno-start: 49

            ciphertext = base64.b64decode(data)
            response = self.kms.decrypt(CiphertextBlob=ciphertext)
            plaintext = response["Plaintext"]


Note that we don't need to provide the key ID to decrypt; decrypt will automatically
determine which key to use based on the ciphertext.

Now use the :ref:`Build tool commands` to deploy your updates.

.. _Using the Encryption Context:

Using the Encryption Context
============================

When encrypting with KMS it's good practice to set an Encryption Context. This
helps ensure that your code doesn't decrypt data intended for a different
purpose, and also helps improve your audit logging.

One of the difficulties around Encryption Contexts with KMS is that it's
necessary to store the context independently from the encrypted data, as it must
be presented when decrypting as well. Here we'll just put a type tag on the
Encryption Context, but if you're feeling ambitious we encourage you to try encoding
the order ID field in the Encryption Context as well.

In a later example we'll show you how the AWS Encryption SDK makes it easy to put
richer information in the Encryption Context as well.

Step by step
------------

Adding an Encryption Context that just has a type field is fairly simple.
First, we'll define some constants at the top of the class:

.. tabs::

    .. group-tab:: Java

        .. code-block:: java
           :lineno-start: 47

            private static final String K_MESSAGE_TYPE = "message type";
            private static final String TYPE_ORDER_INQUIRY = "order inquiry";

    .. group-tab:: Python

        .. code-block:: python
           :lineno-start: 28

            self._message_type = "message_type"
            self._type_order_inquiry = "order inquiry"

Since the strings used in the Encryption Context must match *exactly* between
encrypt and decrypt, it's good practice to define them through shared constants
to reduce the risk of typos.

We can then just add some code to set the context on encrypt, just before the
actual encrypt call:

.. tabs::

    .. group-tab:: Java

        .. code-block:: java
           :lineno-start: 79

            HashMap<String, String> context = new HashMap<>();
            context.put(K_MESSAGE_TYPE, TYPE_ORDER_INQUIRY);
            request.setEncryptionContext(context);

            The same code also needs to be placed right before the decrypt call as well.

    .. group-tab:: Python

        We need to set the Encryption Context on encrypt.

        .. code-block:: python
           :lineno-start: 41

            encryption_context = {self._message_type: self._type_order_inquiry}
            response = self.kms.encrypt(
                KeyId=self.key_id,
                Plaintext=plaintext,
                EncryptionContext=encryption_context
            )

        And also on decrypt.

        .. code-block:: python
           :lineno-start: 54

            encryption_context = {self._message_type: self._type_order_inquiry}
            response = self.kms.decrypt(
                CiphertextBlob=ciphertext,
                EncryptionContext=encryption_context
            )


Once you've used the :ref:`Build tool commands` to deploy this code and sent and
received data with it, about 10 minutes later the CloudTrail logs should show
entries with the new Encryption Context fields.

.. _ex2-change:

Complete change
---------------

View step-by-step changes in context, and compare your work if desired.

.. tabs::

    .. group-tab:: Java

        .. code:: diff

            diff --git a/webapp/src/main/java/example/encryption/EncryptDecrypt.java b/webapp/src/main/java/example/encryption/EncryptDecrypt.java
            index ab8e1d1..51660ec 100644
            --- a/webapp/src/main/java/example/encryption/EncryptDecrypt.java
            +++ b/webapp/src/main/java/example/encryption/EncryptDecrypt.java
            @@ -44,6 +44,11 @@ import com.fasterxml.jackson.databind.JsonNode;
             @Singleton
             public class EncryptDecrypt {
                 private static final Logger LOGGER = Logger.getLogger(EncryptDecrypt.class);
            +    private static final String K_MESSAGE_TYPE = "message type";
            +    private static final String TYPE_ORDER_INQUIRY = "order inquiry";
            +
            +    private final AWSKMS kms;
            +    private final String keyId;

                 @SuppressWarnings("unused") // all fields are used via JSON deserialization
                 private static class FormData {
            @@ -55,7 +60,8 @@ public class EncryptDecrypt {

                 @Inject
                 public EncryptDecrypt(@Named("keyIdEast") final String keyIdEast, @Named("keyIdWest") final String keyIdWest) {
            -        // TODO - do something with keyIdEast?
            +        kms = AWSKMSClient.builder().build();
            +        this.keyId = keyIdEast;
                 }

                 public String encrypt(JsonNode data) throws IOException {
            @@ -64,16 +70,41 @@ public class EncryptDecrypt {
                     // We can access specific form fields using values in the parsed FormData object.
                     LOGGER.info("Got form submission for order " + formValues.orderid);

            -        // TODO: Encryption goes here
            -
                     byte[] plaintext = MAPPER.writeValueAsBytes(formValues);

            -        return Base64.getEncoder().encodeToString(plaintext);
            +        EncryptRequest request = new EncryptRequest();
            +        request.setKeyId(keyId);
            +        request.setPlaintext(ByteBuffer.wrap(plaintext));
            +
            +        HashMap<String, String> context = new HashMap<>();
            +        context.put(K_MESSAGE_TYPE, TYPE_ORDER_INQUIRY);
            +        request.setEncryptionContext(context);
            +
            +        EncryptResult result = kms.encrypt(request);
            +
            +        // Convert to byte array
            +        byte[] ciphertext = new byte[result.getCiphertextBlob().remaining()];
            +        result.getCiphertextBlob().get(ciphertext);
            +
            +        return Base64.getEncoder().encodeToString(ciphertext);
                 }

                 public JsonNode decrypt(String ciphertext) throws IOException {
                     byte[] ciphertextBytes = Base64.getDecoder().decode(ciphertext);

            -        return MAPPER.readTree(ciphertextBytes);
            +        DecryptRequest request = new DecryptRequest();
            +        request.setCiphertextBlob(ByteBuffer.wrap(ciphertextBytes));
            +
            +        HashMap<String, String> context = new HashMap<>();
            +        context.put(K_MESSAGE_TYPE, TYPE_ORDER_INQUIRY);
            +        request.setEncryptionContext(context);
            +
            +        DecryptResult result = kms.decrypt(request);
            +
            +        // Convert to byte array
            +        byte[] plaintext = new byte[result.getPlaintext().remaining()];
            +        result.getPlaintext().get(plaintext);
            +
            +        return MAPPER.readTree(plaintext);
                 }
             }

    .. group-tab:: Python

        .. code:: diff

            diff --git a/src/busy_engineers_workshop/encrypt_decrypt.py b/src/busy_engineers_workshop/encrypt_decrypt.py
            index 0e34c26..b7e8e07 100644
            --- a/src/busy_engineers_workshop/encrypt_decrypt.py
            +++ b/src/busy_engineers_workshop/encrypt_decrypt.py
            @@ -17,6 +17,8 @@ This is the only module that you need to modify in the Busy Engineer's Guide to
             import base64
             import json

            +import boto3
            +

             class EncryptDecrypt(object):
                 """Encrypt and decrypt data."""
            @@ -27,6 +29,7 @@ class EncryptDecrypt(object):
                     self._type_order_inquiry = "order inquiry"
                     self._timestamp = "rough timestamp"
                     self.key_id = key_id
            +        self.kms = boto3.client("kms")

                 def encrypt(self, data):
                     """Encrypt data.
            @@ -35,8 +38,11 @@ class EncryptDecrypt(object):
                     :returns: Base64-encoded, encrypted data
                         :rtype: str
                         """
                +        encryption_context = {self._message_type: self._type_order_inquiry}
                         plaintext = json.dumps(data).encode("utf-8")
                -        return base64.b64encode(plaintext).decode("utf-8")
                +        response = self.kms.encrypt(KeyId=self.key_id, Plaintext=plaintext, EncryptionContext=encryption_context)
                +        ciphertext = response["CiphertextBlob"]
                +        return base64.b64encode(ciphertext).decode("utf-8")

                     def decrypt(self, data):
                         """Decrypt data.
                @@ -44,5 +50,9 @@ class EncryptDecrypt(object):
                         :param bytes data: Base64-encoded, encrypted data
                         :returns: JSON-decoded, decrypted data
                         """
                -        plaintext = base64.b64decode(data).decode("utf-8")
                +        ciphertext = base64.b64decode(data)
                +        encryption_context = {self._message_type: self._type_order_inquiry}
                +        response = self.kms.decrypt(CiphertextBlob=ciphertext, EncryptionContext=encryption_context)
                +        plaintext = response["Plaintext"]
                +
                         return json.loads(plaintext)

Extra credit
============

Feeling ambitious? Try encoding the order ID into the Encryption Context as
well. The tricky part about this is that the order ID must be known at decrypt
time - so you'll need to find a way to encode it into the message outside of
the ciphertext.

If you encode the order ID into the context, you'll see it flowing through to
your CloudTrail logs as well - so you'll know which inquiries are being
decrypted.
