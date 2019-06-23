
.. _Exercise 4:

****************************
Exercise 4: Data Key Caching
****************************

You may have noticed by now that when you send or receive messages, we report
the number of KMS calls issued. Currently, every message you send or receive
incurs a single KMS call. While KMS calls are inexpensive and have high default
limits, when you write an application that performs encrypt or decrypt
operations at extremely high volumes, you might find the overhead of performing a
KMS call every time you encrypt and decrypt to be limiting.

In this exercise we'll explore the caching feature of the AWS Encryption SDK
and how it can help mitigate this issue.

Before we start
===============

We'll assume that you've completed the code changes in :ref:`Exercise 3`
first. If you haven't, you can use this git command to catch up:

.. tabs::

    .. group-tab:: Java

        .. code-block:: bash

            git checkout -f -B exercise-4 origin/exercise-4-start-java

    .. group-tab:: Python

        .. code-block:: bash

            git checkout -f -B exercise-4 origin/exercise-4-start-python

This will give you a codebase that already uses the AWS Encryption SDK.
Note that any uncommitted changes you've made already will be lost.

The :ref:`complete change<ex4-change>` is also available to help you view changes in context
and compare your work.

How the caching feature works
=============================

You enable the caching feature of the AWS Encryption SDK by creating a
"`caching Crypto Materials Manager
<https://docs.aws.amazon.com/encryption-sdk/latest/developer-guide/implement-caching.html>`_"
and using it instead of your Master Key in encrypt and decrypt operations.
Crypto Materials Managers are plugins that can manipulate encrypt and decrypt
requests in certain ways.

When caching is enabled, the Encryption SDK generates a Data Key the first time
encrypt is invoked, then re-uses it for subsequent messages. On decrypt, we
conversely remember the mapping from encrypted Data Key to decrypted Data Key,
and reuse that as well.

The code changes for this are fairly small, so let's jump right into it.

Step by step
------------

First, we'll add the new imports we'll need:

.. tabs::

    .. group-tab:: Java

        .. code-block:: java
           :lineno-start: 25

            import java.util.concurrent.TimeUnit;
            import com.amazonaws.encryptionsdk.CryptoMaterialsManager;
            import com.amazonaws.encryptionsdk.caching.CachingCryptoMaterialsManager;
            import com.amazonaws.encryptionsdk.caching.LocalCryptoMaterialsCache;

    .. group-tab:: Python

        .. code-block:: python
           :lineno-start: 19

            import time


Then, we'll replace our MasterKey field with a CryptoMaterialsManager:

.. tabs::

    .. group-tab:: Java

        .. code-block:: java
           :lineno-start: 54

            private final CryptoMaterialsManager materialsManager;

        It's important to make this a field instead of initializing it for each call,
        as we need the cache to persist from one call to the next.

        In our constructor, we'll set up our Master Key, cache, and Caching Materials Manager:

        .. code-block:: java
           :lineno-start: 67

            KmsMasterKey masterKey = new KmsMasterKeyProvider(keyId)
                .getMasterKey(keyId);

            LocalCryptoMaterialsCache cache = new LocalCryptoMaterialsCache(100);
            materialsManager = CachingCryptoMaterialsManager.newBuilder()
                .withMaxAge(5, TimeUnit.MINUTES)
                .withMasterKeyProvider(masterKey)
                .withMessageUseLimit(10)
                .withCache(cache)
                .build();

    .. group-tab:: Python

        We'll set up the Master Key Provider, cache, and Caching Materials Manager in our ``__init__``:

        .. code-block:: python
           :lineno-start: 33

            master_key_provider = aws_encryption_sdk.KMSMasterKeyProvider(key_ids=[key_id_east])
            cache = aws_encryption_sdk.LocalCryptoMaterialsCache(capacity=100)
            self.materials_manager = aws_encryption_sdk.CachingCryptoMaterialsManager(
                cache=cache, master_key_provider=master_key_provider, max_age=5.0 * 60.0, max_messages_encrypted=10
            )

And finally, we'll use the ``materialsManager`` instead of our ``masterKey`` in our
encrypt and decrypt operations:

.. tabs::

    .. group-tab:: Java

        In your ``encrypt`` function, which should start around line 79, change how you compute ``ciphertext``:

        .. code-block:: java
           :lineno-start: 92

           byte[] ciphertext = new AwsCrypto().encryptData(materialsManager, plaintext, context).getResult();


        And in ``decrypt``, which should start around line 97, change how you compute your ``CryptoResult``:

        .. code-block:: java
           :lineno-start: 100

            CryptoResult<byte[], ?> result = new AwsCrypto().decryptData(materialsManager, ciphertextBytes);

    .. group-tab:: Python

        In your ``encrypt`` function, change how you compute ``ciphertext``:

        .. code-block:: python
           :lineno-start: 50

            ciphertext, _header = aws_encryption_sdk.encrypt(
                source=json.dumps(data),
                materials_manager=self.materials_manager,
                encryption_context=encryption_context
            )

        And in ``decrypt``, change how you compute ``plaintext``:

        .. code-block:: python
           :lineno-start: 62

            plaintext, header = aws_encryption_sdk.decrypt(
                source=ciphertext,
                materials_manager=self.materials_manager
            )

Once you finish the changes, use the appropriate :ref:`Build tool commands` to
deploy and try sending a few messages in a row. You'll see that only one message
out of ten result in a KMS call, for both send and receive.

Encryption Context issues
=========================

If you followed the previous exercise to the end, you'll remember we added the
order ID to the Encryption Context. If not, now's a good time to add it.

Try sending a few messages in a row with different order IDs. You'll note that
the cache doesn't work in this case; this is because messages with different
Encryption Contexts cannot use the same cached result.

This illustrates the balance that needs to be struck between cache performance,
access control verification, and audit log verbosity: improving cache performance
requires reducing the fidelity of the other two elements.

To get benefit from caching here, we'll need to strike a different balance. For
example, instead of putting the order ID in the audit log, we could put an
*approximate* timestamp, like so:

.. tabs::

    .. group-tab:: Java

        Let's define a constant for the timestamp key:

        .. code-block:: java
           :lineno-start: 51

            private static final String K_TIMESTAMP = "rough timestamp";

        And put it in Encryption Context:

        .. code-block:: java
           :lineno-start: 90

            context.put(K_TIMESTAMP, "" + (System.currentTimeMillis() / 3_600_000) * 3_600_000);

    .. group-tab:: Python

        The ``_timestamp`` key is already defined. Let's put it in Encryption Context:

        .. code-block:: python
           :lineno-start: 45

            encryption_context = {
                self._message_type: self._type_order_inquiry,
                self._timestamp: str(int(time.time() / 3600.0)),
            }

This puts a timestamp, rounded down to the nearest hour, in the context. This
provides us a certain degree of information about what data is being decrypted,
without ruining the usefulness of the cache.


.. _ex4-change:

Complete change
---------------

View step-by-step changes in context, and compare your work if desired.

.. tabs::

    .. group-tab:: Java

        .. code:: diff

            diff --git a/webapp/src/main/java/example/encryption/EncryptDecrypt.java b/webapp/src/main/java/example/encryption/EncryptDecrypt.java
            index b544d59..1b75f06 100644
            --- a/webapp/src/main/java/example/encryption/EncryptDecrypt.java
            +++ b/webapp/src/main/java/example/encryption/EncryptDecrypt.java
            @@ -19,7 +19,6 @@ import javax.inject.Inject;
             import javax.inject.Named;
             import javax.inject.Singleton;
             import java.io.IOException;
            -import java.nio.ByteBuffer;
             import java.util.Base64;
             import java.util.HashMap;
             import java.util.Objects;
            @@ -28,15 +27,14 @@ import java.util.concurrent.TimeUnit;
             import org.apache.log4j.Logger;

             import com.amazonaws.encryptionsdk.AwsCrypto;
            +import com.amazonaws.encryptionsdk.CryptoMaterialsManager;
             import com.amazonaws.encryptionsdk.CryptoResult;
            +import com.amazonaws.encryptionsdk.caching.CachingCryptoMaterialsManager;
            +import com.amazonaws.encryptionsdk.caching.LocalCryptoMaterialsCache;
             import com.amazonaws.encryptionsdk.kms.KmsMasterKey;
             import com.amazonaws.encryptionsdk.kms.KmsMasterKeyProvider;
             import com.amazonaws.services.kms.AWSKMS;
             import com.amazonaws.services.kms.AWSKMSClient;
            -import com.amazonaws.services.kms.model.DecryptRequest;
            -import com.amazonaws.services.kms.model.DecryptResult;
            -import com.amazonaws.services.kms.model.EncryptRequest;
            -import com.amazonaws.services.kms.model.EncryptResult;
             import com.fasterxml.jackson.databind.JsonNode;

             /**
            @@ -50,10 +48,10 @@ public class EncryptDecrypt {
                 private static final Logger LOGGER = Logger.getLogger(EncryptDecrypt.class);
                 private static final String K_MESSAGE_TYPE = "message type";
                 private static final String TYPE_ORDER_INQUIRY = "order inquiry";
            -    private static final String K_ORDER_ID = "order ID";
            +    private static final String K_TIMESTAMP = "rough timestamp";

                 private final AWSKMS kms;
            -    private final KmsMasterKey masterKey;
            +    private final CryptoMaterialsManager materialsManager;

                 @SuppressWarnings("unused") // all fields are used via JSON deserialization
                 private static class FormData {
            @@ -66,8 +64,16 @@ public class EncryptDecrypt {
                 @Inject
                 public EncryptDecrypt(@Named("keyId") final String keyId) {
                     kms = AWSKMSClient.builder().build();
            -        this.masterKey = new KmsMasterKeyProvider(keyId)
            +        KmsMasterKey masterKey = new KmsMasterKeyProvider(keyId)
                         .getMasterKey(keyId);
            +
            +        LocalCryptoMaterialsCache cache = new LocalCryptoMaterialsCache(100);
            +        materialsManager = CachingCryptoMaterialsManager.newBuilder()
            +            .withMaxAge(5, TimeUnit.MINUTES)
            +            .withMasterKeyProvider(masterKey)
            +            .withMessageUseLimit(10)
            +            .withCache(cache)
            +            .build();
                 }

                 public String encrypt(JsonNode data) throws IOException {
            @@ -80,11 +86,10 @@ public class EncryptDecrypt {

                     HashMap<String, String> context = new HashMap<>();
                     context.put(K_MESSAGE_TYPE, TYPE_ORDER_INQUIRY);
            -        if (formValues.orderid != null && formValues.orderid.length() > 0) {
            -            context.put(K_ORDER_ID, formValues.orderid);
            -        }
            +        // Round down to an hour
            +        context.put(K_TIMESTAMP, "" + (System.currentTimeMillis() / 3_600_000) * 3_600_000);

            -        byte[] ciphertext = new AwsCrypto().encryptData(masterKey, plaintext, context).getResult();
            +        byte[] ciphertext = new AwsCrypto().encryptData(materialsManager, plaintext, context).getResult();

                     return Base64.getEncoder().encodeToString(ciphertext);
                 }
            @@ -92,7 +97,7 @@ public class EncryptDecrypt {
                 public JsonNode decrypt(String ciphertext) throws IOException {
                     byte[] ciphertextBytes = Base64.getDecoder().decode(ciphertext);

            -        CryptoResult<byte[], ?> result = new AwsCrypto().decryptData(masterKey, ciphertextBytes);
            +        CryptoResult<byte[], ?> result = new AwsCrypto().decryptData(materialsManager, ciphertextBytes);

                     // Check that we have the correct type
                     if (!Objects.equals(result.getEncryptionContext().get(K_MESSAGE_TYPE), TYPE_ORDER_INQUIRY)) {

    .. group-tab:: Python

        .. code:: diff

            diff --git a/src/busy_engineers_workshop/encrypt_decrypt.py b/src/busy_engineers_workshop/encrypt_decrypt.py
            index 4e153a3..c1f4456 100644
            --- a/src/busy_engineers_workshop/encrypt_decrypt.py
            +++ b/src/busy_engineers_workshop/encrypt_decrypt.py
            @@ -16,6 +16,7 @@ This is the only module that you need to modify in the Busy Engineer's Guide to
             """
             import base64
             import json
            +import time

             import aws_encryption_sdk

            @@ -29,7 +30,11 @@ class EncryptDecrypt(object):
                     self._type_order_inquiry = "order inquiry"
                     self._timestamp = "rough timestamp"
                     self._order_id = "order ID"
            -        self.master_key_provider = aws_encryption_sdk.KMSMasterKeyProvider(key_ids=[key_id_east])
            +        master_key_provider = aws_encryption_sdk.KMSMasterKeyProvider(key_ids=[key_id_east])
            +        cache = aws_encryption_sdk.LocalCryptoMaterialsCache(capacity=100)
            +        self.materials_manager = aws_encryption_sdk.CachingCryptoMaterialsManager(
            +            cache=cache, master_key_provider=master_key_provider, max_age=5.0 * 60.0, max_messages_encrypted=10
            +        )

                 def encrypt(self, data):
                     """Encrypt data.
            @@ -38,12 +43,12 @@ class EncryptDecrypt(object):
                     :returns: Base64-encoded, encrypted data
                     :rtype: str
                     """
            -        encryption_context = {self._message_type: self._type_order_inquiry}
            -        order_id = data.get("orderid", "")
            -        if order_id:
            -            encryption_context[self._order_id] = order_id
            +        encryption_context = {
            +            self._message_type: self._type_order_inquiry,
            +            self._timestamp: str(int(time.time() / 3600.0)),
            +        }
                     ciphertext, _header = aws_encryption_sdk.encrypt(
            -            source=json.dumps(data), key_provider=self.master_key_provider, encryption_context=encryption_context
            +            source=json.dumps(data), materials_manager=self.materials_manager, encryption_context=encryption_context
                     )
                     return base64.b64encode(ciphertext).decode("utf-8")

            @@ -54,7 +59,7 @@ class EncryptDecrypt(object):
                     :returns: JSON-decoded, decrypted data
                     """
                     ciphertext = base64.b64decode(data)
            -        plaintext, header = aws_encryption_sdk.decrypt(source=ciphertext, key_provider=self.master_key_provider)
            +        plaintext, header = aws_encryption_sdk.decrypt(source=ciphertext, materials_manager=self.materials_manager)

                     try:
                         if header.encryption_context[self._message_type] != self._type_order_inquiry:


