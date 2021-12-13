#! /usr/bin/env python3
"""
KMS KMIP test server.
"""

from kmip.services.server import KmipServer
import os
import logging
import argparse

HOSTNAME = "localhost"
PORT = 5698
KMIP_SERVER = "kmip_server"


def main():
    dir_path = os.path.dirname(os.path.realpath(__file__))
    kmip_dir = os.path.join(dir_path, "..")
    default_ca_file = os.path.join(
        kmip_dir, "certs", "ca.pem")
    default_cert_file = os.path.join(
        kmip_dir, "certs", "server.pem")

    parser = argparse.ArgumentParser(
        description='MongoDB Mock KMIP KMS Endpoint.')
    parser.add_argument('-p', '--port', type=int,
                        default=PORT, help="Port to listen on")
    parser.add_argument('--ca_file', type=str,
                        default=default_ca_file, help="TLS CA PEM file")
    parser.add_argument('--cert_file', type=str,
                        default=default_cert_file, help="TLS Server PEM file")
    args = parser.parse_args()

    server = KmipServer(
        hostname=HOSTNAME,
        port=args.port,
        certificate_path=args.cert_file,
        ca_path=args.ca_file,
        config_path=None,
        auth_suite="TLS1.2",
        log_path=os.path.join(kmip_dir,
                              KMIP_SERVER, ".logs", "pykmip.log"),
        database_path=os.path.join(
            kmip_dir, KMIP_SERVER, "pykmip.db"),
        logging_level=logging.DEBUG,
    )
    with server:
        print(f"Starting KMS KMIP server on port {args.port}")
        server.serve()


if __name__ == "__main__":
    main()
