
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

How the caching feature works
=============================

You enable the caching feature of the AWS Encryption SDK by creating a
"`caching crypto materials manager
<https://docs.aws.amazon.com/encryption-sdk/latest/developer-guide/implement-caching.html>`_"
and using it instead of your master key in encrypt and decrypt operations.
Crypto materials managers are plugins that can manipulate encrypt and decrypt
requests in certain ways.

When caching is enabled, the Encryption SDK generates a data key the first time
encrypt is invoked, then re-uses it for subsequent messages. On decrypt, we
conversely remember the mapping from encrypted data key to decrypted data key,
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

        We already have all the imports we need. :)


Then, we'll replace our MasterKey field with a CryptoMaterialsManager:

.. tabs::

    .. group-tab:: Java

        .. code-block:: java
           :lineno-start: 54

            private final CryptoMaterialsManager materialsManager;

        It's important to make this a field instead of initializing it for each call,
        as we need the cache to persist from one call to the next.

        In our constructor, we'll set up our master key, cache, and caching materials manager:

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

        We'll set up the master key provider, cache, and caching materials manager in our ``__init__``:

        .. code-block:: python
           :lineno-start: 32

            master_key_provider = aws_encryption_sdk.KMSMasterKeyProvider(key_ids=[key_id])
            cache = aws_encryption_sdk.LocalCryptoMaterialsCache(capacity=100)
            self.materials_manager = aws_encryption_sdk.CachingCryptoMaterialsManager(
                cache=cache,
                master_key_provider=master_key_provider,
                max_age=5.0 * 60.0,
                max_messages_encrypted=10
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
           :lineno-start: 49

            ciphertext, _header = aws_encryption_sdk.encrypt(
                source=json.dumps(data),
                materials_manager=self.materials_manager,
                encryption_context=encryption_context
            )

        And in ``decrypt``, change how you compute ``plaintext``:

        .. code-block:: python
           :lineno-start: 61

            plaintext, header = aws_encryption_sdk.decrypt(
                source=ciphertext,
                materials_manager=self.materials_manager
            )

Once you finish the changes, use the appropriate :ref:`Build tool commands` to
deploy and try sending a few messages in a row. You'll see that only one message
out of ten result in a KMS call, for both send and receive.

Encryption context issues
=========================

If you followed the previous exercise to the end, you'll remember we added the
order ID to the encryption context. If not, now's a good time to add it.

Try sending a few messages in a row with different order IDs. You'll note that
the cache doesn't work in this case; this is because messages with different
encryption contexts cannot use the same cached result.

This illustrates the balance that needs to be struck between cache performance,
access control verification, and audit log verbosity: improving cache performance
requires reducing the fidelity of the other two elements.

To get benefit from caching here, we'll need to strike a different balance. For
example, instead of putting the order ID in the audit log, we could put an
*approximate* timestamp, like so:

.. tabs::

    .. group-tab:: Java

        .. code-block:: java
           :lineno-start: 90

            context.put("approximate timestamp", "" + (System.currentTimeMillis() / 3_600_000) * 3_600_000);

    .. group-tab:: Python

        .. code-block:: python
           :lineno-start: 45

            encryption_context = {
                self._message_type: self._type_order_inquiry,
                self._timestamp: str(int(time.time() / 3600.0)),
            }

This puts a timestamp, rounded down to the nearest hour, in the context. This
provides us a certain degree of information about what data is being decrypted,
without ruining the usefulness of the cache.
