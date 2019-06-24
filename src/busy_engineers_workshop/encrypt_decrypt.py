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
import base64
import json
import time

import aws_encryption_sdk


class EncryptDecrypt(object):
    """Encrypt and decrypt data."""

    def __init__(self, key_id_east, key_id_west):
        """Set up materials manager and static values."""
        self._message_type = "message_type"
        self._type_order_inquiry = "order inquiry"
        self._timestamp = "rough timestamp"
        self._order_id = "order ID"
        master_key_provider = aws_encryption_sdk.KMSMasterKeyProvider(key_ids=[key_id_east])
        cache = aws_encryption_sdk.LocalCryptoMaterialsCache(capacity=100)
        self.materials_manager = aws_encryption_sdk.CachingCryptoMaterialsManager(
            cache=cache, master_key_provider=master_key_provider, max_age=5.0 * 60.0, max_messages_encrypted=10
        )

    def encrypt(self, data):
        """Encrypt data.

        :param data: JSON-encodeable data to encrypt
        :returns: Base64-encoded, encrypted data
        :rtype: str
        """
        encryption_context = {
            self._message_type: self._type_order_inquiry,
            self._timestamp: str(int(time.time() / 3600.0)),
        }
        ciphertext, _header = aws_encryption_sdk.encrypt(
            source=json.dumps(data), materials_manager=self.materials_manager, encryption_context=encryption_context
        )
        return base64.b64encode(ciphertext).decode("utf-8")

    def decrypt(self, data):
        """Decrypt data.

        :param bytes data: Base64-encoded, encrypted data
        :returns: JSON-decoded, decrypted data
        """
        ciphertext = base64.b64decode(data)
        plaintext, header = aws_encryption_sdk.decrypt(source=ciphertext, materials_manager=self.materials_manager)

        try:
            if header.encryption_context[self._message_type] != self._type_order_inquiry:
                raise KeyError()  # overloading KeyError to use the same exit whether wrong or missing
        except KeyError:
            raise ValueError("Bad message type in decrypted message")

        return json.loads(plaintext)
