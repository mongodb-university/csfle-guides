from helpers import read_master_key, ClientBuilder


def main():

    local_master_key = read_master_key()

    kms_providers = {
        "local": {
            "key": local_master_key,
        },
    }

    encrypted_client = ClientBuilder(kms_providers=kms_providers)
    encrypted_client.find_or_create_data_key()


if __name__ == "__main__":
    main()
