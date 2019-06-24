/*
 * Copyright 2017-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

    @SuppressWarnings("unused") // all fields are used via JSON deserialization
    private static class FormData {
        public String name;
        public String email;
        public String orderid;
        public String issue;
    }

    @Inject
    public EncryptDecrypt(@Named("keyIdEast") final String keyIdEast, @Named("keyIdWest") final String keyIdWest) {
        // TODO - do something with keyIdEast?
    }

    public String encrypt(JsonNode data) throws IOException {
        FormData formValues = MAPPER.treeToValue(data, FormData.class);

        // We can access specific form fields using values in the parsed FormData object.
        LOGGER.info("Got form submission for order " + formValues.orderid);

        // TODO: Encryption goes here

        byte[] plaintext = MAPPER.writeValueAsBytes(formValues);

        return Base64.getEncoder().encodeToString(plaintext);
    }

    public JsonNode decrypt(String ciphertext) throws IOException {
        byte[] ciphertextBytes = Base64.getDecoder().decode(ciphertext);

        return MAPPER.readTree(ciphertextBytes);
    }
}
