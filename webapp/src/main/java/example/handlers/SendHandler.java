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

package example.handlers;

import static example.Utils.MAPPER;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.log4j.Logger;

import example.HTTPResponse;
import example.KMSRequestCountingLogAppender;
import example.encryption.EncryptDecrypt;

import com.amazonaws.services.sqs.AmazonSQS;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Singleton
public class SendHandler implements AJAXHandler {
    private static final Logger LOGGER = Logger.getLogger(SendHandler.class);

    private final String queueUrl;
    private final AmazonSQS sqs;
    private final EncryptDecrypt encryptDecrypt;

    @Inject
    public SendHandler(
            @Named("queueUrl") final String queueUrl,
            final AmazonSQS sqs,
            final EncryptDecrypt encryptDecrypt
    ) {
        this.queueUrl = queueUrl;
        this.sqs = sqs;
        this.encryptDecrypt = encryptDecrypt;
    }

    @Override public HTTPResponse handle(final JsonNode request) throws Exception {
        KMSRequestCountingLogAppender.resetCount();

        JsonNode data = request.findValue("data");

        String ciphertext = encryptDecrypt.encrypt(data);

        sqs.sendMessage(queueUrl, ciphertext);

        JsonNodeFactory nodeFactory = MAPPER.getNodeFactory();

        ObjectNode response = nodeFactory.objectNode();
        response.set("status", nodeFactory.textNode("ok"));
        response.set("kmsCallCount", nodeFactory.numberNode(KMSRequestCountingLogAppender.getCount()));

        return HTTPResponse.jsonResponse(200, response);
    }
}
