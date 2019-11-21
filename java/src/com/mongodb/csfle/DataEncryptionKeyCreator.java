package com.mongodb.csfle;

import java.io.IOException;

/**
 * Client-side Field Level Encryption - Data Encryption Key Creator
 * Attempts to read master key from file "master-key.txt" in root directory of project
 * Attempts to locate existing local encryption key from encryption.__keyVault collection
 * 
 * 
 */
public class DataEncryptionKeyCreator {
	
	public static void main(String[] args) throws IOException {
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
			// No key found; create index on key vault and a new encryption key
			CSFLEHelpers.createKeyVaultIndex(connectionString, keyDb, keyColl);
			encryptionKey = CSFLEHelpers.createDataEncryptionKey(connectionString, kmsProvider, masterKey, keyVaultCollection, keyAltName);
			System.out.println("Congratulations on creating a new data encryption key! Copy the key below and paste it into <file>\n" + encryptionKey);
		} else {
			System.out.println("Retrieved your existing key. Copy the key below and paste it into InsertDataWithEncryptedFields\n" + encryptionKey);
		}
	}
}
