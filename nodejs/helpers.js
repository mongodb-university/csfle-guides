const mongodb = require("mongodb");
const { ClientEncryption } = require("mongodb-client-encryption");
const { MongoClient, Binary } = mongodb;

module.exports = {
  CsfleHelper: class {
    constructor({
      provider = null,
      kmsProviders = null,
      masterKey = null,
      keyAltNames = "demo-data-key",
      keyDB = "encryption",
      keyColl = "__keyVault",
      schema = null,
      connectionString = "mongodb://localhost:27017",
      mongocryptdBypassSpawn = false,
      mongocryptdSpawnPath = "mongocryptd",
    } = {}) {
      if (kmsProviders === null) {
        throw new Error("kmsProviders is required");
      }
      if (provider === null) {
        throw new Error("provider is required");
      }
      if (provider !== "local" && masterKey === null) {
        throw new Error("masterKey is required");
      }
      this.kmsProviders = kmsProviders;
      this.masterKey = masterKey;
      this.provider = provider;
      this.keyAltNames = keyAltNames;
      this.keyDB = keyDB;
      this.keyColl = keyColl;
      this.keyVaultNamespace = `${keyDB}.${keyColl}`;
      this.schema = schema;
      this.connectionString = connectionString;
      this.mongocryptdBypassSpawn = mongocryptdBypassSpawn;
      this.mongocryptdSpawnPath = mongocryptdSpawnPath;
      this.regularClient = null;
      this.csfleClient = null;
    }

    /**
     * Creates a unique, partial index in the key vault collection
     * on the ``keyAltNames`` field.
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
                $exists: true,
              },
            },
          });
      } catch (e) {
        throw new Error(e);
      }
    }

    /**
     * In the guide, https://docs.mongodb.com/ecosystem/use-cases/client-side-field-level-encryption-guide/,
     * we create the data key and then show that it is created by
     * retreiving it using a findOne query. Here, in implementation, we only
     * create the key if it doesn't already exist, ensuring we only have one
     * local data key.
     *
     * @param {MongoClient} client
     */
    async findOrCreateDataKey(client) {
      const encryption = new ClientEncryption(client, {
        keyVaultNamespace: this.keyVaultNamespace,
        kmsProviders: this.kmsProviders,
      });

      await this.ensureUniqueIndexOnKeyVault(client);

      let dataKey = await client
        .db(this.keyDB)
        .collection(this.keyColl)
        .findOne({ keyAltNames: { $in: [this.keyAltNames] } });

      if (dataKey === null) {
        dataKey = await encryption.createDataKey(this.provider, {
          masterKey: this.masterKey,
        });
        console.dir(Object.keys(encryption));
        return dataKey.toString("base64");
      }
      return dataKey["_id"].toString("base64");
    }

    async getRegularClient() {
      const client = new MongoClient(this.connectionString, {
        useNewUrlParser: true,
        useUnifiedTopology: true,
      });
      return await client.connect();
    }

    async getCsfleEnabledClient(schemaMap = null) {
      if (schemaMap === null) {
        throw new Error(
          "schemaMap is a required argument. Build it using the CsfleHelper.createJsonSchemaMap method"
        );
      }
      const client = new MongoClient(this.connectionString, {
        useNewUrlParser: true,
        useUnifiedTopology: true,
        monitorCommands: true,
        autoEncryption: {
          keyVaultNamespace: this.keyVaultNamespace,
          kmsProviders: this.kmsProviders,
          schemaMap,
        },
      });
      return await client.connect();
    }

    createJsonSchemaMap(dataKey) {
      return {
        "medicalRecords.patients": {
          bsonType: "object",
          encryptMetadata: {
            keyId: [new Binary(Buffer.from(dataKey, "base64"), 4)],
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
      };
    }
  },
};
