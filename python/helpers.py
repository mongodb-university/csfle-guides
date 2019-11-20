import base64

from pymongo import MongoClient
from pymongo.encryption_options import AutoEncryptionOpts
from pymongo.encryption import ClientEncryption
from bson.codec_options import CodecOptions
from bson.binary import Binary, STANDARD, UUID
from bson import json_util


def read_master_key(path="./master-key.txt"):
    return Binary(open(path, "rb").read(96))


def create_json_schema(data_key=None):
    if data_key is None:
        raise ValueError("data_key must be provided")

    patient_schema = """
    {{
        "medicalRecords.patients": {{
            "bsonType": "object",
            "encryptMetadata": {{
                "keyId": [
                    {{
                        "$binary": {{
                            "base64": "{data_key}",
                            "subType": "04"
                        }}
                    }}
                ]
            }},
            "properties": {{
                "insurance": {{
                    "bsonType": "object",
                    "properties": {{
                        "policyNumber": {{
                            "encrypt": {{
                                "bsonType": "int",
                                "algorithm": "AEAD_AES_256_CBC_HMAC_SHA_512-Deterministic"
                            }}
                        }}
                    }}
                }},
                "medicalRecords": {{
                    "encrypt": {{
                        "bsonType": "array",
                        "algorithm": "AEAD_AES_256_CBC_HMAC_SHA_512-Random"
                    }}
                }},
                "bloodType": {{
                    "encrypt": {{
                        "bsonType": "string",
                        "algorithm": "AEAD_AES_256_CBC_HMAC_SHA_512-Random"
                    }}
                }},
                "ssn": {{
                    "encrypt": {{
                        "bsonType": "int",
                        "algorithm": "AEAD_AES_256_CBC_HMAC_SHA_512-Deterministic"
                    }}
                }}
            }}
        }}
    }}
    """.format(data_key=data_key)

    return json_util.loads(patient_schema)


class ClientBuilder:
    def __init__(self,
                 kms_providers=None,
                 key_alt_name="demo-data-key",
                 key_db="encryption",
                 key_coll="__keyVault",
                 schema=None,
                 connection_string="mongodb://localhost:27017",
                 mongocryptd_bypass_spawn=False,
                 mongocryptd_spawn_path="mongocryptd"):

        super().__init__()
        if kms_providers is None:
            raise ValueError("kms_provider is required")
        self.kms_providers = kms_providers
        self.key_alt_name = key_alt_name
        self.key_db = key_db
        self.key_coll = key_coll
        self.key_vault_namespace = f"{key_db}.{key_coll}"
        self.schema = schema
        self.connection_string = connection_string
        self.mongocryptd_bypass_spawn = mongocryptd_bypass_spawn
        self.mongocryptd_spawn_path = mongocryptd_spawn_path

        fle_opts = AutoEncryptionOpts(
            self.kms_providers,
            self.key_vault_namespace,
            mongocryptd_bypass_spawn=self.mongocryptd_bypass_spawn,
            mongocryptd_spawn_path=self.mongocryptd_spawn_path)

        self.encrypted_client = MongoClient(
            self.connection_string, auto_encryption_opts=fle_opts)

        self.regular_client = MongoClient(self.connection_string)

        self.key_vault = self.encrypted_client.get_database(
            self.key_db).get_collection(self.key_coll)

        # clients are required to create a unique sparse index on keyAltNames
        self.key_vault.create_index("keyAltNames",
                                    unique=True,
                                    partialFilterExpression={
                                        "keyAltNames": {
                                            "$exists": True
                                        }
                                    })

    def find_or_create_data_key(self):
        """
        In the guide, we create the data key and then show that it is created by
        using a find_one query. Here, in implementation, we only create the key if
        it doesn't already exist, ensuring we only have one local data key.

        We also use the key_alt_names field and provide a key alt name to aid in
        finding the key in the clients.py script.
        """

        data_key = self.key_vault.find_one(
            {"keyAltNames": {"$in": ["demo-data-key"]}})

        if data_key is None:
            with ClientEncryption(self.kms_providers,
                                  self.key_vault_namespace,
                                  self.encrypted_client,
                                  CodecOptions(uuid_representation=STANDARD)
                                  ) as client_encryption:

                data_key = client_encryption.create_data_key(
                    "local", key_alt_names=["demo-data-key"])
                uuid_data_key_id = UUID(bytes=data_key)

        else:
            uuid_data_key_id = data_key["_id"]

        base_64_data_key_id = base64.b64encode(
            uuid_data_key_id.bytes).decode("utf-8")

        print("Base64 data key Id, paste this into clients.py: ",
              base_64_data_key_id)

        return uuid_data_key_id

    def get_regular_client(self):
        return self.regular_client

    def get_csfle_enabled_client(self, schema=None):
        if schema is None:
            raise ValueError("schema is required")
        fle_opts = AutoEncryptionOpts(
            self.kms_providers,
            self.key_vault_namespace,
            mongocryptd_bypass_spawn=self.mongocryptd_bypass_spawn,
            mongocryptd_spawn_path=self.mongocryptd_spawn_path,
            schema_map=schema)
        self.encrypted_client.close()
        self.encrypted_client = MongoClient(
            self.connection_string, auto_encryption_opts=fle_opts)
        return self.encrypted_client

    def __del__(self):
        self.regular_client.close()
        self.encrypted_client.close()
