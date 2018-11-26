
.. _Exercise 3:

**********************************
Exercise 3: The AWS Encryption SDK
**********************************

So far we've been working with the SDK's KMS client directly. This has a few
limitations, as we'll see.

Before we start
===============

We'll assume that you've completed the code changes in :ref:`Exercise 2`
first. If you haven't, you can use this git command to catch up:

.. tabs::

    .. group-tab:: Java

        .. code-block:: bash

            git checkout -f -B exercise-3 origin/exercise-3-start-java

    .. group-tab:: Python

        .. code-block:: bash

            git checkout -f -B exercise-3 origin/exercise-3-start-python

This will give you a codebase that includes the base64 changes and direct
KMS encryption from Exercises 1 and 2.
Note that any uncommitted changes you've made already will be lost.

The :ref:`complete change<ex3-change>` is also available to help you view changes in context
and compare your work.


Exploring the guardrails of direct KMS
======================================

Directly using KMS means that your messages will be limited in size to 4096
bytes. Try this out for yourself - copy and paste this block into the message
field, and see how KMS rejects the message:

::

    The history of war teems with occasions where the interception of
    dispatches and orders written in plain language has resulted in
    defeat and disaster for the force whose intentions thus became known
    at once to the enemy. For this reason, prudent generals have used
    cipher and code messages from time immemorial. The necessity for
    exact expression of ideas practically excludes the use of codes for
    military work although it is possible that a special tactical code
    might be useful for preparation of tactical orders.

    It is necessary therefore to fall back on ciphers for general military
    work if secrecy of communication is to be fairly well assured. It
    may as well be stated here that no practicable military cipher is
    mathematically indecipherable if intercepted; the most that can be
    expected is to delay for a longer or shorter time the deciphering of
    the message by the interceptor.

    The capture of messengers is no longer the only means available to
    the enemy for gaining information as to the plans of a commander. All
    radio messages sent out can be copied at hostile stations within radio
    range. If the enemy can get a fine wire within one hundred feet of a
    buzzer line or within thirty feet of a telegraph line, the message can
    be copied by induction. Messages passing over commercial telegraph
    lines, and even over military lines, can be copied by spies in the
    offices. On telegraph lines of a permanent nature it is possible to
    install high speed automatic sending and receiving machines and thus
    prevent surreptitious copying of messages, but nothing but a secure
    cipher will serve with other means of communication.

    It is not alone the body of the message which should be in cipher. It
    is equally important that, during transmission, the preamble, place
    from, date, address and signature be enciphered; but this should
    be done by the sending operator and these parts must, of course,
    be deciphered by the receiving operator before delivery. A special
    operators' cipher should be used for this purpose but it is difficult
    to prescribe one that would be simple enough for the average operator,
    fast and yet reasonably safe. Some form of rotary cipher machine
    would seem to be best suited for this special purpose.

    It is unnecessary to point out that a cipher which can be deciphered
    by the enemy in a few hours is worse than useless. It requires a
    surprisingly long time to encipher and decipher a message, using even
    the simplest kind of cipher, and errors in transmission of cipher
    matter by wire or radio are unfortunately too common.

    Kerckhoffs has stated that a military cipher should fulfill the
    following requirements:


        1st. The system should be materially, if not mathematically,
             indecipherable.
        2d.  It should cause no inconvenience if the apparatus and methods
             fall into the hands of the enemy.
        3d.  The key should be such that it could be communicated and
             remembered without the necessity of written notes and should
             be changeable at the will of the correspondents.
        4th. The system should be applicable to telegraphic correspondence.
        5th. The apparatus should be easily carried and a single person
             should be able to operate it.
        6th. Finally, in view of the circumstances under which it must
             be used, the system should be an easy one to operate,
             demanding neither mental strain nor knowledge of a long series
             of rules.


    A brief consideration of these six conditions must lead to the
    conclusion that there is no perfect military cipher. The first
    requirement is the one most often overlooked by those prescribing
    the use of any given cipher and, even if not overlooked, the
    indecipherability of any cipher likely to be used for military purposes
    is usually vastly overestimated by those prescribing the use of it.

    If this were not true, there would have been neither material for,
    nor purpose in, the preparation of these notes. Of the hundreds of
    actual cipher messages examined by the writer, at least nine-tenths
    have been solved by the methods to be set forth. These messages were
    prepared by the methods in use by the United States Army, the various
    Mexican armies and their secret agents, and by other methods in common
    use. The usual failure has been with very short messages. Foreign
    works consulted lead to the belief that many European powers have
    used, for military purposes, cipher methods which vary from an
    extreme simplicity to a complexity which is more apparent than
    real. What effect recent events have had on this matter remains to
    be seen. It is enough that the cipher experts of practically every
    European country have appealed to the military authorities of their
    respective countries time and again to do away with these useless
    ciphers and to adopt something which offers more security, even at
    the expense of other considerations.

    The cipher of the amateur, or of the non-expert who makes one up
    for some special purpose, is almost sure to fall into one of the
    classes whose solution is an easy matter. The human mind works along
    the same lines, in spite of an attempt at originality on the part of
    the individual, and this is particularly true of cipher work because
    there are so few sources of information available. In other words,
    the average man, when he sits down to evolve a cipher, has nothing
    to improve upon; he invents and there is no one to tell him that his
    invention is, in principle, hundreds of years old. The ciphers of the
    Abbé Tritheme, 1499, are the basis of most of the modern substitution
    ciphers.

    In view of these facts, no message should be considered
    indecipherable. Very short messages are often very difficult and may
    easily be entirely beyond the possibility of analysis and solution,
    but it is surprising what can be done, at times, with a message of
    only a few words.

    In the event of active operations, cipher experts will be in demand
    at once. Like all other experts, the cipher expert is not born or
    made in a day; and it is only constant work with ciphers, combined
    with a thorough knowledge of their underlying principles, that will
    make one worthy of the name.

    Hitt, Parker. (1916) MANUAL FOR THE SOLUTION OF MILITARY CIPHERS.
    Retrieved from https://www.gutenberg.org/ebooks/48871


You may also have noticed that using the KMS client directly requires
a fair amount of boilerplate - in particular, all those byte buffer
conversions. It's also difficult to put any kind of dynamic data in
the Encryption Context, as you need to find a separate place to store
those context values. We'll resolve all of these by converting things
to use the AWS Encryption SDK instead.

Overview of exercise
====================

In this exercise we'll:

#. Implement encryption using the AWS Encryption SDK
#. Set up a dynamic Encryption Context

Step by step
------------

First, let's make sure the Encryption SDK is set up as a dependency correctly.


.. tabs::

    .. group-tab:: Java

        Open up ``webapp/pom.xml`` and add this block in the ``<dependencies>`` section:

        .. code-block:: xml

                <dependency>
                    <groupId>com.amazonaws</groupId>
                    <artifactId>aws-encryption-sdk-java</artifactId>
                    <version>1.3.5</version>
                </dependency>

    .. group-tab:: Python

        Open ``setup.py`` and ensure this requirement is in ``install_requires``:

        .. code-block:: python

            install_requires=["aws_encryption_sdk>=1.3.8"]

Now, let's add some imports:

.. tabs::

    .. group-tab:: Java

        .. code-block:: java
           :lineno-start: 30

            import java.util.Objects;
            import com.amazonaws.encryptionsdk.AwsCrypto;
            import com.amazonaws.encryptionsdk.CryptoResult;
            import com.amazonaws.encryptionsdk.kms.KmsMasterKey;
            import com.amazonaws.encryptionsdk.kms.KmsMasterKeyProvider;

    .. group-tab:: Python

        .. code-block:: python
           :lineno-start: 21

            import aws_encryption_sdk

:ref:`master-keys` are used by the AWS Encryption SDK
to protect your data. The first step to using the Encryption SDK is setting up
a Master Key or Master Key Provider. Once we set up our Master Key,
we won't need to keep around the key ID, so we can discard that value.

.. tabs::

    .. group-tab:: Java

        We won't need the class attribute for ``keyID``, so replace that with ``masterKey``
        for the KMS Master Key.

        .. code-block:: java
           :lineno-start: 56

            private final KmsMasterKey masterKey;

        In our constructor, we'll create the Master Key like so:

        .. code-block:: java
           :lineno-start: 69

            this.masterKey = new KmsMasterKeyProvider(keyId)
                .getMasterKey(keyId);

    .. group-tab:: Python

        We won't need to keep the key ID around, so replace that in ``__init__`` with a new ``KMSMasterKeyProvider``.

        .. code-block:: python
           :lineno-start: 32

            self.master_key_provider = aws_encryption_sdk.KMSMasterKeyProvider(key_ids=[key_id])


The actual encryption process is much simpler than with KMS. We'll keep the
Encryption Context mostly the same, and the body of encrypt can just be:

.. tabs::

    .. group-tab:: Java

        .. code-block:: java
           :lineno-start: 73

            public String encrypt(JsonNode data) throws IOException {
                FormData formValues = MAPPER.treeToValue(data, FormData.class);

                // We can access specific form fields using values in the parsed FormData object.
                LOGGER.info("Got form submission for order " + formValues.orderid);

                byte[] plaintext = MAPPER.writeValueAsBytes(formValues);

                HashMap<String, String> context = new HashMap<>();
                context.put(K_MESSAGE_TYPE, TYPE_ORDER_INQUIRY);

                byte[] ciphertext = new AwsCrypto().encryptData(masterKey, plaintext, context).getResult();

                return Base64.getEncoder().encodeToString(ciphertext);
            }

    .. group-tab:: Python

        .. code-block:: python
           :lineno-start: 34

            def encrypt(self, data):
                """Encrypt data.
                :param data: JSON-encodeable data to encrypt
                :returns: Base64-encoded, encrypted data
                :rtype: str
                """
                encryption_context = {self._message_type: self._type_order_inquiry}
                ciphertext, _header = aws_encryption_sdk.encrypt(
                    source=json.dumps(data),
                    key_provider=self.master_key_provider,
                    encryption_context=encryption_context,
                )
                return base64.b64encode(ciphertext).decode("utf-8")

For decrypt, we no longer need to construct an Encryption Context because the
AWS Encryption SDK records the original context for us. However, this means we now
need to check that the context is consistent with what we expected.
Decrypt therefore ends up looking like:

.. tabs::

    .. group-tab:: Java

        .. code-block:: java
           :lineno-start: 92

            public JsonNode decrypt(String ciphertext) throws IOException {
                byte[] ciphertextBytes = Base64.getDecoder().decode(ciphertext);

                CryptoResult<byte[], ?> result = new AwsCrypto().decryptData(masterKey, ciphertextBytes);

                // Check that we have the correct type
                if (!Objects.equals(result.getEncryptionContext().get(K_MESSAGE_TYPE), TYPE_ORDER_INQUIRY)) {
                    throw new IllegalArgumentException("Bad message type in decrypted message");
                }

                return MAPPER.readTree(result.getResult());
            }

    .. group-tab:: Python

        .. code-block:: python
           :lineno-start: 50

            def decrypt(self, data):
                """Decrypt data.
                :param bytes data: Base64-encoded, encrypted data
                :returns: JSON-decoded, decrypted data
                """
                ciphertext = base64.b64decode(data)
                plaintext, header = aws_encryption_sdk.decrypt(
                    source=ciphertext,
                    key_provider=self.master_key_provider,
                )

                try:
                    if header.encryption_context[self._message_type] != self._type_order_inquiry:
                        raise KeyError()  # overloading KeyError to use the same exit whether wrong or missing
                except KeyError:
                    raise ValueError("Bad message type in decrypted message")

                return json.loads(plaintext)

Now use the :ref:`Build tool commands` to deploy your application again.

Try entering the very large message from the start of this exercise; it should work
now.

.. note::

    If you input a message larger than about 90k you'll still run into
    message size limits related to our use of SQS as well. If handling very large
    messages was needed for your application, you might want to consider putting
    the message in S3, and sending a reference to it via SQS.

.. _master-keys:

Master Keys and Master Key Providers
====================================

Within the AWS Encryption SDK, your data is protected by Data Keys, but those Data Keys must also be protected.
`Master Keys`_ and `Master Key Providers`_ are objects that allow you to control how the AWS Encryption SDK
protects your Data Keys.

Master Keys are used by the AWS Encryption SDK client to generate and manage Data Keys.

Master Key Providers supply Master Keys to the client.

You can provide either a Master Key or a Master Key Provider to the client, and the client will handle obtaining the Master Key it requires.


.. _Master Keys: https://docs.aws.amazon.com/encryption-sdk/latest/developer-guide/concepts.html#master-key-provider
.. _Master Key Providers: https://docs.aws.amazon.com/encryption-sdk/latest/developer-guide/concepts.html#master-key-operations

Adding additional audit metadata to your Encryption Context
===========================================================

Now that you're using the AWS Encryption SDK, it's a lot easier to put
dynamically-changing data in the Encryption Context. For example, we can record
the order ID just by doing:

.. tabs::

    .. group-tab:: Java

        .. code-block:: java
           :lineno-start: 82

            context.put("order ID", formValues.orderid);

    .. group-tab:: Python

        First, import ``time``.

        .. code-block:: python
           :lineno-start: 19

            import time

        Now add the additional metadata.

        .. code-block:: python
           :lineno-start: 41

            encryption_context = {
                self._message_type: self._type_order_inquiry,
                self._timestamp: str(int(time.time() / 3600.0)),
            }

No changes are needed in decrypt. The AWS Encryption SDK stores Encryption Context
for you on the message format it produces so that it is available to provide to
KMS. Your client code can check for the presence or expected values of Encryption
Context keys as a best practice.

After adding these Encryption Context values, redeploy your application with the
:ref:`Build tool commands`, send some messages, and then check
your CloudTrail logs. After 10 minutes, you'll see the Encryption Context values
flowing through.

One caveat to note is that Encryption Context values can't be empty strings. To
deal with this you can either use special values to indicate empty/``null``
fields, only add the key if the field has a meaningful value, or require
that the field be present.

.. _ex3-change:

Complete change
---------------

View step-by-step changes in context, and compare your work if desired.

.. tabs::

    .. group-tab:: Java

        .. code:: diff

            diff --git a/webapp/pom.xml b/webapp/pom.xml
            index a565be8..643dd86 100644
            --- a/webapp/pom.xml
            +++ b/webapp/pom.xml
            @@ -30,6 +30,12 @@
                         <version>1.1.0</version>
                     </dependency>

            +        <dependency>
            +            <groupId>com.amazonaws</groupId>
            +            <artifactId>aws-encryption-sdk-java</artifactId>
            +            <version>1.3.5</version>
            +        </dependency>
            +
                     <dependency>
                         <groupId>com.amazonaws</groupId>
                         <artifactId>aws-java-sdk-sqs</artifactId>
            diff --git a/webapp/src/main/java/example/encryption/EncryptDecrypt.java b/webapp/src/main/java/example/encryption/EncryptDecrypt.java
            index 29b6f71..b544d59 100644
            --- a/webapp/src/main/java/example/encryption/EncryptDecrypt.java
            +++ b/webapp/src/main/java/example/encryption/EncryptDecrypt.java
            @@ -27,6 +27,10 @@ import java.util.concurrent.TimeUnit;

             import org.apache.log4j.Logger;

            +import com.amazonaws.encryptionsdk.AwsCrypto;
            +import com.amazonaws.encryptionsdk.CryptoResult;
            +import com.amazonaws.encryptionsdk.kms.KmsMasterKey;
            +import com.amazonaws.encryptionsdk.kms.KmsMasterKeyProvider;
             import com.amazonaws.services.kms.AWSKMS;
             import com.amazonaws.services.kms.AWSKMSClient;
             import com.amazonaws.services.kms.model.DecryptRequest;
            @@ -46,9 +50,10 @@ public class EncryptDecrypt {
                 private static final Logger LOGGER = Logger.getLogger(EncryptDecrypt.class);
                 private static final String K_MESSAGE_TYPE = "message type";
                 private static final String TYPE_ORDER_INQUIRY = "order inquiry";
            +    private static final String K_ORDER_ID = "order ID";

                 private final AWSKMS kms;
            -    private final String keyId;
            +    private final KmsMasterKey masterKey;

                 @SuppressWarnings("unused") // all fields are used via JSON deserialization
                 private static class FormData {
            @@ -61,7 +66,8 @@ public class EncryptDecrypt {
                 @Inject
                 public EncryptDecrypt(@Named("keyId") final String keyId) {
                     kms = AWSKMSClient.builder().build();
            -        this.keyId = keyId;
            +        this.masterKey = new KmsMasterKeyProvider(keyId)
            +            .getMasterKey(keyId);
                 }

                 public String encrypt(JsonNode data) throws IOException {
            @@ -72,19 +78,13 @@ public class EncryptDecrypt {

                     byte[] plaintext = MAPPER.writeValueAsBytes(formValues);

            -        EncryptRequest request = new EncryptRequest();
            -        request.setKeyId(keyId);
            -        request.setPlaintext(ByteBuffer.wrap(plaintext));
            -
                     HashMap<String, String> context = new HashMap<>();
                     context.put(K_MESSAGE_TYPE, TYPE_ORDER_INQUIRY);
            -        request.setEncryptionContext(context);
            -
            -        EncryptResult result = kms.encrypt(request);
            +        if (formValues.orderid != null && formValues.orderid.length() > 0) {
            +            context.put(K_ORDER_ID, formValues.orderid);
            +        }

            -        // Convert to byte array
            -        byte[] ciphertext = new byte[result.getCiphertextBlob().remaining()];
            -        result.getCiphertextBlob().get(ciphertext);
            +        byte[] ciphertext = new AwsCrypto().encryptData(masterKey, plaintext, context).getResult();

                     return Base64.getEncoder().encodeToString(ciphertext);
                 }
            @@ -92,19 +92,13 @@ public class EncryptDecrypt {
                 public JsonNode decrypt(String ciphertext) throws IOException {
                     byte[] ciphertextBytes = Base64.getDecoder().decode(ciphertext);

            -        DecryptRequest request = new DecryptRequest();
            -        request.setCiphertextBlob(ByteBuffer.wrap(ciphertextBytes));
            -
            -        HashMap<String, String> context = new HashMap<>();
            -        context.put(K_MESSAGE_TYPE, TYPE_ORDER_INQUIRY);
            -        request.setEncryptionContext(context);
            -
            -        DecryptResult result = kms.decrypt(request);
            +        CryptoResult<byte[], ?> result = new AwsCrypto().decryptData(masterKey, ciphertextBytes);

            -        // Convert to byte array
            -        byte[] plaintext = new byte[result.getPlaintext().remaining()];
            -        result.getPlaintext().get(plaintext);
            +        // Check that we have the correct type
            +        if (!Objects.equals(result.getEncryptionContext().get(K_MESSAGE_TYPE), TYPE_ORDER_INQUIRY)) {
            +            throw new IllegalArgumentException("Bad message type in decrypted message");
            +        }

            -        return MAPPER.readTree(plaintext);
            +        return MAPPER.readTree(result.getResult());
                 }
             }

    .. group-tab:: Python

        .. code:: diff

            diff --git a/src/busy_engineers_workshop/encrypt_decrypt.py b/src/busy_engineers_workshop/encrypt_decrypt.py
            index b7e8e07..f2cc5ec 100644
            --- a/src/busy_engineers_workshop/encrypt_decrypt.py
            +++ b/src/busy_engineers_workshop/encrypt_decrypt.py
            @@ -16,8 +16,9 @@ This is the only module that you need to modify in the Busy Engineer's Guide to
             """
             import base64
             import json
            +import time

            -import boto3
            +import aws_encryption_sdk


             class EncryptDecrypt(object):
            @@ -28,8 +29,7 @@ class EncryptDecrypt(object):
                     self._message_type = "message_type"
                     self._type_order_inquiry = "order inquiry"
                     self._timestamp = "rough timestamp"
            -        self.key_id = key_id
            -        self.kms = boto3.client("kms")
            +        self.master_key_provider = aws_encryption_sdk.KMSMasterKeyProvider(key_ids=[key_id])

                 def encrypt(self, data):
                     """Encrypt data.
            @@ -38,10 +38,13 @@ class EncryptDecrypt(object):
                     :returns: Base64-encoded, encrypted data
                         :rtype: str
                         """
                -        encryption_context = {self._message_type: self._type_order_inquiry}
                -        plaintext = json.dumps(data).encode("utf-8")
                -        response = self.kms.encrypt(KeyId=self.key_id, Plaintext=plaintext, EncryptionContext=encryption_context)
                -        ciphertext = response["CiphertextBlob"]
                +        encryption_context = {
                +            self._message_type: self._type_order_inquiry,
                +            self._timestamp: str(int(time.time() / 3600.0)),
                +        }
                +        ciphertext, _header = aws_encryption_sdk.encrypt(
                +            source=json.dumps(data), key_provider=self.master_key_provider, encryption_context=encryption_context
                +        )
                         return base64.b64encode(ciphertext).decode("utf-8")

                     def decrypt(self, data):
                @@ -51,8 +54,12 @@ class EncryptDecrypt(object):
                         :returns: JSON-decoded, decrypted data
                         """
                         ciphertext = base64.b64decode(data)
                -        encryption_context = {self._message_type: self._type_order_inquiry}
                -        response = self.kms.decrypt(CiphertextBlob=ciphertext, EncryptionContext=encryption_context)
                -        plaintext = response["Plaintext"]
                +        plaintext, header = aws_encryption_sdk.decrypt(source=ciphertext, key_provider=self.master_key_provider)
                +
                +        try:
                +            if header.encryption_context[self._message_type] != self._type_order_inquiry:
                +                raise KeyError()  # overloading KeyError to use the same exit whether wrong or missing
                +        except KeyError:
                +            raise ValueError("Bad message type in decrypted message")

                         return json.loads(plaintext)
