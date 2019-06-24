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

package example;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import org.apache.log4j.Logger;

import example.handlers.AJAXHandler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Guice;
import com.google.inject.Injector;

@Singleton
public class Handler implements RequestHandler<HTTPRequest, HTTPResponse> {
    private static final Logger LOGGER = Logger.getLogger(Handler.class);

    @Inject
    public Handler(final Map<String, AJAXHandler> ajaxOperations) {
        this.ajaxOperations = ajaxOperations;
    }

    public static Handler boot(Context invocationContext) {
        Injector injector = Guice.createInjector(new AppModule());

        return injector.getInstance(Handler.class);
    }

    private final Map<String, AJAXHandler> ajaxOperations;

    public HTTPResponse handleRequest(final HTTPRequest request, final Context invocationContext) {
        KMSRequestCountingLogAppender.resetCount();

        if (!request.httpMethod.equals("POST")) {
            return HTTPResponse.badRequest();
        }

        return handlePost(request);
    }

    private HTTPResponse handlePost(final HTTPRequest request) {
        JsonNode json;

        try {
            byte[] requestBody;

            if (request.isBase64Encoded) {
                requestBody = Base64.getDecoder().decode(request.body);
            } else {
                requestBody = request.body.getBytes(StandardCharsets.UTF_8);
            }

            json = Utils.MAPPER.readValue(requestBody, JsonNode.class);
        } catch (IOException e) {
            LOGGER.error("Bad JSON", e);

            String errorMessage = "Unable to parse JSON input";

            return HTTPResponse.jsonError(errorMessage);
        }

        String action = json.get("action").asText("unset");

        AJAXHandler handler = ajaxOperations.get(action);

        if (handler == null) {
            return HTTPResponse.jsonError("Unknown action '" + action + "'");
        }

        try {
            return handler.handle(json);
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));

            LOGGER.error("Handler failed: " + e, e);
            return HTTPResponse.jsonError("Internal error: " + sw.toString());
        }
    }
}
