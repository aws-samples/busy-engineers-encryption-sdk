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
import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;
import static java.util.Collections.singleton;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

import example.HTTPResponse;
import example.KMSRequestCountingLogAppender;
import example.encryption.EncryptDecrypt;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.DeleteMessageBatchRequest;
import com.amazonaws.services.sqs.model.DeleteMessageBatchRequestEntry;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class RecvHandler implements AJAXHandler {
    // SQS can return incomplete lists of messages occasionally, due to eventual consistency. We'll do at least this
    // many rounds of receivemessage, even if we get an empty response.
    private static final int MIN_ROUNDS = 10;
    // Maximum number of messages to fetch at a time
    public static final int FETCH_LIMIT = 50;

    private static final Logger LOGGER = Logger.getLogger(RecvHandler.class);
    public static final String ATTR_TIMESTAMP = "SentTimestamp";

    private final String queueURL;
    private final AmazonSQS sqs;
    private final EncryptDecrypt encryptDecrypt;

    private static final class JsonMessageInfo {
        public String timestamp;
        public String messageID;
        public String plaintext;
        public String ciphertext;
        public String decryptInfo;
        public String decryptError;
    }

    @Inject
    public RecvHandler(
            @Named("queueUrl") final String queueURL,
            final AmazonSQS sqs,
            final EncryptDecrypt encryptDecrypt
    ) {
        this.queueURL = queueURL;
        this.sqs = sqs;
        this.encryptDecrypt = encryptDecrypt;
    }

    @Override public HTTPResponse handle(final JsonNode request) throws Exception {
        if (!request.isObject()) {
            throw new Exception("Bad node type");
        }

        ObjectNode obj = (ObjectNode)request;

        ReceiveMessageRequest rmr = new ReceiveMessageRequest(queueURL);
        rmr.setVisibilityTimeout(10);
        rmr.setMaxNumberOfMessages(10);
        rmr.setAttributeNames(singleton(ATTR_TIMESTAMP));

        Map<String, Message> rawMessages = new HashMap<>();

        ReceiveMessageResult result;
        int round = 0;
        do {
            result = sqs.receiveMessage(rmr);

            result.getMessages().forEach(
                    message -> rawMessages.put(message.getMessageId(), message)
            );

            if (!result.getMessages().isEmpty()) {
                try {
                    sqs.deleteMessageBatch(
                            new DeleteMessageBatchRequest(
                                    queueURL,
                                    result.getMessages().stream().map(
                                            msg -> new DeleteMessageBatchRequestEntry(msg.getMessageId(),
                                                                                      msg.getReceiptHandle())
                                    )
                                    .collect(Collectors.toList())
                    ));
                } catch (Exception e) {
                    LOGGER.warn("Failed to delete messages from SQS", e);
                }
            }

            // Loop to receive more messages, as SQS might not return all messages on a single pass.
        } while ((round++ < MIN_ROUNDS || !result.getMessages().isEmpty()) && rawMessages.size() < FETCH_LIMIT);

        List<JsonMessageInfo> messages =
            rawMessages.values().stream()
                .sorted(Comparator.comparing(msg -> msg.getAttributes().get(ATTR_TIMESTAMP)))
                .map(this::ingestMessage)
                .collect(Collectors.toList());

        ObjectNode rootNode = MAPPER.createObjectNode();
        rootNode.put("status", "ok");
        rootNode.set("messages", MAPPER.valueToTree(messages));

        return HTTPResponse.jsonResponse(200, rootNode);
    }

    private JsonMessageInfo ingestMessage(final Message message) {
        JsonMessageInfo info = new JsonMessageInfo();

        info.timestamp = formatTimestamp(message.getAttributes().get(ATTR_TIMESTAMP));
        info.messageID = message.getMessageId();
        info.ciphertext = wrap(message.getBody());

        info.decryptInfo = "";
        KMSRequestCountingLogAppender.resetCount();
        try {
            info.plaintext = decrypt(message.getBody());
        } catch (Throwable t) {
            StringWriter sw = new StringWriter();
            t.printStackTrace(new PrintWriter(sw));

            info.decryptError = sw.toString();
        } finally {
            long requestCount = KMSRequestCountingLogAppender.resetCount();
            if (requestCount > 0) {
                info.decryptInfo += "KMS calls: " + requestCount;
            }
        }

        return info;
    }

    private String formatTimestamp(final String timestampString) {
        long timestamp = Long.parseLong(timestampString);

        return ISO_OFFSET_DATE_TIME.format(ZonedDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.of("UTC")));
    }

    private String decrypt(final String body) throws IOException {
        return MAPPER.writeValueAsString(encryptDecrypt.decrypt(body));
    }

    private String wrap(final String body) {
        StringBuilder builder = new StringBuilder();

        int index = 0;
        while (index < body.length()) {
            int length = Math.min(80, body.length() - index);
            String line = body.substring(index, index + length);

            builder.append(line);
            builder.append('\n');
            index += length;
        }

        return builder.toString();
    }
}
