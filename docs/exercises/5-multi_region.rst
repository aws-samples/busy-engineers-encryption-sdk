
.. _Exercise 5:

******************************************
Exercise 5: Multiple Master Key Encryption
******************************************

So far we have been working with the AWS Encryption SDK using a single Customer Master Key (CMK) to perform
encryption and decryption. We will now be exploring the multi region CMK capability of the AWS Encryption SDK.

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
            import boto3

:ref:`master-keys` are used by the AWS Encryption SDK
to protect your data. The first step to using the Encryption SDK is setting up
a Master Key or Master Key Provider. Once we set up our Master Key Provider,
we won't need to keep around the key ID, so we can discard that value.

.. tabs::

    .. group-tab:: Java

        First, we will need to write some code to create a master key provider containing multiple
        CMKs. We will create a single master key provider to which all the CMKs are added. Note that
        the first master key added to the master key provider is the one used to generate the new data
        key and the other master keys are used to encrypt the new data key. We will use MultipleProviderFactory
        to combine all the master keys into a single master key provider.

        .. code-block:: java
           :lineno-start: 56

            private final MasterKeyProvider<?> masterKeyProvider;

        In our constructor, we'll create the Master Key like so:

        .. code-block:: java
           :lineno-start: 69

            this.masterKey = new KmsMasterKeyProvider(keyId)
                .getMasterKey(keyId);

    .. group-tab:: Python

        First, we will need to write some code to create a master key provider containing multiple
        CMKs. We will create a single ``KMSMasterKeyProvider`` to which all the CMKs are added. Note that
        the first master key added to the ``KMSMasterKeyProvider`` is the one used to generate the new data
        key and the other master keys are used to encrypt the new data key.

        .. code-block:: python
           :lineno-start: 66

            def construct_multiregion_kms_master_key_provider(self):
                regions = ('us-east-2', 'us-west-2')
                alias = 'alias/busy-engineers-workshop-python-key'
                arn_template = 'arn:aws:kms:{region}:{account_id}:{alias}'

                kms_master_key_provider = aws_encryption_sdk.KMSMasterKeyProvider()
                account_id = boto3.client('sts').get_caller_identity()['Account']
                for region in regions:
                    kms_master_key_provider.add_master_key(arn_template.format(
                    region=region,
                    account_id=account_id,
                    alias='{}-{}'.format(alias, region)
                ))
                return kms_master_key_provider

.. tabs::

    .. group-tab:: Java

        We won't need the class attribute for ``keyID``, so replace that with ``masterKeyProvider``
        for the KMS Master Key Provider.

        .. code-block:: java
           :lineno-start: 56

            private final MasterKeyProvider<?> masterKeyProvider;

        In our constructor, we'll create the Master Key like so:

        .. code-block:: java
           :lineno-start: 69

            this.masterKeyProvider = getMasterKeyProvider()

    .. group-tab:: Python

        We won't need to keep the key ID around, so replace that in ``__init__`` with a call to the new
        multiple master key provider constructor.

        .. code-block:: python
           :lineno-start: 32

            self.master_key_provider = self.construct_multiregion_kms_master_key_provider()


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

                byte[] ciphertext = new AwsCrypto().encryptData(masterKeyProvider, plaintext, context).getResult();

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

                CryptoResult<byte[], ?> result = new AwsCrypto().decryptData(masterKeyProvider, ciphertextBytes);

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

Now we need to modify the handler file to pass in the appropriate arguments when constructing the EncryptDecrypt
object.

.. tabs::

    .. group-tab:: Java

        .. code-block:: java
           :lineno-start: 92

            tbd

    .. group-tab:: Python

        The file we are modifying is /src/busy-engineers-workshop/handler.py Start out by commenting out
        references to KMS_CMK_VAR since our EncryptDecrypt class will collect the kms key ids when constructing
        the master key providers. Next, remove any parameters from being passed into the EncryptDecrypt() object.

        .. code-block:: python
           :lineno-start: 50

            SQS_QUEUE_VAR = "queue_url"
            # KMS_CMK_VAR = "kms_key_id"
            MIN_ROUNDS = 10
            MAX_MESSAGE_BATCH_SIZE = 50
            _LOGGER = logging.getLogger()
            _LOGGER.setLevel(logging.DEBUG)
            _LOG_WATCHER = KmsLogListener()
            logging.basicConfig(level=logging.DEBUG)
            _is_setup = False


            def _setup():
                """Create resources once on Lambda cold start."""
                global _sqs_queue
                queue = os.environ.get(SQS_QUEUE_VAR)
                sqs = boto3.resource("sqs")
                _sqs_queue = sqs.Queue(queue)

                global _encrypt_decrypt
                # key_id = os.environ.get(KMS_CMK_VAR)
                _encrypt_decrypt = EncryptDecrypt()


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

In this portion of the exercise, we will be adding a grant to the local key that checks if the encryption context
equals {'key use':'bad key'}. Upon local key retrieval, this grant will block this key from being accessed because
the encryption context supplied with the ciphertext will not match this constraint. Therefore, preventing the local
key from being accessed.

.. tabs::

    .. group-tab:: Java

        We have built a simple java program that sets the grant, thereby disabling the use of the local key. Compile
        and run the program on the cloud9 CLI as below.

        .. code-block:: java
           :lineno-start: bash

            javac assign_grants.java
            java assign_grants.java

    .. group-tab:: Python

        We have built a simple python script that sets the grant, thereby disabling the use of the local key. Run
        the script on the cloud9 CLI as below.

        .. code-block:: bash

            python assign_grants.py

Now go ahead and send some new encrypted data to the SQS queue in the web interface. Then visit the backend logs
in cloudwatch to see that the ciphertext was encrypted using the key from another region. Afterwards, go ahead
and retrieve the data. Taking a look at the backend logs in cloudwatch, you will see that the key from another
region is used to decrypt the data as well.

Once you are done validating, go ahead and revoke the grant to see the application return back to using the local
key for encryption/decryption.

.. tabs::

    .. group-tab:: Java

        Uncomment the call to revoke the grant. You can get the grant_id from the program output to the CLI and
        directly supply that as a string parameter.

        .. code-block:: java
           :lineno-start:

            TBD

        Then compile and re-run the script.

        .. code-block:: bash

            javac assign_grants.java
            java assign_grants.java

    .. group-tab:: Python

        Comment out the calls to assign the grant. Then, uncomment the call to revoke the grant. You can get the
        grant_id from the program output to the CLI and directly supply that as a string parameter.

        .. code-block:: python
           :lineno-start: 68

            # add encryption context to us-east-2 key to prevent usage
            # encryption_context = {'key_use': 'bad key'}
            # grant_id_east = create_grant(kms_east, key_east_id, iam_arn, encryption_context)
            # print('created grant for us-east-2 key ({}) to prevent usage'.format(key_east_id))
            # print('stopped usage for us-east-2 key...')
            # print('Now retest sending a message and visit the logs to see that us-west-2 key is used
            # 'instead of the us-east-2 key to encrypt/decrypt the data!')

            '''
             To Revoke a Grant: You can grab grant_id_east from the console output and supply it as a string directly.
             Uncomment the code below to revoke the grant and you will see the application move back to using the key in
             us-east-2 in the logs
            '''

             revoke_grant(kms_east, key_east_id, grant_id_east)

        Then re-run the script.

        .. code-block:: bash

            python assign_grants.py

You can now go back to the cloudwatch logs and see the application return to using the local key for encryption
and decryption.

Another good place to see the multi region CMK use in effect is to visit the cloudtrail events for KMS. Here you
will be able to see all each request that comes to KMS. You can use the debugging tips to help narrow done your
results.

.. _ex4-change:

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
            index b7e8e07..b1cef27 100644
            --- a/src/busy_engineers_workshop/encrypt_decrypt.py
            +++ b/src/busy_engineers_workshop/encrypt_decrypt.py
            @@ -17,7 +17,7 @@ This is the only module that you need to modify in the Busy Engineer's Guide to
             import base64
             import json

            -import boto3
            +import aws_encryption_sdk


             class EncryptDecrypt(object):
            @@ -28,8 +28,8 @@ class EncryptDecrypt(object):
                     self._message_type = "message_type"
                     self._type_order_inquiry = "order inquiry"
                     self._timestamp = "rough timestamp"
            -        self.key_id = key_id
            -        self.kms = boto3.client("kms")
            +        self._order_id = "order ID"
            +        self.master_key_provider = aws_encryption_sdk.KMSMasterKeyProvider(key_ids=[key_id])

                 def encrypt(self, data):
                     """Encrypt data.
            @@ -39,9 +39,12 @@ class EncryptDecrypt(object):
                     :rtype: str
                         """
                         encryption_context = {self._message_type: self._type_order_inquiry}
                -        plaintext = json.dumps(data).encode("utf-8")
                -        response = self.kms.encrypt(KeyId=self.key_id, Plaintext=plaintext, EncryptionContext=encryption_context)
                -        ciphertext = response["CiphertextBlob"]
                +        order_id = data.get("orderid", "")
                +        if order_id:
                +            encryption_context[self._order_id] = order_id
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

