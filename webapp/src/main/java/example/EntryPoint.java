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

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

public class EntryPoint implements RequestHandler<HTTPRequest, HTTPResponse> {
    private Handler handler;

    @Override public HTTPResponse handleRequest(final HTTPRequest request, final Context context) {
        if (handler == null) {
            handler = Handler.boot(context);
        }

        return handler.handleRequest(request, context);
    }
}
