using System;
using System.Collections.Generic;
using System.IO;
using System.Threading;
using MongoDB.Bson;
using MongoDB.Driver.Encryption;
using MongoDB.Driver;

namespace CSFLE
{
    public class KmsKeyHelper
    {
        private readonly static string __localMasterKeyPath = "../../../master-key.txt";

        private readonly string _connectionString;
        private readonly CollectionNamespace _keyVaultNamespace;

        public KmsKeyHelper(
            string connectionString,
            CollectionNamespace keyVaultNamespace)
        {
            _connectionString = connectionString;
            _keyVaultNamespace = keyVaultNamespace;
        }

        public void GenerateLocalMasterKey()
        {
            using (var randomNumberGenerator = System.Security.Cryptography.RandomNumberGenerator.Create())
            {
                var bytes = new byte[96];
                randomNumberGenerator.GetBytes(bytes);
                var localMasterKeyBase64 = Convert.ToBase64String(bytes);
                Console.WriteLine(localMasterKeyBase64);
                File.WriteAllText(__localMasterKeyPath, localMasterKeyBase64);
            }
        }

        public string CreateKeyWithLocalKmsProvider()
        {
            string localMasterKeyBase64 = File.ReadAllText(__localMasterKeyPath);
            var localMasterKeyBytes = Convert.FromBase64String(localMasterKeyBase64);

            var kmsProviders = new Dictionary<string, IReadOnlyDictionary<string, object>>();
            var localOptions = new Dictionary<string, object>
            {
                { "key", localMasterKeyBytes }
            };
            kmsProviders.Add("local", localOptions);

            var clientEncryption = GetClientEncryption(kmsProviders);
            var dataKeyId = clientEncryption.CreateDataKey("local", new DataKeyOptions(), CancellationToken.None);
            Console.WriteLine($"Local DataKeyId [UUID]: {dataKeyId}");
            var dataKeyIdBase64 = Convert.ToBase64String(GuidConverter.ToBytes(dataKeyId, GuidRepresentation.Standard));
            Console.WriteLine($"Local DataKeyId [base64]: {dataKeyIdBase64}");

            ValidateKey(dataKeyId);
            return dataKeyIdBase64;
        }

        public string CreateKeyWithAwsKmsProvider()
        {
            var kmsProviders = new Dictionary<string, IReadOnlyDictionary<string, object>>();

            var awsAccessKey = Environment.GetEnvironmentVariable("FLE_AWS_ACCESS_KEY");
            var awsSecretAccessKey = Environment.GetEnvironmentVariable("FLE_AWS_SECRET_ACCESS_KEY");
            var awsKmsOptions = new Dictionary<string, object>
            {
                { "accessKeyId", awsAccessKey },
                { "secretAccessKey", awsSecretAccessKey }
            };
            kmsProviders.Add("aws", awsKmsOptions);

            var clientEncryption = GetClientEncryption(kmsProviders);

            var awsKeyARN = Environment.GetEnvironmentVariable("FLE_AWS_KEY_ARN"); // e.g. "arn:aws:kms:us-east-2:111122223333:alias/test-key"
            var awsKeyRegion = Environment.GetEnvironmentVariable("FLE_AWS_KEY_REGION");
            var awsEndpoint = Environment.GetEnvironmentVariable("FLE_AWS_ENDPOINT"); // Optional, AWS KMS URL.
            var dataKeyOptions = new DataKeyOptions(
                masterKey: new BsonDocument
                {
                    { "region", awsKeyRegion },
                    { "key", awsKeyARN },
                    { "endpoint", () => awsEndpoint, awsEndpoint != null }
                });

            var dataKeyId = clientEncryption.CreateDataKey("aws", dataKeyOptions, CancellationToken.None);
            Console.WriteLine($"AWS DataKeyId [UUID]: {dataKeyId}");
            var dataKeyIdBase64 = Convert.ToBase64String(GuidConverter.ToBytes(dataKeyId, GuidRepresentation.Standard));
            Console.WriteLine($"AWS DataKeyId [base64]: {dataKeyIdBase64}");

            ValidateKey(dataKeyId);
            return dataKeyIdBase64;
        }

        public string CreateKeyWithAzureKmsProvider()
        {
            var kmsProviders = new Dictionary<string, IReadOnlyDictionary<string, object>>();

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

            var clientEncryption = GetClientEncryption(kmsProviders);
            var azureKeyName = Environment.GetEnvironmentVariable("FLE_AZURE_KEY_NAME");
            var azureKeyVaultEndpoint = Environment.GetEnvironmentVariable("FLE_AZURE_KEYVAULT_ENDPOINT"); // typically <azureKeyName>.vault.azure.net
            var azureKeyVersion = Environment.GetEnvironmentVariable("FLE_AZURE_KEY_VERSION"); // Optional
            var dataKeyOptions = new DataKeyOptions(
                masterKey: new BsonDocument
                {
                    { "keyName", azureKeyName },
                    { "keyVaultEndpoint", azureKeyVaultEndpoint },
                    { "keyVersion", () => azureKeyVersion, azureKeyVersion != null }
                });

            var dataKeyId = clientEncryption.CreateDataKey("azure", dataKeyOptions, CancellationToken.None);
            Console.WriteLine($"Azure DataKeyId [UUID]: {dataKeyId}");
            var dataKeyIdBase64 = Convert.ToBase64String(GuidConverter.ToBytes(dataKeyId, GuidRepresentation.Standard));
            Console.WriteLine($"Azure DataKeyId [base64]: {dataKeyIdBase64}");

            ValidateKey(dataKeyId);
            return dataKeyIdBase64;
        }

        public string CreateKeyWithGcpKmsProvider()
        {
            var kmsProviders = new Dictionary<string, IReadOnlyDictionary<string, object>>();

            var gcpPrivateKey = Environment.GetEnvironmentVariable("FLE_GCP_PRIVATE_KEY");
            var gcpEmail = Environment.GetEnvironmentVariable("FLE_GCP_EMAIL");
            var gcpEndpoint = Environment.GetEnvironmentVariable("FLE_GCP_IDENTITY_ENDPOINT"); // Optional, defaults to "oauth2.googleapis.com".
            var gcpKmsOptions = new Dictionary<string, object>
            {
                { "privateKey", gcpPrivateKey },
                { "email", gcpEmail },
            };
            if (gcpEndpoint != null)
            {
                gcpKmsOptions.Add("endpoint", gcpEndpoint);
            }
            kmsProviders.Add("gcp", gcpKmsOptions);

            var clientEncryption = GetClientEncryption(kmsProviders);
            var gcpDataKeyProjectId = Environment.GetEnvironmentVariable("FLE_GCP_PROJ_ID");
            var gcpDataKeyLocation = Environment.GetEnvironmentVariable("FLE_GCP_KEY_LOC"); // Optional. e.g. "global"
            var gcpDataKeyKeyRing = Environment.GetEnvironmentVariable("FLE_GCP_KEY_RING");
            var gcpDataKeyKeyName = Environment.GetEnvironmentVariable("FLE_GCP_KEY_NAME");
            var gcpDataKeyKeyVersion = Environment.GetEnvironmentVariable("FLE_GCP_KEY_VERSION"); // Optional
            var gcpDataKeyEndpoint = Environment.GetEnvironmentVariable("FLE_GCP_KMS_ENDPOINT"); // Optional, KMS URL, defaults to https://www.googleapis.com/auth/cloudkms

            var dataKeyOptions = new DataKeyOptions(
                masterKey: new BsonDocument
                {
                    { "projectId", gcpDataKeyProjectId },
                    { "location", gcpDataKeyLocation } ,
                    { "keyRing", gcpDataKeyKeyRing },
                    { "keyName", gcpDataKeyKeyName },
                    { "keyVersion", () => gcpDataKeyKeyVersion, gcpDataKeyKeyVersion != null },
                    { "endpoint", () => gcpDataKeyEndpoint, gcpDataKeyEndpoint != null }
                });

            var dataKeyId = clientEncryption.CreateDataKey("gcp", dataKeyOptions, CancellationToken.None);
            Console.WriteLine($"DataKeyId [UUID]: {dataKeyId}");
            var dataKeyIdBase64 = Convert.ToBase64String(GuidConverter.ToBytes(dataKeyId, GuidRepresentation.Standard));
            Console.WriteLine($"DataKeyId [base64]: {dataKeyIdBase64}");

            ValidateKey(dataKeyId);
            return dataKeyIdBase64;
        }

        private ClientEncryption GetClientEncryption(
            Dictionary<string, IReadOnlyDictionary<string, object>> kmsProviders)
        {
            var keyVaultClient = new MongoClient(_connectionString);
            var clientEncryptionOptions = new ClientEncryptionOptions(
                keyVaultClient: keyVaultClient,
                keyVaultNamespace: _keyVaultNamespace,
                kmsProviders: kmsProviders);

            return new ClientEncryption(clientEncryptionOptions);
        }

        private void ValidateKey(Guid dataKeyId)
        {
            // Verify that the key document was created
            var client = new MongoClient(_connectionString);
            var collection = client
                .GetDatabase(_keyVaultNamespace.DatabaseNamespace.DatabaseName)
                .GetCollection<BsonDocument>(
                    _keyVaultNamespace.CollectionName,
                    new MongoCollectionSettings
                    {
#pragma warning disable CS0618
                        GuidRepresentation = GuidRepresentation.Standard
#pragma warning restore CS0618
                    });
            var query = Builders<BsonDocument>.Filter.Eq("_id", new BsonBinaryData(dataKeyId, GuidRepresentation.Standard));
            var keyDocument = collection
                .Find(query)
                .Single();

            Console.WriteLine(keyDocument);
        }
    }
}
