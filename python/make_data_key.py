from helpers import read_master_key, ClientBuilder


def main():

    local_master_key = read_master_key()

    kms_providers = {
        "local": {
            "key": local_master_key,
        },
    }

    encrypted_client = ClientBuilder(kms_providers=kms_providers)
    data_key = encrypted_client.find_or_create_data_key()

    print("Base64 data key. Copy and paste this into clients.py\t", data_key)




if __name__ == "__main__":
    main()
