from helpers import read_master_key, ClientBuilder, create_json_schema

"""
This file is meant to be run to demonstrate CSFLE in action. Prior to running
this file, ensure you've run make-local-data-key.py
"""


def main():

    local_master_key = read_master_key()

    kms_providers = {
        "local": {
            "key": local_master_key,
        },
    }

    client_prototype = ClientBuilder(kms_providers=kms_providers)

    # make sure you paste in your base64 key that was generated from
    # make_data_key.py
    data_key = None  # replace with your base64 data key!

    schema = create_json_schema(data_key=data_key)
    csfle_enabled_client = client_prototype.get_csfle_enabled_client(schema)
    regular_client = client_prototype.get_regular_client()

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
        {"ssn": example_document["ssn"]},
        {"$set": example_document}, upsert=True)

    # performing a read using the csfle enabled client. We expect all fields to
    # be readable

    # querying on an encrypted field using strict equality
    csfle_find_result = csfle_client_patients_coll.find_one(
        {"ssn": example_document["ssn"]})

    print("Document retreived with csfle enabled client:\n",
          csfle_find_result, "\n")

    # performing a read using the regular client
    # we can't perform a query on an encrypted field, and the document will be
    # returned to us with encrypted fields unreadable
    regular_find_result = regular_client_patients_coll.find_one(
        {"name": "Jon Doe"})

    print("Document found regular_find_result:\n", regular_find_result)


if __name__ == "__main__":
    main()
