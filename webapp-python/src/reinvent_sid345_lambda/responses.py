""""""
import json


def bad_request():
    """"""
    return {
        'isBase64Encoded': True,
        'statusCode': 400,
        'headers': {},
        'body': 'Bad request'
    }


def json_response(status_code, data):
    """"""
    header = {'Content-Type': 'application/json'}
    try:
        # We don't actually want to JSON-encode this here because Lambda takes care of
        # that for us, but we want to make sure it's JSON-encodable.
        json.dumps(data)
    except TypeError:
        body = 'Internal error'
        status_code = 500
    return {
        'isBase64Encoded': True,
        'statusCode': status_code,
        'headers': header,
        'body': data
    }


def json_error(message):
    """"""
    return json_response(200, {'status': 'error', 'error': message})
