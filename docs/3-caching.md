You may have noticed by now that when you send or receive messages, we report
the number of KMS calls issued. Currently, every message you send or receive
incurs a single KMS call. While KMS calls are inexpensive and have high default
limits, when you write an application that performs encrypt or decrypt
operations at extremely high volumes, you may find the overhead of performing a
KMS call every time you encrypt and decrypt to be limiting.

In this exercise we'll explore the caching feature of the AWS Encryption SDK
and how it can help mitigate this issue.

# Before we start

We'll assume that you've completed the code changes in [exercise
2](2-encryption-sdk.md) first. If you haven't, you can use this git command to
catch up:

    git checkout -f -B exercise-3 origin/exercise-3-start

This will give you a codebase that already has the base64 changes applied.
Note that any uncommitted changes you've made already will be lost.

# How the caching feature works

You enable the caching feature of the AWS Encryption SDK by creating a
"[caching crypto materials
manager](http://docs.aws.amazon.com/encryption-sdk/latest/developer-guide/implement-caching.html)"
and using it instead of your master key in encrypt and decrypt operations.
Crypto materials managers are plugins that can manipulate encrypt and decrypt
requests in certain ways.

When caching is enabled, the Encryption SDK generates a data key the first time
encrypt is invoked, then re-uses it for subsequent messages. On decrypt, we
conversely remember the mapping from encrypted data key to decrypted data key,
and reuse that as well.

The code changes for this are fairly small, so let's jump right into it.

## Step by step

First, we'll add the new imports we'll need:

    import java.util.concurrent.TimeUnit;
    import com.amazonaws.encryptionsdk.CryptoMaterialsManager;
    import com.amazonaws.encryptionsdk.caching.CachingCryptoMaterialsManager;
    import com.amazonaws.encryptionsdk.caching.LocalCryptoMaterialsCache;

Then, we'll replace our MasterKey field with a CryptoMaterialsManager:

    private final CryptoMaterialsManager materialsManager;

It's important to make this a field instead of initializing it for each call,
as we need the cache to persist from one call to the next.

In our constructor, we'll set up our master key, cache, and caching materials manager:

    KmsMasterKey masterKey = new KmsMasterKeyProvider(keyId)
        .getMasterKey(keyId);

    LocalCryptoMaterialsCache cache = new LocalCryptoMaterialsCache(100);
    materialsManager = CachingCryptoMaterialsManager.newBuilder()
        .withMaxAge(5, TimeUnit.MINUTES)
        .withMasterKeyProvider(masterKey)
        .withMessageUseLimit(10)
        .withCache(cache)
        .build();

And finally, we'll use the materialsManager instead of our masterKey in our
encrypt and decrypt operations:

    byte[] ciphertext = new AwsCrypto().encryptData(materialsManager, plaintext, context).getResult();

    // ...

    CryptoResult<byte[], ?> result = new AwsCrypto().decryptData(materialsManager, ciphertextBytes);

Once you finish the changes, `mvn deploy` and try sending a few messages in a
row. You'll see that only one message out of ten result in a KMS call, for both
send and receive.

# Encryption context issues

If you followed the previous exercise to the end, you'll remember we added the
order ID to the encryption context. If not, now's a good time to add it.

Try sending a few messages in a row with different order IDs. You'll note that
the cache doesn't work in this case; this is because messages with different
encryption contexts cannot use the same cached result.

This illustrates the balance that needs to be struck between cachability and
audit log verbosity; if we put too much detail in our audit logs, then caching
won't do us any good.

To get benefit from caching here, we'll need to strike a different balance. For
example, instead of putting the order ID in the audit log, we could put an
_approximate_ timestamp, like so:

    context.put("approximate timestamp", "" + (System.currentTimeMillis() / 3_600_000) * 3_600_000);

This puts a timestamp, rounded down to the nearest hour, in the context. This
provides us a certain degree of information about what data is being decrypted,
without ruining the usefulness of the cache.
