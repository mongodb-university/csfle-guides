# Go CSFLE Example

## Steps

1. Clone this repository:

   ```sh
     git clone https://github.com/mongodb-university/csfle-guides.git
     cd csfle-guides/gocse
   ```

   Work from the gocse directory for the remainder of these
   instructions.

2. Start a `mongod` instance (Enterprise version >= 4.2) running on port 27017

3. Make sure you have the `master-key.txt` file in the root of your
   execution environment. This is a 96-byte cryptographically-secure generated
   master encryption key required to run this example project. To generate your
   own master key or use a KMS, refer to the [CSFLE Use Case
   Guide](https://docs.mongodb.com/drivers/security/client-side-field-level-encryption-guide/).

   The settings for each supported KMS are located in
   `kms/provider.go`. In this project we have used [godotenv](https://github.com/joho/godotenv) to
   load our environmental variables. Feel free to replace the calls to `GetCheckedEnv` with string values if you
   prefer.

4. If `mongocryptd` is not installed in your system path, ensure you specify its
   location in `csfle/encrypted_client.go` in the `EncryptedClient` function.

5. Uncomment the initializer for the KMS provider you would like to use in `main.go`

   ```go
   // preferredProvider := kms.AWSProvider()
   // preferredProvider := kms.AzureProvider()
   // preferredProvider := kms.GCPProvider()
   preferredProvider := kms.LocalProvider(localMasterKey())
   ```

6. Run the `go run -tags=cse .`. This does the following:

   - Local Key Management Service (KMS)

     - Reads in the local key from `master-key.txt`

   - Remote KMS

     - Reads in configuration information for your preferred KMS.
     - Fetches the master key from the KMS.

   - Universally

     - Creates a data key in the **keyVault** collection.
     - Uses this newly created data key to finish setting up JSON schema for automatic encryption.
     - Creates a new, encrypted client configured for automatic Client-Side Field Level Encryption (CSFLE).
     - Inserts a sample document with the encrypted client.
     - Issues a find operation with the encrypted client and prints it out, showing the document in unencrypted form.
     - Issues a find operation with an unencrypted client and prints it out, showing the fields specified for encryption are unreadable.

7. Update the code to insert a document with the regular client. What happens?
