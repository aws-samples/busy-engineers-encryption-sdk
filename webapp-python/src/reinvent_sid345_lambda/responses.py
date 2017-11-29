""""""
import base64
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
        body = base64.b64encode(json.dumps(data).encode('utf-8')).decode('utf-8')
    except TypeError:
        body = 'Internal error'
        status_code = 500
    return {
        'isBase64Encoded': True,
        'statusCode': status_code,
        'headers': header,
        'body': body
    }


def json_error(message):
    """"""
    return json_response(200, {'status': 'error', 'error': message})
