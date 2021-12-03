#! /usr/bin/env python3
"""
Ensures a SecretData is registered with the unique identifier "1" on the KMS KMIP test server.
This is a utility, and not meant for CI testing.
"""

import kmip.pie.client
import kmip.pie.objects
import kmip.pie.exceptions
import kmip.core.enums
import os

HOSTNAME = "localhost"
PORT = 5698
UID = "1"
SECRETDATABYTES = b'\xf5\xc9\x58\x81\xf3\x27\x06\x70\x33\x71\x66\x68\x49\xf9\xfd\x0e\x08\xcd\x8d\xc9\x37\x61\x28\xfb\xde\xfe\xfd\x49\x6f\x3c\x7c\x90\xf8\xfb\x5e\x4c\x5d\x4d\x04\x75\x62\xd4\xda\xc6\x36\x09\xd3\x63\xd7\x70\x31\x7c\x0b\x39\x2e\x9d\x46\x89\xa1\x25\x72\x46\x17\x76\x2f\xf3\xf0\xc8\x81\xe8\x94\x6a\xca\x32\x0f\x70\x14\x38\x90\x33\xd2\x33\x43\xd4\x07\x65\xee\x3c\x29\x8f\x26\xa3\x33\x2e\xc0\x15'

# Regenerate a SecretData.
# The UID is chosen by the server.


def regen(client):
    secretdata = kmip.pie.objects.SecretData(
        SECRETDATABYTES, kmip.core.enums.SecretDataType.PASSWORD)
    uid = client.register(secretdata)
    print(f"Created SecretData with UID={uid}")
    client.activate(uid)
    print(f"Activated SecretData {uid}")
    return uid


def main():
    dir_path = os.path.dirname(os.path.realpath(__file__))
    drivers_evergreen_tools = os.path.join(dir_path, "..", "..")
    client = kmip.pie.client.ProxyKmipClient(
        hostname=HOSTNAME,
        port=PORT,
        cert=os.path.join(drivers_evergreen_tools,
                          ".evergreen", "x509gen", "client.pem"),
        ca=os.path.join(drivers_evergreen_tools,
                        ".evergreen", "x509gen", "ca.pem"),
        config_file=os.path.join(
            drivers_evergreen_tools, ".evergreen", "csfle", "pykmip.conf")
    )
    with client:
        try:
            secretdata = client.get(UID)
            assert type(secretdata) is kmip.pie.objects.SecretData
            assert len(secretdata.value) == 96
            assert secretdata.value == SECRETDATABYTES
            print(f"OK. Found SecretData with UID {UID}. Nothing to do.")
            return
        except kmip.pie.exceptions.KmipOperationFailure as err:
            if err.reason == kmip.core.enums.ResultReason.ITEM_NOT_FOUND:
                print(f"ERROR. SecretData object not found with UID: {UID}.")
                print("Attempting to regenerate.")
                regen(client)
            else:
                raise err


if __name__ == "__main__":
    main()
