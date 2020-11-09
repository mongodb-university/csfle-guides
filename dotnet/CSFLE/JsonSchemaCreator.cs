using MongoDB.Bson;
using System;

namespace CSFLE
{
    public static class JsonSchemaCreator
    {
        private static readonly string DETERMINISTIC_ENCRYPTION_TYPE = "AEAD_AES_256_CBC_HMAC_SHA_512-Deterministic";
        private static readonly string RANDOM_ENCRYPTION_TYPE = "AEAD_AES_256_CBC_HMAC_SHA_512-Random";

        private static BsonDocument CreateEncryptMetadata(string keyIdBase64)
        {
            var keyId = new BsonBinaryData(Convert.FromBase64String(keyIdBase64), BsonBinarySubType.UuidStandard);
            return new BsonDocument("keyId", new BsonArray(new[] { keyId }));
        }

        private static BsonDocument CreateEncryptedField(string bsonType, bool isDeterministic)
        {
            return new BsonDocument
            {
                {
                    "encrypt",
                    new BsonDocument
                    {
                        { "bsonType", bsonType },
                        { "algorithm", isDeterministic ? DETERMINISTIC_ENCRYPTION_TYPE : RANDOM_ENCRYPTION_TYPE}
                    }
                }
            };
        }

        public static BsonDocument CreateJsonSchema(string keyId)
        {
            return new BsonDocument
            {
                { "bsonType", "object" },
                { "encryptMetadata", CreateEncryptMetadata(keyId) },
                {
                    "properties",
                    new BsonDocument
                    {

                        { "ssn", CreateEncryptedField("int", true) },
                        { "bloodType", CreateEncryptedField("string", false) },
                        { "medicalRecords", CreateEncryptedField("array", false) },
                        {
                            "insurance",
                            new BsonDocument
                            {
                                { "bsonType", "object" },
                                {
                                    "properties",
                                    new BsonDocument
                                    {
                                        { "policyNumber", CreateEncryptedField("int", true) }
                                    }
                                }
                            }
                        }
                    }
                }
            };
        }
    }
}
