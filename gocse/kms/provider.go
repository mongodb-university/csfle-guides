package kms

import (
	"log"
	"os"

	"github.com/fatih/structs"
)

// GetCheckedEnv gets the specified environmental variable value and ensures
// it isn't empty
func GetCheckedEnv(env string) string {
	val := os.Getenv(env)
	if val == "" {
		log.Fatalf("You are attempting to use a KMS provider but not providing the required environmental value for %s. Please see the README", env)
	}
	return val
}

// Provider is a common interface for each KMS provider (aws, azure, gcp, local)
type Provider interface {
	Name() string
	Credentials() map[string]map[string]interface{}
	DataKeyOpts() interface{}
}

// gcpKMSCredentials used to access this KMS provider
// See https://github.com/mongodb/specifications/blob/master/source/client-side-encryption/client-side-encryption.rst#kmsproviders
type gcpKMSCredentials struct {
	Email      string `structs:"email"`
	PrivateKey string `structs:"privateKey"`
	Endpoint   string `structs:"endpoint,omitempty"` // optional, defaults to oauth2.googleapis.com
}

// gcpKMSDataKeyOpts are the data key options used for this KMS provider.
// See https://github.com/mongodb/specifications/blob/master/source/client-side-encryption/client-side-encryption.rst#datakeyopts
type gcpKMSDataKeyOpts struct {
	ProjectID  string `bson:"projectId"`
	Location   string `bson:"location"`
	KeyRing    string `bson:"keyRing"`
	KeyName    string `bson:"keyName"`
	KeyVersion string `bson:"keyVersion,omitempty"` // optional, defaults to the key's primary version
	Endpoint   string `bson:"endpoint,omitempty"`   // optional, defaults to cloudkms.googleapis.com
}

// GCP holds the credentials and master key information to use this KMS
// Get an instance of this with the kms.GCPProvider() method
type GCP struct {
	credentials gcpKMSCredentials `structs:"gcp"`
	dataKeyOpts gcpKMSDataKeyOpts
	name        string
}

// GCPProvider reads in the required environment variables for credentials and master key
// location to use GCP as a KMS
func GCPProvider() *GCP {
	// gcp kms privider credentials
	gcpEmail := GetCheckedEnv("FLE_GCP_EMAIL")
	gcpPrivateKey := GetCheckedEnv("FLE_GCP_PRIVATE_KEY")

	// gcp data key opts
	gcpProjectID := GetCheckedEnv("FLE_GCP_PROJ_ID")
	gcpLocation := GetCheckedEnv("FLE_GCP_LOCATION")
	gcpKeyRing := GetCheckedEnv("FLE_GCP_KEY_RING")
	gcpKeyName := GetCheckedEnv("FLE_GCP_KEY_NAME")

	return &GCP{
		credentials: gcpKMSCredentials{
			Email:      gcpEmail,
			PrivateKey: gcpPrivateKey,
		},
		dataKeyOpts: gcpKMSDataKeyOpts{
			ProjectID: gcpProjectID,
			Location:  gcpLocation,
			KeyRing:   gcpKeyRing,
			KeyName:   gcpKeyName,
		},
		name: "gcp",
	}
}

// Name is the name of this provider
func (g *GCP) Name() string {
	return g.name
}

// Credentials are the credentials for this provider returned in the format necessary
// to immediately pass to the driver
func (g *GCP) Credentials() map[string]map[string]interface{} {
	return map[string]map[string]interface{}{"gcp": structs.Map(g.credentials)}
}

// DataKeyOpts are the data key options for this provider returned in the format necessary
// to immediately pass to the driver
func (g *GCP) DataKeyOpts() interface{} {
	return g.dataKeyOpts
}

// Local holds the credentials and master key information to use this KMS
// Get an instance of this with the kms.LocalProvider() method
type Local struct {
	name string
	key  []byte
}

// LocalProvider returns information for using the local KMS.
// Not for production
func LocalProvider(key []byte) *Local {
	return &Local{name: "local", key: key}
}

// Name is the name of this provider
func (l *Local) Name() string {
	return l.name
}

// Credentials are the credentials for this provider returned in the format necessary
// to immediately pass to the driver
func (l *Local) Credentials() map[string]map[string]interface{} {
	return map[string]map[string]interface{}{
		"local": {
			"key": l.key,
		},
	}
}

// DataKeyOpts are the data key options for this provider returned in the format necessary
// to immediately pass to the driver
func (l *Local) DataKeyOpts() interface{} {
	return nil
}

// azureKMSCredentials used to access this KMS provider
// See https://github.com/mongodb/specifications/blob/master/source/client-side-encryption/client-side-encryption.rst#kmsproviders
type azureKMSCredentials struct {
	TenantID                 string `structs:"tenantId"`
	ClientID                 string `structs:"clientId"`
	ClientSecret             string `structs:"clientSecret"`
	IdentityPlatformEndpoint string `structs:"identityPlatformEndpoint,omitempty"` // optional, defaults to login.microsoftonline.com
}

// The data key options used for this KMS provider.
// See https://github.com/mongodb/specifications/blob/master/source/client-side-encryption/client-side-encryption.rst#datakeyopts
type azureKMSDataKeyOpts struct {
	KeyVaultEndpoint string `bson:"keyVaultEndpoint"`
	KeyName          string `bson:"keyName"`
	KeyVersion       string `bson:"keyVersion,omitempty"` // optional, defaults to the key's primary version
}

// Azure holds the credentials and master key information to use this KMS
// Get an instance of this with the kms.AzureProvider() method
type Azure struct {
	credentials azureKMSCredentials
	dataKeyOpts azureKMSDataKeyOpts
	name        string
}

// AzureProvider reads in the required environment variables for credentials and master key
// location to use Azure as a KMS
func AzureProvider() *Azure {
	// azure kms provider credentials
	azureTenantID := GetCheckedEnv("FLE_AZURE_TENANT_ID")
	azureClientID := GetCheckedEnv("FLE_AZURE_CLIENT_ID")
	azureClientSecret := GetCheckedEnv("FLE_AZURE_CLIENT_SECRET")

	// azure data key opts
	azureKeyVaultEndpoint := GetCheckedEnv("FLE_AZURE_KEYVAULT_ENDPOINT")
	azureKeyName := GetCheckedEnv("FLE_AZURE_KEY_NAME")

	return &Azure{
		credentials: azureKMSCredentials{
			TenantID:     azureTenantID,
			ClientID:     azureClientID,
			ClientSecret: azureClientSecret,
		},
		dataKeyOpts: azureKMSDataKeyOpts{
			KeyVaultEndpoint: azureKeyVaultEndpoint,
			KeyName:          azureKeyName,
		},
		name: "azure",
	}
}

// Name is the name of this provider
func (a *Azure) Name() string {
	return a.name
}

// Credentials are the credentials for this provider returned in the format necessary
// to immediately pass to the driver
func (a *Azure) Credentials() map[string]map[string]interface{} {
	return map[string]map[string]interface{}{"azure": structs.Map(a.credentials)}
}

// DataKeyOpts are the data key options for this provider returned in the format necessary
// to immediately pass to the driver
func (a *Azure) DataKeyOpts() interface{} {
	return a.dataKeyOpts
}

// awsKMSCredentials used to access this KMS provider
// See https://github.com/mongodb/specifications/blob/master/source/client-side-encryption/client-side-encryption.rst#kmsproviders

type awsKMSCredentials struct {
	AccessKeyID     string `structs:"accessKeyId"`
	SecretAccessKey string `structs:"secretAccessKey"`
}

// The data key options used for this KMS provider.
// See https://github.com/mongodb/specifications/blob/master/source/client-side-encryption/client-side-encryption.rst#datakeyopts

type awsKMSDataKeyOpts struct {
	Region   string `bson:"region"`
	KeyARN   string `bson:"key"`                // the aws key arn
	Endpoint string `bson:"endpoint,omitempty"` // optional, defaults to kms.<region>.amazonaws.com
}

// AWS holds the credentials and master key information to use this KMS
// Get an instance of this with the kms.AWSProvider() method
type AWS struct {
	credentials awsKMSCredentials
	dataKeyOpts awsKMSDataKeyOpts
	name        string
}

// AWSProvider reads in the required environment variables for credentials and master key
// location to use AWS as a KMS
func AWSProvider() *AWS {
	// aws kms provider credentials
	awsAccessKeyID := GetCheckedEnv("FLE_AWS_ACCESS_KEY")
	awsSecretAccessKey := GetCheckedEnv("FLE_AWS_SECRET_ACCESS_KEY")

	// aws data key opts
	awsKeyARN := GetCheckedEnv("FLE_AWS_KEY_ARN")
	awsKeyRegion := GetCheckedEnv("FLE_AWS_KEY_REGION")

	return &AWS{
		credentials: awsKMSCredentials{
			AccessKeyID:     awsAccessKeyID,
			SecretAccessKey: awsSecretAccessKey,
		},

		dataKeyOpts: awsKMSDataKeyOpts{
			KeyARN: awsKeyARN,
			Region: awsKeyRegion,
		},
		name: "aws",
	}
}

// Name is the name of this provider
func (a *AWS) Name() string {
	return a.name
}

// Credentials are the credentials for this provider returned in the format necessary
// to immediately pass to the driver
func (a *AWS) Credentials() map[string]map[string]interface{} {
	return map[string]map[string]interface{}{"aws": structs.Map(a.credentials)}
}

// DataKeyOpts are the data key options for this provider returned in the format necessary
// to immediately pass to the driver
func (a *AWS) DataKeyOpts() interface{} {
	return a.dataKeyOpts
}
