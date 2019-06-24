# Copyright 2017-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import json


class EncryptDecrypt(object):
    """Encrypt and decrypt data."""

    def __init__(self, key_id_east, key_id_west):
        """Set up materials manager and static values."""
        self._message_type = "message_type"
        self._type_order_inquiry = "order inquiry"
        self._timestamp = "rough timestamp"
        self.key_id = key_id_east

    def encrypt(self, data):
        """Encrypt data.

        :param data: JSON-encodeable data to encrypt
        :returns: Base64-encoded, encrypted data
        :rtype: str
        """
        return json.dumps(data)

    def decrypt(self, data):
        """Decrypt data.

        :param bytes data: Base64-encoded, encrypted data
        :returns: JSON-decoded, decrypted data
        """
        return json.loads(data)
