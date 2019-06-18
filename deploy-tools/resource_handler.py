import json
import logging
import boto3
from botocore.vendored import requests

lg = logging.getLogger()
lg.setLevel(logging.INFO)
_is_setup = False


def lambda_handler(event, context):
    try:
        lg.info('Request Received: {}'.format(event))
        role = event['ResourceProperties']['Role']
        region_name = event['ResourceProperties']['Region']
        alias = '{}'.format(event['ResourceProperties']['Alias'])
        session = boto3.session.Session(region_name=region_name)
        client = session.client('kms')

        if not _is_setup:
            lg.info('Create CMK2 in us-west-2')
            state, keyId = create(region_name, client, alias, role)

            global _is_setup
            _is_setup = True

            return send_response(event, context, 'SUCCESS')

        if event['RequestType'] == 'Create':
            lg.info('Request Type: Create')
            state, keyId = create(region_name, client, alias, role)
            return send_response(event, context, 'SUCCESS')

        if event['RequestType'] == 'Delete':
            lg.info('Request Type: Delete')
            target_key_id = get_key_id(client, alias)
            if target_key_id is None:
                return send_response(event, context, 'FAILED')
            state = delete(client, target_key_id)
            return send_response(event, context, 'SUCCESS')

        if event['RequestType'] == 'Update':
            lg.info('Request Type: Update')
            response = client.list_aliases()
            aliases = response['Aliases']
            for a in aliases:
                if a['AliasName'] == alias:
                    return send_response(event, context, 'SUCCESS')
            return send_response(event, context, 'FAILED')

    except Exception as e:
        lg.info(e)


def get_key_id(client, alias):
    response = client.list_aliases()
    aliases = response['Aliases']
    for a in aliases:
        if a['AliasName'] == alias:
            key_id = a['TargetKeyId']
            return key_id
    return None


def create(region_name, client, alias, role, **_):
    lg.info('entered create func')
    desc = 'Key for protecting critical data in {}'.format(region_name)
    policy = """
    {{
        "Version": "2012-10-17",
        "Id": "key-policy",
        "Statement": [
            {{
                "Sid": "Allow access for Key Administrators",
                "Effect": "Allow",
                "Principal":{{
                    "AWS": "{}"
                }},
                "Action": [
                    "kms:Create*",
                    "kms:Describe*",
                    "kms:Enable*",
                    "kms:List*",
                    "kms:Put*",
                    "kms:Update*",
                    "kms:Revoke*",
                    "kms:Disable*",
                    "kms:Get*",
                    "kms:Delete*",
                    "kms:TagResource",
                    "kms:UntagResource",
                    "kms:ScheduleKeyDeletion",
                    "kms:CancelKeyDeletion"
                ],
                "Resource": "*"
            }},
            {{
                "Sid": "Allow use of the key",
                "Effect": "Allow". 
                "Principal": {{
                    "AWS": "{}"
                }},
                "Action": [
                    "kms:GenerateDataKey*"
                ],
                "Resource": "*"
            }}
        ]
    }}
    """.format(role)
    response = client.create_key(
        Policy=policy,
        Description=desc,
        KeyUsage='ENCRYPT_DECRYPT',
        BypassPolicyLockoutSafetyCheck=True
    )
    key_id = response['KeyMetadata']['Arn']
    set_alias = client.create_alias(
        AliasName=alias,
        TargetKeyId=key_id
    )
    return True, key_id


def delete(client, key_id, **_):
    response = client.describe_key(
        KeyId=key_id
    )
    deletion_status = response['KeyMetadata']['KeyState']
    if deletion_status == 'Enabled':
        response = client.schedule_key_deletion(
            KeyId=key_id,
            PendingWindowInDays=7
        )
    return True


def send_response(event, context, resp_status):
    resp_body = {'Status': resp_status,
                 'Reason': 'See the details in CloudWatch Log Stream: ' + context.log_stream_name,
                 'PhysicalResourceId': context.log_stream_name,
                 'StackId': event['StackId'],
                 'RequestId': event['RequestId'],
                 'LogicalResourceId': event['LogicalResourceId']}
    lg.info('RESPONSE BODY:n' + json.dumps(resp_body))
    try:
        req = requests.put(event['ResponseURL'], data=json.dumps(resp_body))
        if req.status_code != 200:
            lg.info(req.text)
            raise Exception('Did not receive 200')
        return
    except requests.exceptions.RequestException as e:
        lg.info(e)
        raise
