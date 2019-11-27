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

package main.java.com.mongodb.csfle;

import main.java.com.mongodb.csfle.util.CSFLEHelpers;

/*
 * - Reads master key from file "master-key.txt" in root directory of project
 * - Locates existing local encryption key from encryption.__keyVault collection
 */
public class DataEncryptionKeyCreator {

    public static void main(String[] args) throws Exception {
        String connectionString = "mongodb://localhost:27017";
        String keyDb = "encryption";
        String keyColl = "__keyVault";
        String keyAltName = "demo-data-key";
        String kmsProvider = "local";
        String keyVaultCollection = String.join(".", keyDb, keyColl);

        // Read the local master key from the provided file
        byte[] masterKey = CSFLEHelpers.readMasterKey("./master-key.txt");

        // Find or insert a new data data key
        String encryptionKey = CSFLEHelpers.findDataEncryptionKey(connectionString, keyAltName, keyDb, keyColl);

        if (encryptionKey == null) {
            // No key found; create index on key vault and a new encryption key and print the key
            CSFLEHelpers.createKeyVaultIndex(connectionString, keyDb, keyColl);
            encryptionKey = CSFLEHelpers.createDataEncryptionKey(connectionString, kmsProvider, masterKey, keyVaultCollection, keyAltName);
            System.out.println("Congratulations on creating a new data encryption key! Copy the key below and paste it into InsertDataWithEncryptedfields.java\n" + encryptionKey);
        } else {
            // Print the key
            System.out.println("Retrieved your existing key. Copy the key below and paste it into InsertDataWithEncryptedFields.java\n" + encryptionKey);
        }
    }
}
