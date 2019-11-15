from pymongo import MongoClient
from pymongo.encryption_options import AutoEncryptionOpts
from bson import json_util
from bson.binary import Binary, UUID

"""
This file is meant to be run to demonstrate CSFLE in action. Prior to running
this file, ensure you've run make-local-data-key.py
"""

patient_schema = """
{
    "medicalRecords.patients": {
        "bsonType": "object",
        "encryptMetadata": {
            "keyId": [
                    {
                        "$binary":
                            {
                                "base64": "PkOoW5UqSIKtMShrnnYWNA==",
                                "subType": "04"
                            }
                    }
            ]
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
"""

connection_string = "mongodb://localhost:27017"
key_vault_namespace = "encryption.__keyVault"

path = "./master-key.txt"
local_master_key = Binary(open(path, "rb").read(96))

kms_providers = {
    "local": {
        "key": local_master_key,
    },
}

regular_client = MongoClient(
    connection_string,
)

key_vault = regular_client.get_database(
    "encryption").get_collection("__keyVault")
key = key_vault.find_one()
fle_opts = AutoEncryptionOpts(
    kms_providers,
    key_vault_namespace,
    schema_map=json_util.loads(patient_schema)
    # uncomment below if you've started mongocryptd in its own process
    # mongocryptd_bypass_spawn=True
)


csfle_enabled_client = MongoClient(
    connection_string,
    auto_encryption_opts=fle_opts
)

example_document = {
    "name": "Jon Doe",
    "ssn": 241014209,
    "bloodType": "AB+",
    "medicalRecords": [
        {
            "weight": 180,
            "bloodPressure": "120/80"
        }
    ],
    "insurance": {
        "provider": "MaestCare",
        "policyNumber": 123142
    }
}


regular_client_patients_coll = regular_client.get_database(
    "medicalRecords").get_collection("patients")

csfle_client_patients_coll = csfle_enabled_client.get_database(
    "medicalRecords").get_collection("patients")

# performing the insert operation with the csfle enabled client
# we're using an update with upsert so that subsequent runs of this script don't
# add more documents
csfle_client_patients_coll.update_one(
    {"ssn": example_document["ssn"]}, {"$set": example_document}, upsert=True)

# performing a read using the csfle enabled client. We expect all fields to
# be readable

# querying on an encrypted field using strict equality
csfle_find_result = csfle_client_patients_coll.find_one(
    {"ssn": example_document["ssn"]})

print(csfle_find_result)

# performing a read using the regular client
# we can't perform a query on an encrypted field, and the document will be
# returned to us with encrypted fields unreadable
regular_find_result = regular_client_patients_coll.find_one(
    {"name": "Jon Doe"})

print(regular_find_result)
