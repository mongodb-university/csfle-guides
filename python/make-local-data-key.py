import base64

from pymongo import MongoClient
from pymongo.encryption_options import AutoEncryptionOpts
from pymongo.encryption import ClientEncryption
from bson.codec_options import CodecOptions
from bson.binary import Binary, STANDARD, UUID

connection_string = "mongodb://localhost:27017"
key_vault_namespace = "encryption.__keyVault"

path = "./master-key.txt"
local_master_key = Binary(open(path, "rb").read(96))

kms_providers = {
    "local": {
        "key": local_master_key,
    },
}

fle_opts = AutoEncryptionOpts(
    kms_providers,
    key_vault_namespace,
    # mongocryptd_bypass_spawn=True
)

client = MongoClient(
    connection_string,
    auto_encryption_opts=fle_opts
)

key_vault = client.get_database("encryption").get_collection("__keyVault")
data_key = key_vault.find_one()

print("DATA KEY", data_key)

if data_key is None:
    print("data key not found, creating")
    with ClientEncryption(
        kms_providers,
        key_vault_namespace,
        client,
        CodecOptions(uuid_representation=STANDARD)
    ) as client_encryption:

        data_key = client_encryption.create_data_key("local")
        print("created data key", data_key)
        uuid_data_key_id = UUID(bytes=data_key)
else:
    print("data key found, skipping creation")
    uuid_data_key_id = data_key["_id"]


base_64_data_key_id = base64.b64encode(uuid_data_key_id.bytes).decode("utf-8")
print("DataKeyId [UUID]: ", uuid_data_key_id)
print("DataKeyId [base64]: ", base_64_data_key_id)
