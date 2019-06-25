
.. _Exercise 5:

******************************************
Exercise 5: Multiple Master Key Encryption
******************************************

So far we have been working with the AWS Encryption SDK using a single Customer Master Key (CMK) to perform
encryption and decryption. We will now be exploring the multi-CMK capability of the AWS Encryption SDK
by using Multiple Master Key Encryption.

Before we start
===============

This exercise builds off of Exercise 3 and runs in parallel
to Exercise 4. We'll assume that you've completed the code changes in
:ref:`Exercise 3` first. If you haven't, you can use this git command to catch up:

.. tabs::

    .. group-tab:: Java

        .. code-block:: bash

            git checkout -f -B exercise-5  origin/exercise-5-start-java

    .. group-tab:: Python

        .. code-block:: bash

            git checkout -f -B exercise-5 origin/exercise-5-start-python

This will give you a codebase that already uses the AWS Encryption SDK.
Note that any uncommitted changes you've made already will be lost.

The :ref:`complete change<ex5-change>` is also available to help you view changes in context
and compare your work.


Introduction to Multiple Master Key Encryption
==============================================

The Encryption SDK uses envelope encryption with data keys protected by KMS. One of the benefits of envelope encryption
is that it supports methods of encrypting the message to explicitly grant access to holders of different keys.

One method to grant multiple accesses to an encrypted message is to encrypt a message's data key using multiple KMS CMKs.
These CMKs can even be CMKs in different AWS accounts or different AWS regions.
You explicitly configure which CMKs, in which regions, your application will use.

Use cases for this pattern include cross-region replication for high availability and backup, as well as sharing data
between different custodians who control different CMKs.

For high availability use cases, encrypting the data key with KMS CMKs in different regions allows each region to have
independent access to the encrypted data without requiring the other region to be accessible. It also allows data to be
replicated between regions in its encrypted form without re-encrypting it.

The multiple CMKs don't have to be in different regions, though. Multiple CMKs from different accounts in the same
region can be used, for example, to let different parties who own different CMKs independently manage access to the data.
It is also a way to mitigate risk from deletion of a KMS CMK.

Overview of exercise
====================

In this exercise you will:

#. Configure the AWS Encryption SDK to use multiple Customer Master Keys (CMKs) to protect a message.
#. Add and remove access to one of the CMKs.
#. Observe and confirm that you are still able to encrypt and decrypt even when one CMK is not accessible.

Step by step
------------

First, let's make sure the dependencies are setup correctly.

.. tabs::

    .. group-tab:: Java

        Open up ``webapp/pom.xml`` and ensure this block is in the ``<dependencies>`` section:

        .. code-block:: xml

                <dependency>
                    <groupId>com.amazonaws</groupId>
                    <artifactId>aws-encryption-sdk-java</artifactId>
                    <version>1.6.0</version>
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

            import com.amazonaws.encryptionsdk.MasterKeyProvider;
            import com.amazonaws.encryptionsdk.multi.MultipleProviderFactory;

    .. group-tab:: Python

            No additional imports needed.

:ref:`master-keys` are used by the AWS Encryption SDK to protect your data.
In Exercise 3, you configured a Master Key and Master Key Provider for a single KMS CMK. Now you will extend this to
configure a Multiple Master Key Provider with a CMK in the demo application's primary region, us-east-2, as well as
in a secondary region, us-west-2. The CloudFormation template automatically creates these two CMKs for you, so now
all that's left is to configure the Encryption SDK to use them both.

.. tabs::

    .. group-tab:: Java

        Just like before, you'll create a Master Key Provider (MKP). This time you'll use a ``MultipleProviderFactory``
        to configure a MKP with more than one Master Key. Here is the code in a helper function:

        .. code-block:: java
           :lineno-start: 60

            private static MasterKeyProvider<?> getKeyProvider(KmsMasterKey masterKeyEast, KmsMasterKey masterKeyWest) {
                return MultipleProviderFactory.buildMultiProvider(masterKeyWest, masterKeyEast);
            }



    .. group-tab:: Python

        Just like before, you'll create a Master Key Provider (MKP). This time you'll add multiple Master Keys, one for
        each CMK, to the MKP configuration. Here is the code in a helper function:

        .. code-block:: python
           :lineno-start: 66

            def construct_multiregion_kms_master_key_provider(self, key_id_east, key_id_west):
            """Generate Multiple Master Key Provider."""
                kms_master_key_provider = aws_encryption_sdk.KMSMasterKeyProvider()
                kms_master_key_provider.add_master_key(key_id_west)
                kms_master_key_provider.add_master_key(key_id_east)

            return kms_master_key_provider

Now you have a Master Key Provider with multiple Master Keys configured. Using this MKP configures the Encryption SDK to
use multiple CMKs for cryptographic operations.

Note that the us-west-2 key is the first configured key. For encrypt operations, the first configured Master Key
is significant: it is the key used for the ``kms:GenerateDataKey`` operation. Any other configured keys are used to
re-encrypt that data key, with those additional encrypted copies written to the envelope in
the `Encryption SDK's message format`_.

For decrypt operations, the configured Master Keys determine which CMKs the Encryption SDK may attempt to use to
decrypt the data key.

You'll see more about each of these behaviors in a minute.

.. _Encryption SDK's message format: https://docs.aws.amazon.com/encryption-sdk/latest/developer-guide/message-format.html

.. tabs::

    .. group-tab:: Java

        Now you have two Master Keys to use in your encryption operations, so modify ``MasterKey`` to ``MasterKeyEast``
        for the CMK in us-east-2 and add ``MasterKeyWest`` for the CMK in us-west-2. Add ``MasterKeyProvider``
        for the Multi Master Key Provider.

        .. code-block:: java
           :lineno-start: 59

            private final KmsMasterKey masterKeyEast;
            private final KmsMasterKey masterKeyWest;
            private final MasterKeyProvider<?> provider;

        In your constructor, you can create the Master Keys like so:

        .. code-block:: java
           :lineno-start: 73

            kms = AWSKMSClient.builder().build();
            this.masterKeyEast = new KmsMasterKeyProvider(keyIdEast)
                .getMasterKey(keyIdEast);
            this.masterKeyWest = new KmsMasterKeyProvider(keyIdWest)
                .getMasterKey(keyIdWest);

        In your constructor, you can use the helper function to create the Master Key Provider using the Master Keys:

        .. code-block:: java
           :lineno-start: 78

            this.provider = getKeyProvider(masterKeyEast, masterKeyWest)

    .. group-tab:: Python

        Now you need to update ``__init__`` to replace the ``master_key_provider`` initialization with the new Multi
        Master Key Provider:

        .. code-block:: python
           :lineno-start: 31

            self.master_key_provider = self.construct_multiregion_kms_master_key_provider(key_id_east, key_id_west)

.. tabs::

    .. group-tab:: Java

        Encrypt needs to be updated to use the multi Master Key Provider, but otherwise everything mostly stays the same.

        .. code-block:: java
           :lineno-start: 81

            public String encrypt(JsonNode data) throws IOException {
                FormData formValues = MAPPER.treeToValue(data, FormData.class);

                // We can access specific form fields using values in the parsed FormData object.
                LOGGER.info("Got form submission for order " + formValues.orderid);

                byte[] plaintext = MAPPER.writeValueAsBytes(formValues);

                HashMap<String, String> context = new HashMap<>();
                context.put(K_MESSAGE_TYPE, TYPE_ORDER_INQUIRY);

                byte[] ciphertext = new AwsCrypto().encryptData(provider, plaintext, context).getResult();

                return Base64.getEncoder().encodeToString(ciphertext);
            }

    .. group-tab:: Python

        Encrypt is already using the ``KMSMasterKeyProvider``, so it automatically picks up the change to use multiple
        Master Keys / CMKs.

Recall that your Master Key Provider is configured with the us-west-2 CMK first, and the us-east-2 CMK second. Now what
will happen on ``encrypt`` is that the Encryption SDK will call ``kms:GenerateDataKey`` on the us-west-2 CMK, and receive a new
data key from KMS in response. The Encryption SDK will call ``kms:Encrypt`` on that data key in us-east-2, producing a new encrypted
copy of the same plaintext data key. Your message will be encrypted with that plaintext data key, producing your message
ciphertext. Then both the us-west-2 encrypted data key and the us-east-2 encrypted data key will be written alongside
that ciphertext in the envelope-encrypted Encryption SDK message format.

Now that message can be stored or transmitted wherever it needs to go securely, and access to either the us-west-2 key
or the us-east-2 key is sufficient to access the plaintext.


.. tabs::

    .. group-tab:: Java

        The change to decrypt looks similar to the change to encrypt:

        .. code-block:: java
           :lineno-start: 100

            public JsonNode decrypt(String ciphertext) throws IOException {
                byte[] ciphertextBytes = Base64.getDecoder().decode(ciphertext);

                CryptoResult<byte[], ?> result = new AwsCrypto().decryptData(provider, ciphertextBytes);

                // Check that we have the correct type
                if (!Objects.equals(result.getEncryptionContext().get(K_MESSAGE_TYPE), TYPE_ORDER_INQUIRY)) {
                    throw new IllegalArgumentException("Bad message type in decrypted message");
                }

                return MAPPER.readTree(result.getResult());
            }

    .. group-tab:: Python

        Decrypt is already using the ``KMSMasterKeyProvider``, so it automatically picks up the change to use multiple
        Master Keys / CMKs.

Now that you have configured your Encryption SDK to use multiple Master Keys, the Encryption SDK can try multiple CMKs on decrypt.
This means that if the Encryption SDK tries to use a CMK but can't, perhaps because it does not have permissions to use that CMK,
it has another CMK option to try before giving up.

When using KMS CMKs, recall that KMS checks access permissions for every call and writes an audit log entry both on
success and on failure. This behavior is completely independent from the configuration of the Encryption SDK. Your Encryption SDK configuration
constrains what your application behavior will be, but your KMS configuration is the final arbiter of which operations
will succeed and which will fail. Either way, KMS always writes a log entry to CloudTrail on every attempt to use a CMK.

You'll see this behavior in action in just a minute. For now, use the :ref:`Build tool commands` to deploy your
application again.

Illustrating Multi-CMK Usage
============================

Now that you have configured your client to use multiple Master Keys, you'll work through an example scenario of how this
behavior can work in practice.

The us-west-2 key that we set up for you has a restricted set of permissions. You may call ``kms:GenerateDataKey``, but not
``kms:Encrypt`` or ``kms:Decrypt``. When you send a message through your web application, you will see two KMS calls now: one for
the ``kms:GenerateDataKey`` in us-west-2, and one for the ``kms:Encrypt`` call in us-east-2.

If you use the receive message function and observe your KMS logs right now, you will see the Encryption SDK attempting to use your
us-west-2 CMK for ``kms:Decrypt``, failing, and moving on to your us-east-2 CMK.

Give that a test run by sending a few test messages now and checking your application logs and your CloudTrail logs for
your us-west-2 CMK and your us-east-2 CMK. Come back and proceed further after you've had a chance to see that in action.

* `Click here for CloudTrail in us-east-2`_
* `Click here for CloudTrail in us-west-2`_
* `Click here for CloudWatch Logs in us-east-2 filtered to Lambda`_

.. _Click here for CloudTrail in us-east-2: https://us-east-2.console.aws.amazon.com/cloudtrail/home?region=us-east-2#/events
.. _Click here for CloudTrail in us-west-2: https://us-east-2.console.aws.amazon.com/cloudtrail/home?region=us-west-2#/events
.. _Click here for CloudWatch Logs in us-east-2 filtered to Lambda: https://us-east-2.console.aws.amazon.com/cloudwatch/home?region=us-east-2#logs:prefix=/aws/lambda/busy-engineers-

Adding CMK access through Grants
--------------------------------

One of the access control primitives offered by KMS is `Grants`_. Grants are designed for modular permissions definitions
and work in conjunction with Key Policies as part of AWS KMS' access control features.

Now you'll use KMS Grants to give yourself permission to use the us-west-2 CMK for more operations
and observe in logs how the behavior changes. Then you can revoke the permission and watch the behavior change again.

The grant assignment and revocation are already scripted for you, but you're welcome to take a peek to see what it looks
like to do yourself, `in Java`_ or `using the AWS CLI`_.

.. _Grants: https://docs.aws.amazon.com/kms/latest/developerguide/grants.html
.. _in Java: https://github.com/aws-samples/busy-engineers-encryption-sdk/blob/a810c76317d51c90988d806606f06dbc62114382/deploy-plugin/src/main/java/sample/AssignGrantPlugin.java#L69
.. _using the AWS CLI: https://github.com/aws-samples/busy-engineers-encryption-sdk/blob/0ad93fb1e8cd720df4bc8f9a4bbb9c3a7cfb3ed6/build-tools/assign_grant.sh#L12

.. tabs::

    .. group-tab:: Java

        Use the ``assign-grant`` goal on ``deploy`` to add your grant.

        .. code-block:: bash

            mvn deploy -P"assign-grant"

    .. group-tab:: Python

        Use the ``assign-grant`` target in ``tox`` to add your grant.

        .. code-block:: bash

            tox -e assign-grant

Go send some new messages through your application. No need to redeploy. Check your application logs again and your
CloudTrail logs for your CMKs (keep in mind that there is a few minutes' propagation delay). With the grant in place,
now you should see your us-west-2 CMK being used successfully in operations where it was unsuccessful before.

Once you are done validating, go ahead and revoke the grant to see the application return to using the CMK in
us-east-2.

.. tabs::

    .. group-tab:: Java

        Use the ``revoke-grant`` goal on ``deploy`` to revoke your grant.

        .. code-block:: bash

            mvn deploy -P"revoke-grant"


    .. group-tab:: Python

        Use the ``revoke-grant`` target in ``tox`` to revoke your grant.

        .. code-block:: bash

            tox -e revoke-grant

You can now go back to the CloudWatch logs and see the application continue to successfully use the key in us-east-2,
while the us-west-2 key will start failing Decrypt permissions checks again now that the grant is gone.

Another good place to see the multi-CMK use in effect is to visit the CloudTrail events for KMS. Here you
will be able to see each request that comes to KMS, whether successful or unsuccessful.

Summing up
==========

Even though ``kms:Decrypt`` permission for your application to use us-west-2's CMK has been added and revoked at this point, your
application has continued to function the entire time. In addition to your application logs, KMS also recorded audit
information in CloudTrail for every call it received.

You can use these same primitives in your real-world deployments to finely control access to your application and to
audit how and why data is being accessed.

Feel free to experiment with adding, removing, and changing permissions to see how your application behavior changes.

You can use the :ref:`Debugging Tips` for additional analysis options for your logs.

.. _ex5-change:

Complete change
---------------

View step-by-step changes in context, and compare your work if desired.

.. tabs::

    .. group-tab:: Java

        .. code:: diff

            diff --git a/webapp/src/main/java/example/encryption/EncryptDecrypt.java b/webapp/src/main/java/example/encryption/EncryptDecrypt.java
            index 906a136..d4d6bc0 100644
            --- a/webapp/src/main/java/example/encryption/EncryptDecrypt.java
            +++ b/webapp/src/main/java/example/encryption/EncryptDecrypt.java
            @@ -39,6 +39,9 @@ import com.amazonaws.services.kms.model.EncryptRequest;
             import com.amazonaws.services.kms.model.EncryptResult;
             import com.fasterxml.jackson.databind.JsonNode;

            +import com.amazonaws.encryptionsdk.MasterKeyProvider;
            +import com.amazonaws.encryptionsdk.multi.MultipleProviderFactory;
            +
             /**
              * This class centralizes the logic for encryption and decryption of messages, to allow for easier modification.
              *
            @@ -53,7 +56,9 @@ public class EncryptDecrypt {
                 private static final String K_ORDER_ID = "order ID";

                 private final AWSKMS kms;
            -    private final KmsMasterKey masterKey;
            +    private final KmsMasterKey masterKeyEast;
            +    private final KmsMasterKey masterKeyWest;
            +    private final MasterKeyProvider<?> provider;

                 @SuppressWarnings("unused") // all fields are used via JSON deserialization
                 private static class FormData {
            @@ -66,8 +71,11 @@ public class EncryptDecrypt {
                 @Inject
                 public EncryptDecrypt(@Named("keyIdEast") final String keyIdEast, @Named("keyIdWest") final String keyIdWest) {
                     kms = AWSKMSClient.builder().build();
            -        this.masterKey = new KmsMasterKeyProvider(keyIdEast)
            +        this.masterKeyEast = new KmsMasterKeyProvider(keyIdEast)
                         .getMasterKey(keyIdEast);
            +        this.masterKeyWest = new KmsMasterKeyProvider(keyIdWest)
            +            .getMasterKey(keyIdWest);
            +        this.provider = getKeyProvider(masterKeyEast, masterKeyWest);
                 }

                 public String encrypt(JsonNode data) throws IOException {
            @@ -84,7 +92,7 @@ public class EncryptDecrypt {
                         context.put(K_ORDER_ID, formValues.orderid);
                     }

            -        byte[] ciphertext = new AwsCrypto().encryptData(masterKey, plaintext, context).getResult();
            +        byte[] ciphertext = new AwsCrypto().encryptData(provider, plaintext, context).getResult();

                     return Base64.getEncoder().encodeToString(ciphertext);
                 }
            @@ -92,7 +100,7 @@ public class EncryptDecrypt {
                 public JsonNode decrypt(String ciphertext) throws IOException {
                     byte[] ciphertextBytes = Base64.getDecoder().decode(ciphertext);

            -        CryptoResult<byte[], ?> result = new AwsCrypto().decryptData(masterKey, ciphertextBytes);
            +        CryptoResult<byte[], ?> result = new AwsCrypto().decryptData(provider, ciphertextBytes);

                     // Check that we have the correct type
                     if (!Objects.equals(result.getEncryptionContext().get(K_MESSAGE_TYPE), TYPE_ORDER_INQUIRY)) {
            @@ -101,4 +109,7 @@ public class EncryptDecrypt {

                     return MAPPER.readTree(result.getResult());
                 }
            +    private static MasterKeyProvider<?> getKeyProvider(KmsMasterKey masterKeyEast, KmsMasterKey masterKeyWest) {
            +        return MultipleProviderFactory.buildMultiProvider(masterKeyWest, masterKeyEast);
            +    }
             }

    .. group-tab:: Python

        .. code:: diff

            diff --git a/src/busy_engineers_workshop/encrypt_decrypt.py b/src/busy_engineers_workshop/encrypt_decrypt.py
            index 4e153a3..b8785b1 100644
            --- a/src/busy_engineers_workshop/encrypt_decrypt.py
            +++ b/src/busy_engineers_workshop/encrypt_decrypt.py
            @@ -16,7 +16,6 @@ This is the only module that you need to modify in the Busy Engineer's Guide to
             """
             import base64
             import json
            -
             import aws_encryption_sdk


            @@ -29,7 +28,7 @@ class EncryptDecrypt(object):
                     self._type_order_inquiry = "order inquiry"
                     self._timestamp = "rough timestamp"
                     self._order_id = "order ID"
            -        self.master_key_provider = aws_encryption_sdk.KMSMasterKeyProvider(key_ids=[key_id_east])
            +        self.master_key_provider = self.construct_multiregion_kms_master_key_provider(key_id_east, key_id_west)

                 def encrypt(self, data):
                     """Encrypt data.
            @@ -63,3 +62,11 @@ class EncryptDecrypt(object):
                         raise ValueError("Bad message type in decrypted message")

                     return json.loads(plaintext)
            +
            +    def construct_multiregion_kms_master_key_provider(self, key_id_east, key_id_west):
            +        """Generate Multiple Master Key Provider."""
            +        kms_master_key_provider = aws_encryption_sdk.KMSMasterKeyProvider()
            +        kms_master_key_provider.add_master_key(key_id_west)
            +        kms_master_key_provider.add_master_key(key_id_east)
            +
            +        return kms_master_key_provider
