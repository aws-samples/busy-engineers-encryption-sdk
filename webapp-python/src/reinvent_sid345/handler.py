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
"""Main Lambda handler for SID345."""
import base64
import json
import logging
import os
import traceback
from datetime import datetime, timezone
from itertools import zip_longest

import boto3

from reinvent_sid345.encrypt_decrypt import EncryptDecrypt
from reinvent_sid345.responses import bad_request, json_error, json_response

SQS_QUEUE_VAR = "queue_url"
KMS_CMK_VAR = "kms_key_id"
MIN_ROUNDS = 10
MAX_MESSAGE_BATCH_SIZE = 50
_LOGGER = logging.getLogger()
_LOGGER.setLevel(logging.DEBUG)
logging.basicConfig(level=logging.DEBUG)
_is_setup = False


def _setup():
    """Create resources once on Lambda cold start."""
    global _sqs_queue
    queue = os.environ.get(SQS_QUEUE_VAR)
    sqs = boto3.resource("sqs")
    _sqs_queue = sqs.Queue(queue)

    global _encrypt_decrypt
    key_id = os.environ.get(KMS_CMK_VAR)
    _encrypt_decrypt = EncryptDecrypt(key_id)

    global _is_setup
    _is_setup = True


def lambda_handler(event, context):  # pylint: disable=unused-argument
    """Lambda entry point."""
    _LOGGER.debug("REQUEST:")
    _LOGGER.debug(event)
    if not _is_setup:
        _setup()

    if event["httpMethod"] != "POST":
        _LOGGER.debug("Bad request method: {}".format(event["httpMethod"]))
        return bad_request()

    response = handle_post(event)
    _LOGGER.debug("RESPONSE:")
    _LOGGER.debug(response)
    return response


def send(request):
    """Handle send requests."""
    data = request["data"]
    ciphertext = _encrypt_decrypt.encrypt(data)
    _sqs_queue.send_message(MessageBody=ciphertext)
    return json_response(200, {"status": "ok", "kmsCallCount": 0})


def receive(request):  # pylint: disable=unused-argument
    """Handle receive requests."""
    raw_messages = []
    for _round in range(MIN_ROUNDS):
        messages = _sqs_queue.receive_messages(
            AttributeNames=("SentTimestamp",), MaxNumberOfMessages=10, VisibilityTimeout=10
        )
        if not messages and raw_messages or len(raw_messages) > MAX_MESSAGE_BATCH_SIZE:
            break

        for message in messages:
            # Delete them once we have them?
            raw_messages.append(message)

    parsed_messages = [_parse_message(message) for message in raw_messages]
    response = {"status": "ok", "messages": parsed_messages, "kmsCallCount": 0}
    return json_response(200, response)


def _parse_message(message):
    """Parse and decrypt a SQS message into the expected JSON structure."""
    _LOGGER.debug("MESSAGE:")
    _LOGGER.debug(message)
    _LOGGER.debug("MESSAGE ATTRIBUTES:")
    _LOGGER.debug(message.attributes)
    info = {
        "timestamp": _iso_format_datetime_from_sent_timestamp(message.attributes["SentTimestamp"]),
        "messageID": message.message_id,
        "ciphertext": "\n".join(_data_to_lines(message.body)),
        "decryptInfo": "",
    }
    try:
        info["plaintext"] = json.dumps(_encrypt_decrypt.decrypt(message.body), indent=4, sort_keys=True)
        info["decryptInfo"] += "KMS calls: {}".format(0)
    except Exception:  # pylint: disable=broad-except
        info["decryptError"] = traceback.format_exc()
    message.delete()
    return info


def _iso_format_datetime_from_sent_timestamp(sent_timestamp):
    """Format a SentTimestamp value from SQS as a ISO8601 datetime."""
    epoch_seconds = int(sent_timestamp) / 1000.0
    return datetime.utcfromtimestamp(epoch_seconds).astimezone(timezone.utc).isoformat()


def _data_to_lines(data):
    """Convert a string of data into 80-character-long lines."""
    return map("".join, zip_longest(*[iter(data)] * 80, fillvalue=""))


_ACTIONS = {"send": send, "recv": receive}


def handle_post(event):
    """Process a POST request."""
    request_body = event["body"]
    if event["isBase64Encoded"]:
        request_body = base64.b64decode(request_body)

    try:
        json_body = json.loads(request_body)
    except Exception:  # pylint: disable=broad-except
        message = "Unable to parse JSON input"
        _LOGGER.debug(message)
        return json_error(message)

    action = json_body["action"]
    _LOGGER.debug('Discovered action: "%s"', action)

    try:
        handler = _ACTIONS[action]
    except KeyError:
        message = "Unknown action: '{}'".format(action)
        _LOGGER.debug(message)
        return json_error(message)

    try:
        return handler(json_body)
    except Exception as error:  # pylint: disable=broad-except
        message = "Internal error: {cls}({args})".format(
            cls=error.__class__.__name__, args=", ".join(repr(arg) for arg in error.args)
        )
        _LOGGER.exception(message)
        return json_error(message)
