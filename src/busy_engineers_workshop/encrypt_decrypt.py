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

import boto3


class EncryptDecrypt(object):
    """Encrypt and decrypt data."""

    def __init__(self, key_id_east, key_id_west):
        """Set up materials manager and static values."""
        self._message_type = "message_type"
        self._type_order_inquiry = "order inquiry"
        self._timestamp = "rough timestamp"
        self.key_id = key_id_east
        self.kms = boto3.client("kms")

    def encrypt(self, data):
        """Encrypt data.

        :param data: JSON-encodeable data to encrypt
        :returns: Base64-encoded, encrypted data
        :rtype: str
        """
        encryption_context = {self._message_type: self._type_order_inquiry}
        plaintext = json.dumps(data).encode("utf-8")
        response = self.kms.encrypt(KeyId=self.key_id, Plaintext=plaintext, EncryptionContext=encryption_context)
        ciphertext = response["CiphertextBlob"]
        return base64.b64encode(ciphertext).decode("utf-8")

    def decrypt(self, data):
        """Decrypt data.

        :param bytes data: Base64-encoded, encrypted data
        :returns: JSON-decoded, decrypted data
        """
        ciphertext = base64.b64decode(data)
        encryption_context = {self._message_type: self._type_order_inquiry}
        response = self.kms.decrypt(CiphertextBlob=ciphertext, EncryptionContext=encryption_context)
        plaintext = response["Plaintext"]

        return json.loads(plaintext)
