package csfle

import (
	"context"
	"fmt"

	"github.com/mongodb-university/csfle-guides/gocse/kms"
	"go.mongodb.org/mongo-driver/mongo"
	"go.mongodb.org/mongo-driver/mongo/options"
)

func EncryptedClient(keyVaultNamespace, uri string, schemaMap map[string]interface{}, provider kms.Provider) (*mongo.Client, error) {
	// extra options that can be specified
	// see https://github.com/mongodb/specifications/blob/master/source/client-side-encryption/client-side-encryption.rst#extraoptions
	extraOptions := map[string]interface{}{
		// "mongocryptdURI":, defaults to "mongodb://localhost:27020"
		// "mongocryptdBypassSpawn":, defaults to false
		// "mongocryptdSpawnPath":, defaults to an empty string and spawns mongocryptd from the system path
		// "mongocryptdSpawnArgs":, defauls to ["--idleShutdownTimeoutSecs=60"]
	}
	autoEncryptionOpts := options.AutoEncryption().
		SetKmsProviders(provider.Credentials()).
		SetKeyVaultNamespace(keyVaultNamespace).
		SetSchemaMap(schemaMap).
		SetExtraOptions(extraOptions)
	client, err := mongo.Connect(context.TODO(), options.Client().ApplyURI(uri).SetAutoEncryptionOptions(autoEncryptionOpts))
	if err != nil {
		return nil, fmt.Errorf("Connect error for encrypted client: %v", err)
	}
	return client, nil
}

func InsertTestData(client *mongo.Client, doc interface{}, dbName, collName string) error {
	collection := client.Database(dbName).Collection(collName)

	if err := collection.Drop(context.TODO()); err != nil {
		return fmt.Errorf("Drop error: %v", err)
	}

	if _, err := collection.InsertOne(context.TODO(), doc); err != nil {
		return fmt.Errorf("InsertOne error: %v", err)
	}
	return nil
}
