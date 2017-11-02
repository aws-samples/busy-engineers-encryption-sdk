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

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnySetter;

@SuppressWarnings("unused")
public class HTTPRequest {
    public String resource;
    public String path;
    public String httpMethod;
    public Map<String, String> headers;
    public String queryStringParameters;
    public Map<String, String> pathParameters;
    public Map<String, String> stageVariables;
    public RequestContext requestContext;
    public String body;
    public boolean isBase64Encoded;

    public Map<String, Object> unknownFields = new HashMap<>();

    @JsonAnySetter
    public void setUnknownField(String key, Object value) {
        unknownFields.put(key, value);
    }

    public static class RequestContext {
        public String path;
        public String accountId;
        public String resourceId;
        public String stage;
        public String requestId;
        public Map<String, Object> identity;
        public String resourcePath;
        public String httpMethod;
        public String apiId;

        public Map<String, Object> unknownFields = new HashMap<>();

        @JsonAnySetter
        public void setUnknownField(String key, Object value) {
            unknownFields.put(key, value);
        }
    }
}
