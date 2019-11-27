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

package main.java.com.mongodb.csfle.util;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bson.BsonBinary;
import org.bson.BsonDocument;
import org.bson.Document;
import org.bson.conversions.Bson;

import com.mongodb.AutoEncryptionSettings;
import com.mongodb.ClientEncryptionSettings;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.vault.DataKeyOptions;
import com.mongodb.client.vault.ClientEncryption;
import com.mongodb.client.vault.ClientEncryptions;

/*
 * Helper methods and sample data for this companion project.
 */
public class CSFLEHelpers {

    // Sample data
    public static String SAMPLE_NAME_VALUE = "John Doe";
    public static Integer SAMPLE_SSN_VALUE = 145014000;

    public static Document SAMPLE_DOC = new Document()
            .append("name", SAMPLE_NAME_VALUE)
            .append("ssn", SAMPLE_SSN_VALUE)
            .append("bloodType", "AB-")
            .append("medicalRecords", Arrays.asList(
                    new Document().append("weight", "180"),
                    new Document().append("bloodPressure", "120/80")))
            .append("insurance", new Document()
                    .append("policyNumber", 123142)
                    .append("provider", "MaestCare"));

    // Reads the 96-byte local master key
    public static byte[] readMasterKey(String filePath) throws Exception {
        int numBytes = 96;
        byte[] fileBytes = new byte[numBytes];

        try (FileInputStream fis = new FileInputStream(filePath)) {
            if (fis.read(fileBytes) < numBytes)
                throw new Exception("Expected to read 96 bytes from file");
        }
        return fileBytes;
    }

    // JSON Schema helpers
    private static String DETERMINISTIC_ENCRYPTION_TYPE = "AEAD_AES_256_CBC_HMAC_SHA_512-Deterministic";
    private static String RANDOM_ENCRYPTION_TYPE = "AEAD_AES_256_CBC_HMAC_SHA_512-Random";

    private static Document buildEncryptedField(String bsonType, Boolean isDeterministic) {
        return new Document().
                append("encrypt", new Document()
                        .append("bsonType", bsonType)
                        .append("algorithm",
                                (isDeterministic) ? DETERMINISTIC_ENCRYPTION_TYPE : RANDOM_ENCRYPTION_TYPE));
    }

    private static Document createEncryptMetadataSchema(String keyId) {
        List<Document> keyIds = new ArrayList<>();
        keyIds.add(new Document()
                .append("$binary", new Document()
                        .append("base64", keyId)
                        .append("subType", "04")));
        return new Document().append("keyId", keyIds);
    }

    public static Document createJSONSchema(String keyId) throws IllegalArgumentException {
        if (keyId.isEmpty()) {
            throw new IllegalArgumentException("keyId must contain your base64 encryption key id.");
        }
        return new Document().append("bsonType", "object").append("encryptMetadata", createEncryptMetadataSchema(keyId))
                .append("properties", new Document()
                        .append("ssn", buildEncryptedField("int", true))
                        .append("bloodType", buildEncryptedField("string", false))
                        .append("medicalRecords", buildEncryptedField("array", false))
                        .append("insurance", new Document()
                                .append("bsonType", "object")
                                .append("properties",
                                        new Document().append("policyNumber", buildEncryptedField("int", true)))));
    }

    // Creates Normal Client
    private static MongoClient createMongoClient(String connectionString) {
        return MongoClients.create(connectionString);
    }

    // Creates KeyVault which allows you to create a key as well as encrypt and decrypt fields
    private static ClientEncryption createKeyVault(String connectionString, String kmsProvider, byte[] localMasterKey, String keyVaultCollection) {
        Map<String, Object> masterKeyMap = new HashMap<>();
        masterKeyMap.put("key", localMasterKey);
        Map<String, Map<String, Object>> kmsProviders = new HashMap<>();
        kmsProviders.put(kmsProvider, masterKeyMap);

        ClientEncryptionSettings clientEncryptionSettings = ClientEncryptionSettings.builder()
                .keyVaultMongoClientSettings(MongoClientSettings.builder()
                        .applyConnectionString(new ConnectionString(connectionString))
                        .build())
                .keyVaultNamespace(keyVaultCollection)
                .kmsProviders(kmsProviders)
                .build();

        return ClientEncryptions.create(clientEncryptionSettings);
    }

    // You may need to update the following variable to point to your mongocryptd binary
    private static String mongocryptdPath = "/usr/local/bin/mongocryptd";

    // Creates Encrypted Client which performs automatic encryption and decryption of fields
    public static MongoClient createEncryptedClient(String connectionString, String kmsProvider, byte[] masterKey, String keyVaultCollection, Document schema, String dataDb, String dataColl) {
        String recordsNamespace = dataDb + "." + dataColl;

        Map<String, BsonDocument> schemaMap = new HashMap<>();
        schemaMap.put(recordsNamespace, BsonDocument.parse(schema.toJson()));

        Map<String, Object> keyMap = new HashMap<>();
        keyMap.put("key", masterKey);

        Map<String, Map<String, Object>> kmsProviders = new HashMap<>();
        kmsProviders.put(kmsProvider, keyMap);

        Map<String, Object> extraOpts = new HashMap<>();
        extraOpts.put("mongocryptdSpawnPath", mongocryptdPath);
        // uncomment the following line if you are running mongocryptd manually
        //      extraOpts.put("mongocryptdBypassSpawn", true);

        AutoEncryptionSettings autoEncryptionSettings = AutoEncryptionSettings.builder()
                .keyVaultNamespace(keyVaultCollection)
                .kmsProviders(kmsProviders)
                .extraOptions(extraOpts)
                .schemaMap(schemaMap)
                .build();

        MongoClientSettings clientSettings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(connectionString))
                .autoEncryptionSettings(autoEncryptionSettings)
                .build();

        return MongoClients.create(clientSettings);
    }

    // Returns existing data encryption key
    public static String findDataEncryptionKey(String connectionString, String keyAltName, String keyDb, String keyColl) {
        try (MongoClient mongoClient = createMongoClient(connectionString)) {
            Document query = new Document("keyAltNames", keyAltName);
            MongoCollection<Document> collection = mongoClient.getDatabase(keyDb).getCollection(keyColl);
            BsonDocument doc = collection
                    .withDocumentClass(BsonDocument.class)
                    .find(query)
                    .first();

            if (doc != null) {
                return Base64.getEncoder().encodeToString(doc.getBinary("_id").getData());
            }
            return null;
        }
    }

    // Creates index for keyAltNames in the specified key collection
    public static void createKeyVaultIndex(String connectionString, String keyDb, String keyColl) {
        try (MongoClient mongoClient = createMongoClient(connectionString)) {
            MongoCollection<Document> collection = mongoClient.getDatabase(keyDb).getCollection(keyColl);

            Bson filterExpr = Filters.exists("keyAltNames", true);
            IndexOptions indexOptions = new IndexOptions().unique(true).partialFilterExpression(filterExpr);

            collection.createIndex(new Document("keyAltNames", 1), indexOptions);
        }
    }

    // Create data encryption key in the specified key collection
    // Call only after checking whether a data encryption key with same keyAltName exists
    public static String createDataEncryptionKey(String connectionString, String kmsProvider, byte[] localMasterKey, String keyVaultCollection, String keyAltName) {
        try (ClientEncryption keyVault = createKeyVault(connectionString, kmsProvider, localMasterKey, keyVaultCollection)) {
            BsonBinary dataKeyId = keyVault.createDataKey(kmsProvider, new DataKeyOptions().keyAltNames(Arrays.asList(keyAltName)));

            return Base64.getEncoder().encodeToString(dataKeyId.getData());
        }
    }
}
