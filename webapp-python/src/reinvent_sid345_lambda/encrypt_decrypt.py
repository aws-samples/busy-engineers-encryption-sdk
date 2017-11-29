""""""
import base64
import json
import time

import aws_encryption_sdk

class EncryptDecrypt(object):
    """"""

    def __init__(self, key_id):
        """"""
        self._message_type = 'message_type'
        self._type_order_inquiry = 'order inquiry'
        self._timestamp = 'rough timestamp'
        master_key_provider = aws_encryption_sdk.KMSMasterKeyProvider(key_ids=[key_id])
        cache = aws_encryption_sdk.LocalCryptoMaterialsCache(capacity=100)
        self.materials_manager = aws_encryption_sdk.CachingCryptoMaterialsManager(
            cache=cache,
            master_key_provider=master_key_provider,
            max_age=5.0 * 60.0,
            max_messages_encrypted=10
        )

    def encrypt(self, data):
        """"""
        encryption_context = {
            self._message_type: self._type_order_inquiry,
            self._timestamp: str(int(time.time() / 3600.0))
        }
        ciphertext, _header = aws_encryption_sdk.encrypt(
            source=json.dumps(data),
            materials_manager=self.materials_manager,
            encryption_context=encryption_context
        )
        return base64.b64encode(ciphertext).decode('utf-8')

    def decrypt(self, data):
        """"""
        ciphertext = base64.b64decode(data)
        plaintext, header = aws_encryption_sdk.decrypt(
            source=ciphertext,
            materials_manager=self.materials_manager
        )

        try:
            if header.encryption_context[self._message_type] != self._type_order_inquiry:
                raise KeyError()  # overloading KeyError to use the same exit whether wrong or missing
        except KeyError:
            raise ValueError('Bad message type in decrypted message')

        return json.loads(plaintext)
