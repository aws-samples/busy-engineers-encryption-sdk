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
"""Helper class to handle encryption.

This is the only module that you need to modify in the Busy Engineer's Guide to the Encryption SDK workshop.
"""
import base64
import json
import time
import boto3

import aws_encryption_sdk


class EncryptDecrypt(object):
    """Encrypt and decrypt data."""

    def __init__(self, key_id):
        """Set up materials manager and static values."""
        self._message_type = "message_type"
        self._type_order_inquiry = "order inquiry"
        self._timestamp = "rough timestamp"
        self._order_id = "order ID"
        self.master_key_provider = self.construct_multiregion_kms_master_key_provider(key_id)

    def encrypt(self, data):
        """Encrypt data.

        :param data: JSON-encodeable data to encrypt
        :returns: Base64-encoded, encrypted data
        :rtype: str
        """
        encryption_context = {self._message_type: self._type_order_inquiry}
        if order_id:
            encryption_context[self._order_id] = order_id
        ciphertext, _header = aws_encryption_sdk.encrypt(
            source=json.dumps(data), key_provider=self.master_key_provider, encryption_context=encryption_context
        )
        return base64.b64encode(ciphertext).decode("utf-8")

    def decrypt(self, data):
        """Decrypt data.

        :param bytes data: Base64-encoded, encrypted data
        :returns: JSON-decoded, decrypted data
        """
        ciphertext = base64.b64decode(data)
        plaintext, header = aws_encryption_sdk.decrypt(source=ciphertext, key_provider=self.master_key_provider)

        try:
            if header.encryption_context[self._message_type] != self._type_order_inquiry:
                raise KeyError()  # overloading KeyError to use the same exit whether wrong or missing
        except KeyError:
            raise ValueError("Bad message type in decrypted message")

        return json.loads(plaintext)

    def construct_multiregion_kms_master_key_provider(self, key_id_east):
        alias_west = 'alias/busy-engineers-workshop-python-key-us-west-2-finalCheckPlz'
        arn_template = 'arn:aws:kms:{region}:{account_id}:{alias}'

        kms_master_key_provider = aws_encryption_sdk.KMSMasterKeyProvider()
        account_id = boto3.client('sts').get_caller_identity()['Account']

        kms_master_key_provider.add_master_key(key_id_east)
        kms_master_key_provider.add_master_key(arn_template.format(
            region="us-west-2",
            account_id=account_id,
            alias=alias_west
        ))
        return kms_master_key_provider
