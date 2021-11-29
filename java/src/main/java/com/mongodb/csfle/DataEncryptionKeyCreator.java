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

import java.util.HashMap;
import java.util.Map;

import com.mongodb.csfle.util.CSFLEHelpers;;

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
        String keyAltName = "demo-data-key";

        Map<String, Object> masterKeyProperties = new HashMap<>();
        Map<String, Map<String, Object>> kmsProviderProperties = new HashMap<>();


        /* START: Local master key block */
        String kmsProvider = "local";

        masterKeyProperties.put("provider", kmsProvider);
        byte[] masterKey = CSFLEHelpers.readMasterKey("./master-key.txt");
        masterKeyProperties.put("key", masterKey);

        Map<String, Object> localMasterKey = new HashMap<>();
        localMasterKey.put("key",  masterKey);
        kmsProviderProperties.put(kmsProvider, localMasterKey);
        /* END: Local master key block */

        /*
         * AWS KMS
         * Uncomment this block to use your AWS KMS provider key
         String kmsProvider = "aws";
         masterKeyProperties.put("provider", kmsProvider);
         masterKeyProperties.put("key", "<Master Key ARN>");
         masterKeyProperties.put("region", "<Master Key AWS Region>");
         masterKeyProperties.put("endpoint", "<AWS Custom Endpoint Host>"); // optional
         Map<String, Object> providerDetails = new HashMap<>();
         providerDetails.put("accessKeyId", "<IAM User Access Key ID>");
         providerDetails.put("secretAccessKey","<IAM User Secret Access Key>");
         kmsProviders.put(kmsProvider,  providerDetails);
        */

        /*
         * Azure KMS
         * Uncomment this block to use your Azure KMS provider key
         String kmsProvider = "azure";
         masterKeyProperties.put("provider", kmsProvider);
         masterKeyProperties.put("keyName": "<Azure key name>");
         masterKeyProperties.put("keyVersion": "<Azure key version>");
         masterKeyProperties.put("keyVaultEndpoint": "<Azure key vault endpoint");
         Map<String, Object> providerDetails = new HashMap<>();
         providerDetails.put("tenantId", "<Azure account organization>");
         providerDetails.put("clientId", "<Azure client ID>");
         providerDetails.put("clientSecret", "<Azure client secret>");
         providerDetails.put("identityPlatformEndpoint", "<Azure custom endpoint host>"); // optional
         kmsProviders.put(kmsProvider, providerDetails);

        /*
         * KMIP KMS
         * Uncomment this block to use your KMIP KMS provider key
         String kmsProvider = "kmip";
         masterKeyProperties.put("provider", "kmip");
         masterKeyProperties.put("keyId", "1");

         Map<String, Object> providerDetails = new HashMap<>();
         providerDetails.put("endpoint", "localhost:5698");
         kmsProviderProperties.put(kmsProvider,  providerDetails);
         */

        // Ensure index exists on key vault
        CSFLEHelpers.createKeyVaultIndex(connectionString, keyDb, keyColl);

        // Find or insert a new data data key
        String encryptionKey = CSFLEHelpers.findDataEncryptionKey(connectionString, keyAltName, keyDb, keyColl);

        if (encryptionKey != null) {
            // Print the key
            System.out.println("Retrieved your existing key. Copy the key below and paste it into InsertDataWithEncryptedFields.java\n" + encryptionKey);
            System.exit(0);
        }

        encryptionKey = CSFLEHelpers.createDataEncryptionKey(connectionString, masterKeyProperties, kmsProviderProperties, keyVaultCollection, keyAltName);

        System.out.println("Congratulations on creating a new data encryption key! Copy the key below and paste it into InsertDataWithEncryptedfields.java\n" + encryptionKey);
    }
}
