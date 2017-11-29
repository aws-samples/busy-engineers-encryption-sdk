""""""
import base64
from itertools import zip_longest
import json
import logging
import os
import traceback

import boto3

from reinvent_sid345_lambda.encrypt_decrypt import EncryptDecrypt
from reinvent_sid345_lambda.responses import bad_request, json_error, json_response

SQS_QUEUE_VAR = 'queue_url'
KMS_CMK_VAR = 'kms_key_id'
MIN_ROUNDS = 10
MAX_MESSAGE_BATCH_SIZE = 50
_LOGGER = logging.getLogger()
_LOGGER.setLevel(logging.DEBUG)
logging.basicConfig(level=logging.DEBUG)
_is_setup = False


def _setup():
    global _is_setup
    if _is_setup:
        return

    global _sqs_queue
    queue = os.environ.get(SQS_QUEUE_VAR)
    sqs = boto3.resource('sqs')
    _sqs_queue = sqs.Queue(queue)
    global _encrypt_decrypt
    key_id = os.environ.get(KMS_CMK_VAR)
    _encrypt_decrypt = EncryptDecrypt(key_id)
    _is_setup = True


def sid345_handler(event, context):
    """"""
    _LOGGER.debug('REQUEST:')
    _LOGGER.debug(event)
    _setup()
    if event['httpMethod']  != 'POST':
        _LOGGER.debug('Bad request method: {}'.format(event['httpMethod']))
        return bad_request()

    response = _handle_post(event)
    _LOGGER.debug('RESPONSE:')
    _LOGGER.debug(response)
    return response


def _send(request):
    """"""
    data = request['data']
    ciphertext = _encrypt_decrypt.encrypt(data)
    _sqs_queue.send_message(MessageBody=ciphertext)
    return json_response(200, {'status': 'ok', 'kmsCallCount': 0})


def _receive(request):
    """"""
    raw_messages = []
    for _round  in range(MIN_ROUNDS):
        messages = _sqs_queue.receive_messages(
            AttributeNames('SentTimestamp'),
            MaxNumberOfMessages=10,
            VisibilityTimeout=10
        )
        if not messages and raw_messages or len(raw_messages) > MAX_MESSAGE_BATCH_SIZE:
            break

        for message in messages:
            # Delete them once we have them?
            raw_messages.append(message)


    parsed_messages = [_parse_message(message) for message in raw_messages]
    response = {
        'status': 'ok',
        'messages': parsed_messages,
        'kmsCallCount': 0
    }
    return json_response(200, response)


def _parse_message(message):
    """"""
    info = {
        'timestamp': FORMAT_TIMESTAMP(message.attributes['SentTimestamp']),
        'messageID': message.message_id,
        'ciphertext': _data_to_lines(message.body),
        'decryptInfo': ''
    }
    try:
        info['plaintext'] = _encrypt_decrypt.decrypt(message.body)
    except Exception as error:
        info['decryptError'] = traceback.format_exc(error)
    return info


def _data_to_lines(data):
    """"""
    return map(
        ''.join,
        zip_longest(
            *[iter(data)] * 80,
            fillvalue=''
        )
    )

_ACTIONS = {
    'send': _send,
    'recv': _receive
}

def _handle_post(event):
    """"""
    request_body = event['body']
    if event['isBase64Encoded']:
        request_body = base64.b64decode(request_body)

    try:
        json_body = json.loads(request_body)
    except Exception:
        message = 'Unable to parse JSON input'
        _LOGGER.debug(message)
        return json_error(message)

    action = json_body['action']
    _LOGGER.debug('Discovered action: "{}"'.format(action))

    try:
        handler = _ACTIONS[action]
    except KeyError:
        message = "Unknown action: '{}'".format(action)
        _LOGGER.debug(message)
        return json_error(message)

    try:
        return handler(json_body)
    except Exception as error:
        message = 'Internal error: {cls}({args})'.format(
            cls=error.__class__.__name__,
            args=', '.join(repr(arg) for arg in error.args)
        )
        _LOGGER.exception(message)
        return json_error(message)
