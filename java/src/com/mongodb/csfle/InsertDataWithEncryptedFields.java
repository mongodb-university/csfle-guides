package com.mongodb.csfle;

import static com.mongodb.client.model.Filters.eq;

import java.io.IOException;
import java.util.ArrayList;

import org.bson.Document;
import org.bson.conversions.Bson;

import com.mongodb.ConnectionString;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.UpdateOptions;

public class InsertDataWithEncryptedFields {
	
	public static void main(String[] args) throws IOException {
		String connectionString = "mongodb://localhost:27017";
		String keyDb = "encryption";
		String keyColl = "__keyVault";
		String kmsProvider = "local";
		String keyVaultCollection = String.join(".", keyDb, keyColl);
		String recordsDb = "medicalRecords";
		String recordsColl = "patients";
		
		// Read the local master key from the provided file
		byte[] masterKey = CSFLEHelpers.readMasterKey("./master-key.txt");
		
		// Construct a JSON Schema
		String keyId = "GmUw8Uy2Rn2n5pIwwUkqeQ=="; //"<paste your data encryption key here>";
		Document schema = CSFLEHelpers.createJSONSchema(keyId);
				
		// Construct an encrypted client
		MongoClient encryptedClient = 
				CSFLEHelpers.createEncryptedClient(connectionString, kmsProvider, masterKey, keyVaultCollection, schema, recordsDb, recordsColl);

		// Insert into the collection
		MongoCollection<Document> collection = encryptedClient.getDatabase(recordsDb).getCollection(recordsColl);
		
		Bson ssnQuery = eq("ssn", CSFLEHelpers.SAMPLE_SSN_VALUE);
		collection.updateOne(ssnQuery, new Document("$set", CSFLEHelpers.SAMPLE_DOC), new UpdateOptions().upsert(true));
		System.out.println("Successfully upserted the sample document!");

		// Query SSN field with encrypted client
		Document result = collection.find(ssnQuery).first();
		System.out.println("Encrypted client query by the SSN (deterministically-encrypted) field:\n" + result.toJson());
		
		
		// Query SSN field with normal client without encryption
		MongoClient normalMongoClient = MongoClients.create(new ConnectionString(connectionString));
		MongoCollection<Document> normalCollection = normalMongoClient.getDatabase(recordsDb).getCollection(recordsColl);
		
		Document normalClientResult = normalCollection.find(ssnQuery).first();
		assert(normalClientResult == null) : "Check your collection documents for unencrypted SSN fields:\n" + normalClientResult;
		
		// Query name (unencrypted) field with normal client without encryption
		Bson nameQuery = eq("name", CSFLEHelpers.SAMPLE_NAME_VALUE); 
		Document normalClientNameResult = normalCollection.find(nameQuery).first();
		assert(normalClientNameResult != null) : "Check your collection to ensure a document with the matching name field exists.";
		System.out.println("Query by name returned the following document:\n " + normalClientNameResult);
	}
}
