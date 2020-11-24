package schema

import (
	"fmt"

	"go.mongodb.org/mongo-driver/bson"
)

func CreateJSONSchema(dataKeyBase64 string) (bson.Raw, error) {
	jsc := `{
	"bsonType": "object",
	"encryptMetadata": {
		"keyId": [
			{
				"$binary": {
					"base64": "%s",
					"subType": "04"
				}
	        }
		]
	},
	"properties": {
		"insurance": {
			"bsonType": "object",
			"properties": {
				"policyNumber": {
					"encrypt": {
						"bsonType": "int",
						"algorithm": "AEAD_AES_256_CBC_HMAC_SHA_512-Deterministic"
					}
				}
			}
		},
		"medicalRecords": {
			"encrypt": {
				"bsonType": "array",
				"algorithm": "AEAD_AES_256_CBC_HMAC_SHA_512-Random"
			}
		},
		"bloodType": {
			"encrypt": {
				"bsonType": "string",
				"algorithm": "AEAD_AES_256_CBC_HMAC_SHA_512-Random"
			}
		},
		"ssn": {
			"encrypt": {
				"bsonType": "int",
				"algorithm": "AEAD_AES_256_CBC_HMAC_SHA_512-Deterministic"
			}
		}
	}
}`
	schema := fmt.Sprintf(jsc, dataKeyBase64)
	var schemaDoc bson.Raw
	if err := bson.UnmarshalExtJSON([]byte(schema), true, &schemaDoc); err != nil {
		return nil, fmt.Errorf("UnmarshalExtJSON error: %v", err)
	}
	return schemaDoc, nil
}
