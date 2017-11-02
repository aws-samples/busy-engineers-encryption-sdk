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

package example;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class HTTPResponse {
    private static final Logger LOGGER = Logger.getLogger(HTTPResponse.class);

    public static final String CONTENT_TYPE = "Content-Type";
    @SuppressWarnings("unused") // needed in the serialized json
    public boolean isBase64Encoded = true;
    public int statusCode = 500;
    public Map<String, String> headers = new HashMap<>();
    public String body;

    public static HTTPResponse jsonError(final String errorMessage) {
        Map<String, String> result = new HashMap<>();
        result.put("status", "error");
        result.put("error", errorMessage);

        return jsonResponse(200, result);
    }

    public static HTTPResponse badRequest() {
        HTTPResponse response = new HTTPResponse();
        response.statusCode = 400;
        response.setBody("Bad request");
        return response;
    }

    public void setBody(String bodyText) {
        setBody(bodyText.getBytes(StandardCharsets.UTF_8));
    }

    public void setBody(byte[] bodyBytes) {
        body = Base64.getEncoder().encodeToString(bodyBytes);
    }

    public static HTTPResponse jsonResponse(int statusCode, final Object node) {
        HTTPResponse response = new HTTPResponse();
        response.statusCode = statusCode;

        try {
            response.setBody(Utils.MAPPER.writeValueAsString(node));
        } catch (JsonProcessingException e) {
            response.statusCode = 500;
            response.headers.put(CONTENT_TYPE, "application/json");
            response.setBody("Internal error");
            LOGGER.error("Failed to serialize error response: " + e, e);
        }
        return response;
    }
}
