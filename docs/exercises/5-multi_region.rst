
.. _Exercise 5:

******************************************
Exercise 5: Multiple Master Key Encryption
******************************************

So far we have been working with the AWS Encryption SDK using a single Customer Master Key (CMK) to perform
encryption and decryption. We will now be exploring the multi region CMK capability of the AWS Encryption SDK
by using Multiple Master Key Encryption.

Before we start
===============

Note, that this exercise builds off of Exercise 3 and runs in parallel
to Exercise 4. We'll assume that you've completed the code changes in
:ref:`Exercise 3` first. If you haven't, you can use this git command to catch up:

.. tabs::

    .. group-tab:: Java

        .. code-block:: bash

            git checkout -f -B exercise-5  origin/exercise-5-start-java

    .. group-tab:: Python

        .. code-block:: bash

            git checkout -f -B exercise-5 origin/exercise-5-start-python

This will give you a codebase that already uses the AWS Encryption SDK
Note that any uncommitted changes you've made already will be lost.

The :ref:`complete change<ex4-change>` is also available to help you view changes in context
and compare your work.


How multiple master key encryption works
========================================

Encrypting application secrets under multiple regional KMS master keys
increases availability. Many customers want to build systems that not
only span multiple Availability Zones, but also multiple regions. KMS
does not allow you to share KMS customer master keys (CMKs) in different
regions.

There is a workaround to this limitation. By using envelope encryption,
you can encrypt the data key with KMS CMKs in different regions. Applications
running in each region can then use the local KMS endpoint to decrypt data
faster and with higher availability. This also allows your data to be recovered
using the key from another region if the local KMS CMK has been corrupted,
disabled, or deleted.

Overview of exercise
====================

In this exercise we'll:

#. Implement encryption using a multiple master key provider
#. Utilize KMS grants to disable access to one of the CMKs
#. Observe via logs and the application how we are still able to encrypt/decrypt the data

Step by step
------------

First, let's make sure the dependencies are setup correctly.


.. tabs::

    .. group-tab:: Java

        Open up ``webapp/pom.xml`` and add this block in the ``<dependencies>`` section:

        .. code-block:: xml

                <dependency>
                    <groupId>com.amazonaws</groupId>
                    <artifactId>aws-java-sdk-core</artifactId>
                    <version>1.11.213</version>
                </dependency>

                <dependency>
                    <groupId>com.amazonaws</groupId>
                    <artifactId>aws-java-sdk-sts</artifactId>
                    <version>1.11.213</version>
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

        .. code-block:: python
           :lineno-start: 21

            import aws_encryption_sdk
            import boto3

:ref:`master-keys` are used by the AWS Encryption SDK to protect your data.
The first step to setting up multiple master keys is setting up a Master Key
Provider. When setting up our Master Key Provider, we will be adding a local
master key (the key for the region, us-east-2, we are currently in) and a key
in another region. Please note, the cloud formation template has already created
the keys for you in two different regions, us-east-2 and us-west-2.

.. tabs::

    .. group-tab:: Java

        First, we will need to write some code to create a master key provider containing multiple
        CMKs. We will create a single master key provider to which all the CMKs are added. Note that
        the first master key added to the master key provider is the one used to generate the new data
        key and the other master keys are used to encrypt the new data key. We will use MultipleProviderFactory
        to combine all the master keys into a single master key provider. We will construct the master keys
        to pass to the ``getKeyProvider`` after this.

        .. code-block:: java
           :lineno-start: 60

            private static MasterKeyProvider<?> getKeyProvider(KmsMasterKey masterKeyEast, KmsMasterKey masterKeyWest) {
                return MultipleProviderFactory.buildMultiProvider(masterKeyEast, masterKeyWest);
            }


    .. group-tab:: Python

        First, we will need to write some code to create a master key provider containing multiple
        CMKs. We will create a single ``KMSMasterKeyProvider`` to which all the CMKs are added. Note that
        the first master key added to the ``KMSMasterKeyProvider`` is the one used to generate the new data
        key and the other master keys are used to encrypt the new data key.

        .. code-block:: python
           :lineno-start: 66

             def construct_multiregion_kms_master_key_provider(self, key_id_east):
                alias_west = 'alias/busy-engineers-workshop-python-key-us-west-2'
                arn_template = 'arn:aws:kms:{region}:{account_id}:{alias}'

                kms_master_key_provider = aws_encryption_sdk.KMSMasterKeyProvider()
                account_id = boto3.client('sts').get_caller_identity()['Account']

                kms_master_key_provider.add_master_key(key_id_east)
                kms_master_key_provider.add_master_key(arn_template.format(
                    region="us-west-2",
                    account_id=account_id,
                    alias=alias_west
                ))
                return kms_master_key_provider

.. tabs::

    .. group-tab:: Java

        We won't need the class attribute for ``MasterKey``, so modify that with ``MasterKeyEast``
        for the key in us-east-2 and ``MasterKeyWest`` for the key in us-west-2. Add ``MasterKeyProvider``
        for the KMS Master Key Provider.

        .. code-block:: java
           :lineno-start: 60

            private final KmsMasterKey masterKeyEast;
            private final KmsMasterKey masterKeyWest;
            private final MasterKeyProvider<?> provider;

        In our constructor, we'll create the Master Keys like so:

        .. code-block:: java
           :lineno-start: 75

            kms = AWSKMSClient.builder().build();
            //Get Master Keys from East and West
            this.masterKeyEast = new KmsMasterKeyProvider(keyId).getMasterKey(keyId);
            String[] arrOfStr = keyId.split(":");
            String accountId = arrOfStr[4];
            String keyIdWest = "arn:aws:kms:us-west-2:" + accountId +
                ":alias/busy-engineers-encryption-sdk-key-us-west-2-eek";
            this.masterKeyWest = new KmsMasterKeyProvider(keyIdWest).getMasterKey(keyIdWest);
            //Construct Master Key Provider
            this.provider = getKeyProvider(masterKeyEast, masterKeyWest);

        In our constructor, we'll create the Master Key Provider and pass in the Master Keys like so:

        .. code-block:: java
           :lineno-start: 85

            this.masterKeyProvider = getMasterKeyProvider(masterKeyEast, masterKeyWest)

    .. group-tab:: Python

        We will be constructing a new multi region KMS Master Key Provider, so replace the call to the
        KMSMasterKeyProvider in ``__init__`` with a call to our multi region KMS Master Key Provider constructor.

        .. code-block:: python
           :lineno-start: 32

            self.master_key_provider = self.construct_multiregion_kms_master_key_provider(key_id)


For encrypt, everything mostly stays the same, we just need to make sure we are passing in the master key
provider.

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

                byte[] ciphertext = new AwsCrypto().encryptData(provider, plaintext, context).getResult();

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

For decrypt, we just need to make sure we are passing in the master key provider.

.. tabs::

    .. group-tab:: Java

        .. code-block:: java
           :lineno-start: 92

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

Illustrating Multi Region CMKs Usage
====================================

Now that you are done making the necessary code changes we will be leveraging grants to prevent usage of the local
key to illustrate that encryption and decryption is still possible by using a key in another region. Grants are one
of the supported resource based access control mechanisms that allow you to programmatically delegate the use of CMKs.
Grants enable more granular permissions management.

In this portion of the exercise, we will be adding a grant to the local key in us-east-2 that will block the key from
being accessed. In this grant, we will be adding an encryption context constraint that will only permit use of the key
if the encryption context matches the one specified in the grant explicitly. Since we do not want to permit access to
the key, we will be setting the encryption context equal to {'key use':'bad key'}. Upon key retrieval, this grant will
block access to the key and the Master Key Provider will use the key in us-west-2 and you will see that the
encryption operations are still successful.

.. tabs::

    .. group-tab:: Java

        We have built a simple bash script that sets the grant, thereby disabling the use of the local key. Run
        the script as below.

        Note, be sure to save the grant_id that outputs to the CLI. You will need this to revoke the grant.

        .. code-block:: bash

            ./assign_grant.sh

    .. group-tab:: Python

        We have built a simple python script that sets the grant, thereby disabling the use of the local key. Run
        the script on the cloud9 CLI as below.

        Note, be sure to save the grant_id that outputs to the CLI. You will need this to revoke the grant.

        .. code-block:: bash

            ./assign_grant.sh

Now go ahead and send some new encrypted data to the SQS queue in the web interface. Then visit the backend logs
in cloudwatch to see that the ciphertext was encrypted using the key from us-west-2. Afterwards, go ahead
and retrieve the data. Taking a look at the backend logs in cloudwatch, you will see that the key from us-west-2
is used to decrypt the data as well.

Once you are done validating, go ahead and revoke the grant to see the application return back to using the local
us-east-2 key for encryption/decryption.

.. tabs::

    .. group-tab:: Java

        We have built a simple python script that revokes the grant, thereby enabling the use of the local key. Run
        the script on the cloud9 CLI as below.

        Be sure to put the grant_id you saved from assigning the grant in the shell script and run as below.

        .. code-block:: bash

            ./revoke_grant.sh


    .. group-tab:: Python

        We have built a simple python script that revokes the grant, thereby enabling the use of the local key. Run
        the script on the cloud9 CLI as below.

        Be sure to put the grant_id you saved from assigning the grant in the shell script and run as below.

        .. code-block:: bash

            ./revoke_grant.sh

You can now go back to the cloudwatch logs and see the application return to using the local key in us-east-2 for
encryption and decryption.

Another good place to see the multi region CMK use in effect is to visit the cloudtrail events for KMS. Here you
will be able to see each request that comes to KMS. You can use the debugging tips to help narrow done your
results.

.. _ex4-change:

Complete change
---------------

View step-by-step changes in context, and compare your work if desired.

.. tabs::

    .. group-tab:: Java

        .. code:: diff

            diff --git a/webapp/src/main/java/example/encryption/EncryptDecrypt.java b/webapp/src/main/java/example/encryption/EncryptDecrypt.java
            index b544d59..65828bd 100644
            --- a/webapp/src/main/java/example/encryption/EncryptDecrypt.java
            +++ b/webapp/src/main/java/example/encryption/EncryptDecrypt.java
            @@ -39,6 +39,10 @@ import com.amazonaws.services.kms.model.EncryptRequest;
             import com.amazonaws.services.kms.model.EncryptResult;
             import com.fasterxml.jackson.databind.JsonNode;

            +import com.amazonaws.encryptionsdk.MasterKeyProvider;
            +import com.amazonaws.encryptionsdk.multi.MultipleProviderFactory;
            +
            +
             /**
              * This class centralizes the logic for encryption and decryption of messages, to allow for easier modification.
              *
            @@ -53,7 +57,9 @@ public class EncryptDecrypt {
                 private static final String K_ORDER_ID = "order ID";

                 private final AWSKMS kms;
            -    private final KmsMasterKey masterKey;
            +    private final KmsMasterKey masterKeyEast;
            +    private final KmsMasterKey masterKeyWest;
            +    private final MasterKeyProvider<?> provider;

                 @SuppressWarnings("unused") // all fields are used via JSON deserialization
                 private static class FormData {
            @@ -66,8 +72,17 @@ public class EncryptDecrypt {
                 @Inject
                 public EncryptDecrypt(@Named("keyId") final String keyId) {
                     kms = AWSKMSClient.builder().build();
            -        this.masterKey = new KmsMasterKeyProvider(keyId)
            +        //Get Master Keys from East and West
            +        this.masterKeyEast = new KmsMasterKeyProvider(keyId)
                         .getMasterKey(keyId);
            +        String[] arrOfStr = keyId.split(":");
            +        String accountId = arrOfStr[4];
            +        String keyIdWest = "arn:aws:kms:us-west-2:" + accountId +
            +            ":alias/busy-engineers-encryption-sdk-key-us-west-2-eek";
            +        this.masterKeyWest = new KmsMasterKeyProvider(keyIdWest).getMasterKey(keyIdWest);
            +        //Construct Master Key Provider
            +        this.provider = getKeyProvider(masterKeyEast, masterKeyWest);
            +
                 }

                 public String encrypt(JsonNode data) throws IOException {
            @@ -84,7 +99,7 @@ public class EncryptDecrypt {
                         context.put(K_ORDER_ID, formValues.orderid);
                     }

            -        byte[] ciphertext = new AwsCrypto().encryptData(masterKey, plaintext, context).getResult();
            +        byte[] ciphertext = new AwsCrypto().encryptData(provider, plaintext, context).getResult();

                     return Base64.getEncoder().encodeToString(ciphertext);
                 }
            @@ -92,13 +107,17 @@ public class EncryptDecrypt {
                 public JsonNode decrypt(String ciphertext) throws IOException {
                     byte[] ciphertextBytes = Base64.getDecoder().decode(ciphertext);

            -        CryptoResult<byte[], ?> result = new AwsCrypto().decryptData(masterKey, ciphertextBytes);
            +        CryptoResult<byte[], ?> result = new AwsCrypto().decryptData(provider, ciphertextBytes);

                     // Check that we have the correct type
                     if (!Objects.equals(result.getEncryptionContext().get(K_MESSAGE_TYPE), TYPE_ORDER_INQUIRY)) {
                         throw new IllegalArgumentException("Bad message type in decrypted message");
                     }
            -
                     return MAPPER.readTree(result.getResult());
                 }
            +
            +    private static MasterKeyProvider<?> getKeyProvider(KmsMasterKey masterKeyEast, KmsMasterKey masterKeyWest) {
            +        return MultipleProviderFactory.buildMultiProvider(masterKeyEast, masterKeyWest);
            +    }
            +
             }

    .. group-tab:: Python

        .. code:: diff

            diff --git a/src/busy_engineers_workshop/encrypt_decrypt.py b/src/busy_engineers_workshop/encrypt_decrypt.py
            index 256397f..09fdef0 100644
            --- a/src/busy_engineers_workshop/encrypt_decrypt.py
            +++ b/src/busy_engineers_workshop/encrypt_decrypt.py
            @@ -11,10 +11,13 @@
             # ANY KIND, either express or implied. See the License for the specific
             # language governing permissions and limitations under the License.
             """Helper class to handle encryption.
            +
             This is the only module that you need to modify in the Busy Engineer's Guide to the Encryption SDK workshop.
             """
             import base64
             import json
            +import time
            +import boto3

             import aws_encryption_sdk

            @@ -28,10 +31,11 @@ class EncryptDecrypt(object):
                     self._type_order_inquiry = "order inquiry"
                     self._timestamp = "rough timestamp"
                     self._order_id = "order ID"
            -        self.master_key_provider = aws_encryption_sdk.KMSMasterKeyProvider(key_ids=[key_id])
            +        self.master_key_provider = self.construct_multiregion_kms_master_key_provider(key_id)

                 def encrypt(self, data):
                     """Encrypt data.
            +
                     :param data: JSON-encodeable data to encrypt
                     :returns: Base64-encoded, encrypted data
                     :rtype: str
            @@ -47,6 +51,7 @@ class EncryptDecrypt(object):

                 def decrypt(self, data):
                     """Decrypt data.
            +
                     :param bytes data: Base64-encoded, encrypted data
                     :returns: JSON-decoded, decrypted data
                     """
            @@ -60,3 +65,18 @@ class EncryptDecrypt(object):
                         raise ValueError("Bad message type in decrypted message")

                     return json.loads(plaintext)
            +
            +    def construct_multiregion_kms_master_key_provider(self, key_id_east):
            +        alias_west = 'alias/busy-engineers-workshop-python-key-us-west-2'
            +        arn_template = 'arn:aws:kms:{region}:{account_id}:{alias}'
            +
            +        kms_master_key_provider = aws_encryption_sdk.KMSMasterKeyProvider()
            +        account_id = boto3.client('sts').get_caller_identity()['Account']
            +
            +        kms_master_key_provider.add_master_key(key_id_east)
            +        kms_master_key_provider.add_master_key(arn_template.format(
            +            region="us-west-2",
            +            account_id=account_id,
            +            alias=alias_west
            +        ))
            +        return kms_master_key_provider


