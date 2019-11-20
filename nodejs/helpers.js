const fs = require("fs")
const { MongoClient } = require("mongodb")
const { ClientEncryption } = require("mongodb-client-encryption")
export function readMasterKey(path = "./master-key.txt") {
  return fs.readFileSync(path)
}

export function createJsonSchema(dataKey = null) {
  if (dataKey === null) {
    throw new Error(
      "dataKey is a required argument. Ensure you've defined it in clients.js",
    )
  }
  return {
    "medicalRecords.patients": {
      bsonType: "object",
      encryptMetadata: {
        keyId: [
          {
            $binary: {
              base64: `${dataKey}`,
              subType: "04",
            },
          },
        ],
      },
      properties: {
        insurance: {
          bsonType: "object",
          properties: {
            policyNumber: {
              encrypt: {
                bsonType: "int",
                algorithm: "AEAD_AES_256_CBC_HMAC_SHA_512-Deterministic",
              },
            },
          },
        },
        medicalRecords: {
          encrypt: {
            bsonType: "array",
            algorithm: "AEAD_AES_256_CBC_HMAC_SHA_512-Random",
          },
        },
        bloodType: {
          encrypt: {
            bsonType: "string",
            algorithm: "AEAD_AES_256_CBC_HMAC_SHA_512-Random",
          },
        },
        ssn: {
          encrypt: {
            bsonType: "int",
            algorithm: "AEAD_AES_256_CBC_HMAC_SHA_512-Deterministic",
          },
        },
      },
    },
  }
}

export class ClientBuilder {
  constructor({
    kmsProviders = null,
    keyAltNames = "demo-data-key",
    keyDB = "encryption",
    keyColl = "__keyVault",
    schema = null,
    connectionString = "mongodb://localhost:27017",
    mongocryptdBypassSpawn = false,
    mongocryptdSpawnPath = "mongocryptd",
  } = {}) {
    if (kmsProviders === null) {
      throw new Error("kmsProviders is required")
    }
    this.kmsProviders = kmsProviders
    this.keyAltNames = keyAltNames
    this.keyDB = keyDB
    this.keyColl = keyColl
    this.keyVaultNamespace = `${keyDB}.${keyColl}`
    this.schema = schema
    this.connectionString = connectionString
    this.mongocryptdBypassSpawn = mongocryptdBypassSpawn
    this.mongocryptdSpawnPath = mongocryptdSpawnPath
  }

  async clientPrototype() {}

  async regularClient() {}

  async csfleEnabledClient() {}
}
