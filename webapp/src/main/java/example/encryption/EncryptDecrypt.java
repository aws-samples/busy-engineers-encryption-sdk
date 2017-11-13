/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance with
 * the License. A copy of the License is located at
 *
 *     http://aws.amazon.com/apache2.0/
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */

package example.encryption;

import static example.Utils.MAPPER;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.amazonaws.encryptionsdk.AwsCrypto;
import com.amazonaws.encryptionsdk.CryptoResult;
import com.amazonaws.encryptionsdk.kms.KmsMasterKey;
import com.amazonaws.encryptionsdk.kms.KmsMasterKeyProvider;
import com.amazonaws.services.kms.AWSKMS;
import com.amazonaws.services.kms.AWSKMSClient;
import com.amazonaws.services.kms.model.DecryptRequest;
import com.amazonaws.services.kms.model.DecryptResult;
import com.amazonaws.services.kms.model.EncryptRequest;
import com.amazonaws.services.kms.model.EncryptResult;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * This class centralizes the logic for encryption and decryption of messages, to allow for easier modification.
 *
 * The guice wiring will ensure that this is a singleton; any fields initialized in the constructor will be retained
 * for subsequent invocations and/or messages.
 */
@Singleton
public class EncryptDecrypt {
    private static final Logger LOGGER = Logger.getLogger(EncryptDecrypt.class);
    private static final String K_MESSAGE_TYPE = "message type";
    private static final String TYPE_ORDER_INQUIRY = "order inquiry";
    private static final String K_ORDER_ID = "order ID";

    private final AWSKMS kms;
    private final KmsMasterKey masterKey;

    @SuppressWarnings("unused") // all fields are used via JSON deserialization
    private static class FormData {
        public String name;
        public String email;
        public String orderid;
        public String issue;
    }

    @Inject
    public EncryptDecrypt(@Named("keyId") final String keyId) {
        kms = AWSKMSClient.builder().build();
        this.masterKey = new KmsMasterKeyProvider(keyId)
            .getMasterKey(keyId);
    }

    public String encrypt(JsonNode data) throws IOException {
        FormData formValues = MAPPER.treeToValue(data, FormData.class);

        // We can access specific form fields using values in the parsed FormData object.
        LOGGER.info("Got form submission for order " + formValues.orderid);

        byte[] plaintext = MAPPER.writeValueAsBytes(formValues);

        HashMap<String, String> context = new HashMap<>();
        context.put(K_MESSAGE_TYPE, TYPE_ORDER_INQUIRY);
        if (formValues.orderid != null && formValues.orderid.length() > 0) {
            context.put(K_ORDER_ID, formValues.orderid);
        }

        byte[] ciphertext = new AwsCrypto().encryptData(masterKey, plaintext, context).getResult();

        return Base64.getEncoder().encodeToString(ciphertext);
    }

    public JsonNode decrypt(String ciphertext) throws IOException {
        byte[] ciphertextBytes = Base64.getDecoder().decode(ciphertext);

        CryptoResult<byte[], ?> result = new AwsCrypto().decryptData(masterKey, ciphertextBytes);

        // Check that we have the correct type
        if (!Objects.equals(result.getEncryptionContext().get(K_MESSAGE_TYPE), TYPE_ORDER_INQUIRY)) {
            throw new IllegalArgumentException("Bad message type in decrypted message");
        }

        return MAPPER.readTree(result.getResult());
    }
}
