using System;
using MongoDB.Driver;

namespace CSFLE
{
    public enum KmsKeyLocation
    {
        AWS,
        Azure,
        GCP,
        Local
    }

    class Program
    {
        static void Main(string[] args)
        {
            var connectionString = "mongodb://localhost:27017";
            var keyVaultNamespace = CollectionNamespace.FromFullName("encryption.__keyVault");

            var kmsKeyHelper = new KmsKeyHelper(
                connectionString: connectionString,
                keyVaultNamespace: keyVaultNamespace);
            var autoEncryptionHelper = new AutoEncryptionHelper(
                connectionString: connectionString,
                keyVaultNamespace: keyVaultNamespace);

            string kmsKeyIdBase64;
            //Only run GenerateLocalMasterKey() once
            //kmsKeyHelper.GenerateLocalMasterKey();

            //Local
            kmsKeyIdBase64 = kmsKeyHelper.CreateKeyWithLocalKmsProvider();
            autoEncryptionHelper.EncryptedWriteAndRead(kmsKeyIdBase64, KmsKeyLocation.Local);

            // AWS
            //kmsKeyIdBase64 = kmsKeyHelper.CreateKeyWithAwsKmsProvider();
            //autoEncryptionHelper.EncryptedWriteAndRead(kmsKeyIdBase64, KmsKeyLocation.AWS);

            // Azure
            //kmsKeyIdBase64 = kmsKeyHelper.CreateKeyWithAzureKmsProvider();
            //autoEncryptionHelper.EncryptedWriteAndRead(kmsKeyIdBase64, KmsKeyLocation.Azure);

            // GCP
            //kmsKeyIdBase64 = kmsKeyHelper.CreateKeyWithGcpKmsProvider();
            //autoEncryptionHelper.EncryptedWriteAndRead(kmsKeyIdBase64, KmsKeyLocation.GCP);

            autoEncryptionHelper.QueryWithNonEncryptedClient();

            Console.ReadKey();
        }
    }
}
