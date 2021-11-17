package com.mongodb.csfle;
/*
 * Copyright 2008-present MongoDB, Inc.

 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */


import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

import com.mongodb.csfle.util.CSFLEHelpers;;import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

/*
 * - Reads master key from file "master-key.txt" in root directory of project, or creates one on a KMS
 * - Locates existing local encryption key from encryption.__keyVault collection, or from a KMS
 * - Prints base 64-encoded value of the data encryption key
 */
public class DataEncryptionKeyCreator {

    public static void main(String[] args) throws Exception {
        String connectionString = "mongodb://localhost:27017";
        String keyDb = "encryption";
        String keyColl = "__keyVault";
        String keyVaultCollection = String.join(".", keyDb, keyColl);
        String keyAltName = "altname";

        Map<String, Object> masterKeyProperties = new HashMap<>();
        Map<String, Map<String, Object>> kmsProviderProperties = new HashMap<>();

        /*
         * KMIP KMS
         */
         String kmsProvider = "kmip";
         masterKeyProperties.put("provider", "kmip");
         masterKeyProperties.put("keyId", "1");

         Map<String, Object> providerDetails = new HashMap<>();
         providerDetails.put("endpoint", "localhost:5698");
         Map<String, SSLContext> SSLContext = new HashMap<>();
         String path = "/Library/Java/JavaVirtualMachines/adoptopenjdk-11.jdk/Contents/Home/lib/security/cacerts";
         SSLContext.put("kmip", sslContext(path, "testpw"));
         kmsProviderProperties.put(kmsProvider,  providerDetails);

        // Ensure index exists on key vault
        CSFLEHelpers.createKeyVaultIndex(connectionString, keyDb, keyColl);

        // Find or insert a new data data key
        String encryptionKey = CSFLEHelpers.findDataEncryptionKey(connectionString, keyAltName, keyDb, keyColl);

        if (encryptionKey != null) {
            // Print the key
            System.out.println("Retrieved your existing key. Copy the key below and paste it into InsertDataWithEncryptedFields.java\n" + encryptionKey);
            System.exit(0);
        }

        encryptionKey = CSFLEHelpers.createDataEncryptionKey(connectionString, masterKeyProperties, kmsProviderProperties, keyVaultCollection, SSLContext,keyAltName);

        System.out.println("Congratulations on creating a new data encryption key! Copy the key below and paste it into InsertDataWithEncryptedfields.java\n" + encryptionKey);
    }

    private static SSLContext sslContext(String keystoreFile, String password)
            throws GeneralSecurityException, IOException {
        KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
        try (InputStream in = new FileInputStream(keystoreFile)) {
            keystore.load(in, password.toCharArray());
        }
        KeyManagerFactory keyManagerFactory =
                KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keystore, password.toCharArray());

        TrustManagerFactory trustManagerFactory =
                TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(keystore);

        SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
        sslContext.init(
                keyManagerFactory.getKeyManagers(),
                trustManagerFactory.getTrustManagers(),
                new SecureRandom());

        return sslContext;
    }

}
