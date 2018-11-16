# Copyright 2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License"). You
# may not use this file except in compliance with the License. A copy of
# the License is located at
#
# http://aws.amazon.com/apache2.0/
#
# or in the "license" file accompanying this file. This file is
# distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF
# ANY KIND, either express or implied. See the License for the specific
# language governing permissions and limitations under the License.
"""Helper functions for returning consistent JSON responses."""
import base64
import json


def bad_request():
    """Return value for a bad request."""
    return {"isBase64Encoded": False, "statusCode": 400, "headers": {}, "body": "Bad request"}


def json_response(status_code, data):
    """Return value for a successful request.

    :param int status_code: HTTP status code to include in response
    :param data: JSON-encodeable data to include in response as body
    """
    header = {"Content-Type": "application/json"}
    try:
        body = base64.b64encode(json.dumps(data).encode("utf-8")).decode("utf-8")
    except TypeError:
        body = "Internal error"
        status_code = 500
    return {"isBase64Encoded": True, "statusCode": status_code, "headers": header, "body": body}


def json_error(message):
    """Return value for a failed request.

    :param str message: Error message to include in response
    """
    return json_response(200, {"status": "error", "error": message})
