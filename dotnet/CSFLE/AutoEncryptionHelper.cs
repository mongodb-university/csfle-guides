using System;
using System.Collections.Generic;
using System.IO;
using MongoDB.Bson;
using MongoDB.Driver;
using MongoDB.Driver.Encryption;

namespace CSFLE
{
    public class AutoEncryptionHelper
    {
        private static readonly string __localMasterKeyPath = "../../../master-key.txt";
        private static readonly string __sampleNameValue = "John Doe";
        private static readonly int __sampleSsnValue = 145014000;

        private static readonly BsonDocument __sampleDocFields =
            new BsonDocument
            {
                { "name", __sampleNameValue },
                { "ssn", __sampleSsnValue },
                { "bloodType", "AB-" },
                {
                    "medicalRecords",
                    new BsonArray(new []
                    {
                        new BsonDocument("weight", 180),
                        new BsonDocument("bloodPressure", "120/80")
                    })
                },
                {
                    "insurance",
                    new BsonDocument
                    {
                        { "policyNumber", 123142 },
                        { "provider", "MaestCare" }
                    }
                }
            };

        private readonly string _connectionString;
        private readonly CollectionNamespace _keyVaultNamespace;
        private readonly CollectionNamespace _medicalRecordsNamespace;

        public AutoEncryptionHelper(string connectionString, CollectionNamespace keyVaultNamespace)
        {
            _connectionString = connectionString;
            _keyVaultNamespace = keyVaultNamespace;
            _medicalRecordsNamespace = CollectionNamespace.FromFullName("medicalRecords.patients");
        }

        public void EncryptedWriteAndRead(string keyIdBase64, KmsKeyLocation kmsKeyLocation)
        {
            // Construct a JSON Schema
            var schema = JsonSchemaCreator.CreateJsonSchema(keyIdBase64);

            // Construct an auto-encrypting client
            var autoEncryptingClient = CreateAutoEncryptingClient(
                kmsKeyLocation,
                _keyVaultNamespace,
                schema);
            var collection = autoEncryptingClient
                .GetDatabase(_medicalRecordsNamespace.DatabaseNamespace.DatabaseName)
                .GetCollection<BsonDocument>(_medicalRecordsNamespace.CollectionName);

            var ssnQuery = Builders<BsonDocument>.Filter.Eq("ssn", __sampleSsnValue);

            // Insert a document into the collection
            collection.UpdateOne(ssnQuery, new BsonDocument("$set", __sampleDocFields), new UpdateOptions() { IsUpsert = true });
            Console.WriteLine("Successfully upserted the sample document!");

            // Query by SSN field with auto-encrypting client
            var result = collection.Find(ssnQuery).Single();

            Console.WriteLine($"Encrypted client query by the SSN (deterministically-encrypted) field:\n {result}\n");
        }

        public void QueryWithNonEncryptedClient()
        {
            var nonAutoEncryptingClient = new MongoClient(_connectionString);
            var collection = nonAutoEncryptingClient
              .GetDatabase(_medicalRecordsNamespace.DatabaseNamespace.DatabaseName)
              .GetCollection<BsonDocument>(_medicalRecordsNamespace.CollectionName);
            var ssnQuery = Builders<BsonDocument>.Filter.Eq("ssn", __sampleSsnValue);

            var result = collection.Find(ssnQuery).FirstOrDefault();
            if (result != null)
            {
                throw new Exception("Expected no document to be found but one was found.");
            }

            // Query by name field with a normal non-auto-encrypting client
            var nameQuery = Builders<BsonDocument>.Filter.Eq("name", __sampleNameValue);
            result = collection.Find(nameQuery).FirstOrDefault();
            if (result == null)
            {
                throw new Exception("Expected the document to be found but none was found.");
            }

            Console.WriteLine($"Query by name (non-encrypted field) using non-auto-encrypting client returned:\n {result}\n");
        }

        private IMongoClient CreateAutoEncryptingClient(
            KmsKeyLocation kmsKeyLocation,
            CollectionNamespace keyVaultNamespace,
            BsonDocument schema)
        {
            var kmsProviders = new Dictionary<string, IReadOnlyDictionary<string, object>>();

            switch (kmsKeyLocation)
            {
                case KmsKeyLocation.Local:
                    var localMasterKeyBase64 = File.ReadAllText(__localMasterKeyPath);
                    var localMasterKeyBytes = Convert.FromBase64String(localMasterKeyBase64);
                    var localOptions = new Dictionary<string, object>
                    {
                        { "key", localMasterKeyBytes }
                    };
                    kmsProviders.Add("local", localOptions);
                    break;

                case KmsKeyLocation.AWS:
                    var awsAccessKey = Environment.GetEnvironmentVariable("FLE_AWS_ACCESS_KEY");
                    var awsSecretAccessKey = Environment.GetEnvironmentVariable("FLE_AWS_SECRET_ACCESS_KEY");
                    var awsKmsOptions = new Dictionary<string, object>
                    {
                        { "accessKeyId", awsAccessKey },
                        { "secretAccessKey", awsSecretAccessKey }
                    };
                    kmsProviders.Add("aws", awsKmsOptions);
                    break;

                case KmsKeyLocation.Azure:
                    var azureTenantId = Environment.GetEnvironmentVariable("FLE_AZURE_TENANT_ID");
                    var azureClientId = Environment.GetEnvironmentVariable("FLE_AZURE_CLIENT_ID");
                    var azureClientSecret = Environment.GetEnvironmentVariable("FLE_AZURE_CLIENT_SECRET");
                    var azureIdentityPlatformEndpoint = Environment.GetEnvironmentVariable("FLE_AZURE_IDENTIFY_PLATFORM_ENPDOINT"); // Optional, only needed if user is using a non-commercial Azure instance

                    var azureKmsOptions = new Dictionary<string, object>
                    {
                        { "tenantId", azureTenantId },
                        { "clientId", azureClientId },
                        { "clientSecret", azureClientSecret },
                    };
                    if (azureIdentityPlatformEndpoint != null)
                    {
                        azureKmsOptions.Add("identityPlatformEndpoint", azureIdentityPlatformEndpoint);
                    }
                    kmsProviders.Add("azure", azureKmsOptions);
                    break;

                case KmsKeyLocation.GCP:
                    var gcpPrivateKey = Environment.GetEnvironmentVariable("FLE_GCP_PRIVATE_KEY");
                    var gcpEmail = Environment.GetEnvironmentVariable("FLE_GCP_EMAIL");
                    var gcpKmsOptions = new Dictionary<string, object>
                    {
                        { "privateKey", gcpPrivateKey },
                        { "email", gcpEmail },
                    };
                    kmsProviders.Add("gcp", gcpKmsOptions);
                    break;
            }

            var schemaMap = new Dictionary<string, BsonDocument>();
            schemaMap.Add(_medicalRecordsNamespace.ToString(), schema);

            var extraOptions = new Dictionary<string, object>()
            {
                // uncomment the following line if you are running mongocryptd manually
                // { "mongocryptdBypassSpawn", true }
            };

            var clientSettings = MongoClientSettings.FromConnectionString(_connectionString);
            var autoEncryptionOptions = new AutoEncryptionOptions(
                keyVaultNamespace: keyVaultNamespace,
                kmsProviders: kmsProviders,
                schemaMap: schemaMap,
                extraOptions: extraOptions);
            clientSettings.AutoEncryptionOptions = autoEncryptionOptions;
            return new MongoClient(clientSettings);
        }
    }
}
