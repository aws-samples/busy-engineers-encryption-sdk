So far we've been working with the SDK's KMS client directly. This has a few
limitations, as we'll see.

# Before we start

We'll assume that you've completed the code changes in [exercise
1](1-kms_encryption.md) first. If you haven't, you can use this git command to
catch up:

    git checkout -f -B exercise-2 origin/exercise-2-start

This will give you a codebase that already has the base64 changes applied.
Note that any uncommitted changes you've made already will be lost.

# Exploring the limitations of direct KMS

Directly using KMS means that your messages will be limited in size to 4096
bytes. Try this out for yourself - copy and paste this block into the message
field, and see how KMS rejects the message:

> The history of war teems with occasions where the interception of
> dispatches and orders written in plain language has resulted in
> defeat and disaster for the force whose intentions thus became known
> at once to the enemy. For this reason, prudent generals have used
> cipher and code messages from time immemorial. The necessity for
> exact expression of ideas practically excludes the use of codes for
> military work although it is possible that a special tactical code
> might be useful for preparation of tactical orders.
> 
> It is necessary therefore to fall back on ciphers for general military
> work if secrecy of communication is to be fairly well assured. It
> may as well be stated here that no practicable military cipher is
> mathematically indecipherable if intercepted; the most that can be
> expected is to delay for a longer or shorter time the deciphering of
> the message by the interceptor.
> 
> The capture of messengers is no longer the only means available to
> the enemy for gaining information as to the plans of a commander. All
> radio messages sent out can be copied at hostile stations within radio
> range. If the enemy can get a fine wire within one hundred feet of a
> buzzer line or within thirty feet of a telegraph line, the message can
> be copied by induction. Messages passing over commercial telegraph
> lines, and even over military lines, can be copied by spies in the
> offices. On telegraph lines of a permanent nature it is possible to
> install high speed automatic sending and receiving machines and thus
> prevent surreptitious copying of messages, but nothing but a secure
> cipher will serve with other means of communication.
> 
> It is not alone the body of the message which should be in cipher. It
> is equally important that, during transmission, the preamble, place
> from, date, address and signature be enciphered; but this should
> be done by the sending operator and these parts must, of course,
> be deciphered by the receiving operator before delivery. A special
> operators' cipher should be used for this purpose but it is difficult
> to prescribe one that would be simple enough for the average operator,
> fast and yet reasonably safe. Some form of rotary cipher machine
> would seem to be best suited for this special purpose.
> 
> It is unnecessary to point out that a cipher which can be deciphered
> by the enemy in a few hours is worse than useless. It requires a
> surprisingly long time to encipher and decipher a message, using even
> the simplest kind of cipher, and errors in transmission of cipher
> matter by wire or radio are unfortunately too common.
> 
> Kerckhoffs has stated that a military cipher should fulfill the
> following requirements:
> 
> 
>     1st. The system should be materially, if not mathematically,
>          indecipherable.
>     2d.  It should cause no inconvenience if the apparatus and methods
>          fall into the hands of the enemy.
>     3d.  The key should be such that it could be communicated and
>          remembered without the necessity of written notes and should
>          be changeable at the will of the correspondents.
>     4th. The system should be applicable to telegraphic correspondence.
>     5th. The apparatus should be easily carried and a single person
>          should be able to operate it.
>     6th. Finally, in view of the circumstances under which it must
>          be used, the system should be an easy one to operate,
>          demanding neither mental strain nor knowledge of a long series
>          of rules.
> 
> 
> A brief consideration of these six conditions must lead to the
> conclusion that there is no perfect military cipher. The first
> requirement is the one most often overlooked by those prescribing
> the use of any given cipher and, even if not overlooked, the
> indecipherability of any cipher likely to be used for military purposes
> is usually vastly overestimated by those prescribing the use of it.
> 
> If this were not true, there would have been neither material for,
> nor purpose in, the preparation of these notes. Of the hundreds of
> actual cipher messages examined by the writer, at least nine-tenths
> have been solved by the methods to be set forth. These messages were
> prepared by the methods in use by the United States Army, the various
> Mexican armies and their secret agents, and by other methods in common
> use. The usual failure has been with very short messages. Foreign
> works consulted lead to the belief that many European powers have
> used, for military purposes, cipher methods which vary from an
> extreme simplicity to a complexity which is more apparent than
> real. What effect recent events have had on this matter remains to
> be seen. It is enough that the cipher experts of practically every
> European country have appealed to the military authorities of their
> respective countries time and again to do away with these useless
> ciphers and to adopt something which offers more security, even at
> the expense of other considerations.
> 
> The cipher of the amateur, or of the non-expert who makes one up
> for some special purpose, is almost sure to fall into one of the
> classes whose solution is an easy matter. The human mind works along
> the same lines, in spite of an attempt at originality on the part of
> the individual, and this is particularly true of cipher work because
> there are so few sources of information available. In other words,
> the average man, when he sits down to evolve a cipher, has nothing
> to improve upon; he invents and there is no one to tell him that his
> invention is, in principle, hundreds of years old. The ciphers of the
> Abbé Tritheme, 1499, are the basis of most of the modern substitution
> ciphers.
> 
> In view of these facts, no message should be considered
> indecipherable. Very short messages are often very difficult and may
> easily be entirely beyond the possibility of analysis and solution,
> but it is surprising what can be done, at times, with a message of
> only a few words.
> 
> In the event of active operations, cipher experts will be in demand
> at once. Like all other experts, the cipher expert is not born or
> made in a day; and it is only constant work with ciphers, combined
> with a thorough knowledge of their underlying principles, that will
> make one worthy of the name.
> 
> Hitt, Parker. (1916) MANUAL FOR THE SOLUTION OF MILITARY CIPHERS.
> Retrieved from http://www.gutenberg.org/ebooks/48871


You may also have noticed that using the KMS client directly requires
a fair amount of boilerplate - in particular, all those byte buffer
conversions. It's also difficult to put any kind of dynamic data in
the encryption context, as you need to find a separate place to store
those context values. We'll resolve all of these by converting things
to use the encryption SDK instead.

# Overview of exercise

In this exercise we'll:

1. Implement encryption using the AWS Encryption SDK
2. Set up a dynamic encryption context

## Step by step

First, let's make sure the encryption SDK is set up as a dependency correctly.
Open up `webapp/pom.xml` and add this block in the `<dependencies>` section:

        <dependency>
            <groupId>com.amazonaws</groupId>
            <artifactId>aws-encryption-sdk-java</artifactId>
            <version>1.3.1</version>
        </dependency>

Now, let's add some imports:

    import java.util.Objects;
    import com.amazonaws.encryptionsdk.AwsCrypto;
    import com.amazonaws.encryptionsdk.CryptoResult;
    import com.amazonaws.encryptionsdk.kms.KmsMasterKey;
    import com.amazonaws.encryptionsdk.kms.KmsMasterKeyProvider;

The first step to using the Encryption SDK is setting up a master key (or
master key provider) to decide which keys will be used for the encryption.
Once we set up our master key, we won't need to keep around the `keyId` field,
so go ahead and replace it with:

    private final KmsMasterKey masterKey;

In our constructor, we'll create the master key like so:

    this.masterKey = new KmsMasterKeyProvider(keyId)
        .getMasterKey(keyId);

The actual encryption process is much simpler than with KMS. We'll keep the
context hashmap mostly the same, and the body of encrypt can just be:

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

For decrypt, we no longer need to construct an encryption context, because the
Encryption SDK records the original context for us. However, this means we now
need to check that the context is consistent with what we expected.
Decrypt therefore ends up looking like:

    public JsonNode decrypt(String ciphertext) throws IOException {
        byte[] ciphertextBytes = Base64.getDecoder().decode(ciphertext);

        CryptoResult<byte[], ?> result = new AwsCrypto().decryptData(masterKey, ciphertextBytes);

        // Check that we have the correct type
        if (!Objects.equals(result.getEncryptionContext().get(K_MESSAGE_TYPE), TYPE_ORDER_INQUIRY)) {
            throw new IllegalArgumentException("Bad message type in decrypted message");
        }

        return MAPPER.readTree(result.getResult());
    }

At this point you should be able to deploy and test the application. Try
entering the very large message from the start of this exercise; it should work
now.

_Note: If you input a message larger than about 90k you'll still run into
message size limits related to our use of SQS as well. If handling very large
messages was needed for your application, you might want to consider putting
the message in S3, and sending a reference to it via SQS._

# Adding additional audit metadata to your encryption context

Now that you're using the encryption SDK, it's a lot easier to put
dynamically-changing data in the encryption context. For example, we can record
the order ID just by doing:

    context.put("order ID", formValues.orderid);

No changes are needed in decrypt (however, it's good practice to check at least
that the key exists now). If you add this, send some messages, and then check
your cloudtrail logs after 10 minutes, you'll see the encryption context values
flowing through.

One caveat to note is that encryption context values can't be empty strings; to
deal with this, you can either use special values to indicate empty/null
fields, or only add the key if the field has a meaningful value (or require
that the field be present).
