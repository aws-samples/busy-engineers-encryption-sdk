In our previous exercise, we updated the application to use base64 encoding in
preparation for actually encrypting data. Next, in this exercise we'll actually
encrypt by using KMS directly. We'll also see some of the limitations that
arise when using KMS directly as well.

# Before we start

We'll assume that you've completed the code changes in [exercise
0](0-explore.md) first. If you haven't, you can use this git command to catch
up:

    git checkout -f -B exercise-1 origin/exercise-1-start

This will give you a codebase that already has the base64 changes applied.
Note that any uncommitted changes you've made already will be lost.

If you haven't done exercise 0 at all, we encourage you to go through the
preparation and deployment steps in there at a minimum.

# Using KMS directly

In this exercise we'll use direct KMS `Encrypt` and `Decrypt` calls to encrypt
and decrypt data. We'll also set an appropriate encryption context, and observe
some of the subtle pitfalls in using KMS directly.

## The KMS SDK API

The maven project is already configured to import the KMS SDK. To construct a
client we can just create an instance of it:

    AWSKMS kms = AWSKMSClient.builder().build();

Since we're running in AWS Lambda, the region and credentials are automatically
configured for us.

Actually making requests is a bit more involved. We need to construct
`EncryptRequest` or `DecryptRequest` objects with the data to encrypt or
decrypt. One major pitfall with these is that they use `java.nio.ByteBuffer`s to
transfer binary data. The APIs we looked at in the previous example used byte
arrays (`byte[]`s) instead, so we'll need to see how to convert between the two.

Converting from `byte[]` to `ByteBuffer` is easy:

    byte[] myArray = ...;
    ByteBuffer myBuffer = ByteBuffer.wrap(myArray);

Converting from `ByteBuffer` to `byte[]` is a bit more complicated:

    ByteBuffer myBuffer = ...;

    byte[] myArray = new byte[myBuffer.remaining()];
    myBuffer.get(myArray);

Note that invoking `get` changes the state of the `ByteBuffer`; if you do this
twice on the same buffer, you'll get an empty array as the second result.

The KMS Client API uses `ByteBuffer`s for all plaintext and ciphertext inputs
and outputs, so you'll need to be comfortable converting between the two.

# Actually encrypting using KMS

Now let's try actually using KMS to encrypt and decrypt. If you'd like to try
putting it together on your own, you can refer to the [KMS SDK API
documentation](http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/kms/AWSKMSClient.html)
and skip to 'Adding encryption context' once you have it working; otherwise
we'll have specific directions below.

## Step by step

First, we'll want to set up a KMS client. It's good practice to construct the
client once and use that same instance throughout the life of your program, so
we'll do that here.

We'll also need to save the key ID we want to encrypt with. The sample code already
passes that key ID into the `EncryptDecrypt` class constructor, so we'll just save
it in a field for later reference.

We'll add to the top of our class a field definition for the client and key ID.

    private static final Logger LOGGER = Logger.getLogger(EncryptDecrypt.class);
    private final AWSKMS kms; // <-- add this line
    private final String keyId; // <-- this one too

Then, we'll initialize it in the constructor:

    @Inject
    public EncryptDecrypt(@Named("keyId") final String keyId) {
        kms = AWSKMSClient.builder().build();
        this.keyId = keyId;
    }

In `encrypt()`, we'll then build and issue the request:

        EncryptRequest request = new EncryptRequest();
        request.setKeyId(keyId);
        request.setPlaintext(ByteBuffer.wrap(plaintext));

        EncryptResult result = kms.encrypt(request);

We'll then need to convert the resulting ciphertext to a byte array before base64ing it:

        // Convert to byte array
        byte[] ciphertext = new byte[result.getCiphertextBlob().remaining()];
        result.getCiphertextBlob().get(ciphertext);

        return Base64.getEncoder().encodeToString(ciphertext);

At this point encryption should be working. What's left is decryption, which works very similarly:

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

Note that we don't need to provide keyId to decrypt; decrypt will automatically
determine which key to use based on the ciphertext.

# Using the encryption context

When encrypting with KMS it's good practice to set an encryption context. This
helps ensure that your code doesn't decrypt data intended for a different
purpose, and also helps improve your audit logging.

One of the difficulties around encryption contexts with KMS is that it's
necessary to store the context independently from the encrypted data, as it must
be presented when decrypting as well. Here we'll just put a type tag on the 
encryption context, but if you're feeling ambitious we encourage you to try encoding
the order ID field in the encryption context as well.

In a later example we'll show you how the AWS Encryption SDK makes it easy to put
richer information in the encryption context as well.

## Step by step

Adding an encryption context that just has a type field is fairly simple.
First, we'll define some constants at the top of the class:

    private static final String K_MESSAGE_TYPE = "message type";
    private static final String TYPE_ORDER_INQUIRY = "order inquiry";

Since the strings used in the encryption context must match _exactly_ between
encrypt and decrypt, it's good practice to define them through shared constants
to reduce the risk of typos.

We can then just add some code to set the context on encrypt, just before the
actual encrypt call:

    HashMap<String, String> context = new HashMap<>();
    context.put(K_MESSAGE_TYPE, TYPE_ORDER_INQUIRY);
    request.setEncryptionContext(context);

The same code also needs to be placed right before the decrypt call as well.

Once you've deployed this code and sent and received data with it, about 10
minutes later the cloudtrail logs should show entries with the new encryption
context fields.

# Extra credit

Feeling ambitious? Try encoding the order ID into the encryption context as
well. The tricky part about this is that the order ID must be known at decrypt
time - so you'll need to find a way to encode it into the message outside of
the ciphertext.

If you encode the order ID into the context, you'll see it flowing through to
your cloudtrail logs as well - so you'll know which inquires are being
decrypted.
