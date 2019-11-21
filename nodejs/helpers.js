const fs = require("fs")
const mongodb = require("mongodb")
const { ClientEncryption } = require("mongodb-client-encryption")(mongodb)
const { MongoClient, Binary } = mongodb

module.exports = {
  readMasterKey: function(path = "./master-key.txt") {
    return fs.readFileSync(path)
  },
  ClientBuilder: class {
    constructor({
      kmsProviders = null,
      keyAltNames = "demo-data-key",
      keyDB = "encryption",
      keyColl = "__keyVault",
      schema = null,
      connectionString = "mongodb://localhost:27017",
      mongocryptdBypassSpawn = false,
      mongocryptdSpawnPath = "mongocryptd"
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
      this.regularClient = null
      this.csfleClient = null
    }

    /**
     *
     * @param {MongoClient} client
     */
    async ensureUniqueIndexOnKeyVault(client) {
      try {
        await client
          .db(this.keyDB)
          .collection(this.keyColl)
          .createIndex("keyAltNames", {
            unique: true,
            partialFilterExpression: {
              keyAltNames: {
                $exists: true
              }
            }
          })
      } catch (e) {
        console.error(e)
        process.exit(1)
      }
    }

    /**
     * In the guide, https://docs.mongodb.com/ecosystem/use-cases/client-side-field-level-encryption-guide/,
     * we create the data key and then show that it is created by
     * using a find_one query. Here, in implementation, we only create the key if
     * it doesn't already exist, ensuring we only have one local data key.
     */
    async findOrCreateDataKey() {
      const client = await new MongoClient(this.connectionString, {
        useNewUrlParser: true,
        useUnifiedTopology: true
      }).connect()

      const encryption = new ClientEncryption(client, {
        keyVaultNamespace: this.keyVaultNamespace,
        kmsProviders: this.kmsProviders
      })

      await this.ensureUniqueIndexOnKeyVault(client)

      let dataKey = await client
        .db(this.keyDB)
        .collection(this.keyColl)
        .findOne({ keyAltNames: { $in: [this.keyAltNames] } })

      if (dataKey === null) {
        dataKey = await encryption.createDataKey("local", {
          keyAltNames: [this.keyAltNames]
        })
      }

      client.close()
      return dataKey
    }

    async getRegularClient() {
      if (this.regularClient) {
        return this.regularClient
      }
      this.regularClient = await new MongoClient(this.connectionString, {
        useNewUrlParser: true,
        useUnifiedTopology: true
      }).connect()
      return this.regularClient
    }

    async getCsfleEnabledClient(schemaMap = null) {
      if (schemaMap === null) {
        throw new Error(
          "schemaMap is a required argument. Build it using the ClientBuilder.createJsonSchemaMap method"
        )
      }
      if (this.csfleClient) {
        return this.csfleClient
      }
      this.csfleClient = await new MongoClient(this.connectionString, {
        useNewUrlParser: true,
        useUnifiedTopology: true,
        monitorCommands: true,
        autoEncryption: {
          keyVaultNamespace: this.keyVaultNamespace,
          kmsProviders: this.kmsProviders,
          schemaMap
        }
      }).connect()
      return this.csfleClient
    }

    createJsonSchemaMap(dataKey = null) {
      if (dataKey === null) {
        throw new Error(
          "dataKey is a required argument. Ensure you've defined it in clients.js"
        )
      }
      return {
        [this.keyVaultNamespace]: {
          bsonType: "object",
          encryptMetadata: {
            keyId: [new Binary(Buffer.from(dataKey, "base64"), 4)]
          },
          properties: {
            insurance: {
              bsonType: "object",
              properties: {
                policyNumber: {
                  encrypt: {
                    bsonType: "int",
                    algorithm: "AEAD_AES_256_CBC_HMAC_SHA_512-Deterministic"
                  }
                }
              }
            },
            medicalRecords: {
              encrypt: {
                bsonType: "array",
                algorithm: "AEAD_AES_256_CBC_HMAC_SHA_512-Random"
              }
            },
            bloodType: {
              encrypt: {
                bsonType: "string",
                algorithm: "AEAD_AES_256_CBC_HMAC_SHA_512-Random"
              }
            },
            ssn: {
              encrypt: {
                bsonType: "int",
                algorithm: "AEAD_AES_256_CBC_HMAC_SHA_512-Deterministic"
              }
            }
          }
        }
      }
    }
  }
}
