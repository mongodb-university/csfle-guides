# clone the test kmip server
git clone https://github.com/mongodb-labs/drivers-evergreen-tools.git

# key configuration from evergreen
openssl pkcs12 -CAfile drivers-evergreen-tools/.evergreen/x509gen/ca.pem -export -in drivers-evergreen-tools/.evergreen/x509gen/client.pem -out client.pkc -password pass:${KEYSTORE_PASSWORD}
cp ${JAVA_HOME}/lib/security/cacerts mongo-truststore
${JAVA_HOME}/bin/keytool -importcert -trustcacerts -file drivers-evergreen-tools/.evergreen/x509gen/ca.pem -keystore mongo-truststore -storepass ${KEYSTORE_PASSWORD} -storetype JKS -noprompt

# specify password in maven config file
mkdir .mvn
cp maven.config.tmpl .mvn/maven.config
sed -i '' -e "s/REPLACE-WITH-KEYSTORE-PASSWORD/$KEYSTORE_PASSWORD/g" .mvn/maven.config
sed -i '' -e "s/REPLACE-WITH-TRUSTSTORE-PASSWORD/$TRUSTSTORE_PASSWORD/g" .mvn/maven.config

# start the kmip server
echo "Starting the KMIP Server..."
cd drivers-evergreen-tools/.evergreen/csfle
. ./activate_venv.sh
./kmstlsvenv/bin/python3 -u kms_kmip_server.py
