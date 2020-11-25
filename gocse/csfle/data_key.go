package csfle

import (
	"context"
	"encoding/base64"
	"fmt"

	"github.com/mongodb-university/csfle-guides/gocse/kms"
	"go.mongodb.org/mongo-driver/mongo"
	"go.mongodb.org/mongo-driver/mongo/options"
)

// GetDataKey creates a new data key and returns the base64 encoding to be used
// in schema configuration for automatic encryption
func GetDataKey(keyVaultNamespace, uri string, provider kms.Provider) (string, error) {

	// configuring encryption options by setting the keyVault namespace and the kms providers information
	// we configure this client to fetch the master key so that we can
	// create a data key in the next step
	clientEncryptionOpts := options.ClientEncryption().SetKeyVaultNamespace(keyVaultNamespace).SetKmsProviders(provider.Credentials())
	keyVaultClient, err := mongo.Connect(context.TODO(), options.Client().ApplyURI(uri))
	if err != nil {
		return "", fmt.Errorf("Client encryption connect error %v", err)
	}
	clientEnc, err := mongo.NewClientEncryption(keyVaultClient, clientEncryptionOpts)
	if err != nil {
		return "", fmt.Errorf("NewClientEncryption error %v", err)
	}
	defer func() {
		_ = clientEnc.Close(context.TODO())
	}()

	// specify the master key information that will be used to
	// encrypt the data key(s) that will in turn be used to encrypt
	// fields, and create the data key
	dataKeyOpts := options.DataKey().SetMasterKey(provider.DataKeyOpts())
	dataKeyID, err := clientEnc.CreateDataKey(context.TODO(), provider.Name(), dataKeyOpts)
	if err != nil {
		return "", fmt.Errorf("create data key error %v", err)
	}

	return base64.StdEncoding.EncodeToString(dataKeyID.Data), nil
}
