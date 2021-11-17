# clone the test kmip server
git clone https://github.com/mongodb-labs/drivers-evergreen-tools.git
# generate a pkcs12 file from the ca.pem file in evergreen tools
openssl pkcs12 -CAfile drivers-evergreen-tools/.evergreen/x509gen/ca.pem -export -in drivers-evergreen-tools/.evergreen/x509gen/client.pem -out client.pkc -password pass:${KEYSTORE_PASSWORD}
cp ${JAVA_HOME}/lib/security/cacerts mongo-truststore
${JAVA_HOME}/bin/keytool -importcert -trustcacerts -file drivers-evergreen-tools/.evergreen/x509gen/ca.pem -keystore mongo-truststore -storepass ${KEYSTORE_PASSWORD} -storetype JKS -noprompt


echo "Starting the KMIP Server..."
trap "deactivate" INT
cd drivers-evergreen-tools/.evergreen/csfle
. ./activate_venv.sh
./kmstlsvenv/bin/python3 -u kms_kmip_server.py
