#
# Copyright 2019-present MongoDB, Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

"""Helpers for CSFLE implementation."""

import base64

from pymongo import MongoClient
from pymongo.encryption_options import AutoEncryptionOpts
from pymongo.encryption import ClientEncryption
from bson.codec_options import CodecOptions
from bson.binary import Binary, STANDARD, UUID


def read_master_key(path="./master-key.txt"):
    return Binary(open(path, "rb").read(96))


class CsfleHelper:
    """This is a helper class that aids in csfle implementation."""

    def __init__(self,

                 kms_providers=None,
                 key_alt_name="demo-data-key",
                 key_db="encryption",
                 key_coll="__keyVault",
                 schema=None,
                 connection_string="mongodb://localhost:27017",
                 mongocryptd_bypass_spawn=False,
                 mongocryptd_spawn_path="mongocryptd"):
        """If mongocryptd
        is not installed to in your search path, ensure you override
        mongocryptd_spawn_path
        """
        super().__init__()
        if kms_providers is None:
            raise ValueError("kms_provider is required")
        self.kms_providers = kms_providers
        self.key_alt_name = key_alt_name
        self.key_db = key_db
        self.key_coll = key_coll
        self.key_vault_namespace = f"{self.key_db}.{self.key_coll}"
        self.schema = schema
        self.connection_string = connection_string
        self.mongocryptd_bypass_spawn = mongocryptd_bypass_spawn
        self.mongocryptd_spawn_path = mongocryptd_spawn_path

    def ensure_unique_index_on_key_vault(self, key_vault):

        # clients are required to create a unique partial index on keyAltNames
        key_vault.create_index("keyAltNames",
                               unique=True,
                               partialFilterExpression={
                                   "keyAltNames": {
                                       "$exists": True
                                   }
                               })

    def find_or_create_data_key(self):
        """
        In the guide, https://docs.mongodb.com/ecosystem/use-cases/client-side-field-level-encryption-guide/,
        we create the data key and then show that it is created by
        using a find_one query. Here, in implementation, we only create the key if
        it doesn't already exist, ensuring we only have one local data key.

        We also use the key_alt_names field and provide a key alt name to aid in
        finding the key in the clients.py script.
        """

        key_vault_client = MongoClient(
            self.connection_string, auto_encryption_opts=AutoEncryptionOpts(
                self.kms_providers,
                self.key_vault_namespace,
                mongocryptd_bypass_spawn=self.mongocryptd_bypass_spawn,
                mongocryptd_spawn_path=self.mongocryptd_spawn_path))

        key_vault = (key_vault_client
                     .get_database(self.key_db)
                     .get_collection(self.key_coll))

        self.ensure_unique_index_on_key_vault(key_vault)

        data_key = key_vault.find_one(
            {"keyAltNames": self.key_alt_name}
        )

        if data_key is None:
            with ClientEncryption(self.kms_providers,
                                  self.key_vault_namespace,
                                  key_vault_client,
                                  CodecOptions(uuid_representation=STANDARD)
                                  ) as client_encryption:

                data_key = client_encryption.create_data_key(
                    "local", key_alt_names=[self.key_alt_name])
                uuid_data_key_id = UUID(bytes=data_key)

        else:
            uuid_data_key_id = data_key["_id"]

        base_64_data_key_id = (base64
                               .b64encode(uuid_data_key_id.bytes)
                               .decode("utf-8"))

        return base_64_data_key_id

    def get_regular_client(self):
        return MongoClient(self.connection_string)

    def get_csfle_enabled_client(self, schema=None):
        if schema is None:
            raise ValueError("schema is required")

        return MongoClient(
            self.connection_string,
            auto_encryption_opts=AutoEncryptionOpts(
                self.kms_providers,
                self.key_vault_namespace,
                mongocryptd_bypass_spawn=self.mongocryptd_bypass_spawn,
                mongocryptd_spawn_path=self.mongocryptd_spawn_path,
                schema_map=schema)
        )

    def create_json_schema(self, data_key=None):
        if data_key is None:
            raise ValueError("data_key must be provided")

        patient_schema = {
            "medicalRecords.patients": {
                "bsonType": "object",
                "encryptMetadata": {
                    "keyId": [Binary(base64.b64decode(data_key), 4)]
                },
                "properties": {
                    "insurance": {
                        "bsonType": "object",
                        "properties": {
                            "policyNumber": {
                                "encrypt": {
                                    "bsonType": "int",
                                    "algorithm": "AEAD_AES_256_CBC_HMAC_SHA_512-Deterministic"
                                }
                            }
                        }
                    },
                    "medicalRecords": {
                        "encrypt": {
                            "bsonType": "array",
                            "algorithm": "AEAD_AES_256_CBC_HMAC_SHA_512-Random"
                        }
                    },
                    "bloodType": {
                        "encrypt": {
                            "bsonType": "string",
                            "algorithm": "AEAD_AES_256_CBC_HMAC_SHA_512-Random"
                        }
                    },
                    "ssn": {
                        "encrypt": {
                            "bsonType": "int",
                            "algorithm": "AEAD_AES_256_CBC_HMAC_SHA_512-Deterministic"
                        }
                    }
                }
            }
        }
        return patient_schema
