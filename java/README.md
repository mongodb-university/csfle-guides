# Java CSFLE Example

## Steps

1. Start a `mongod` instance (version >= 4.2) running on port 27017
2. Set up your build environment for the Java project. The following build
   files are located in the project root:
   - `pom.xml` for [Maven](https://maven.apache.org/)
   - `build.gradle` for [Gradle](https://gradle.org/)

3. Make sure you have the `master-key.txt` file in the root of your
   execution environment. This is a 96-byte cryptographically-secure generated
   master encryption key required to run this example project. To generate your
   own master key or use a KMS, refer to the [CSFLE Use Case
   Guide](https://docs.mongodb.com/ecosystem/use-cases/client-side-field-level-encryption-guide/).

3. Run the `main` method in `DataEncryptionKeyCreator.java` to insert
   a new data key in the **encryption.__keyVault** collection. Copy the key id
   that is printed on the console.

4. Update the `keyId` variable in `InsertDataWithEncryptedFields.java`
   with the value from the previous step.

5. Run the `main` method in `InsertDataWithEncryptedFields.java`. This
   executes the following:

   - Inserts (using upsert) a sample document with encrypted fields
   - Retrieves the sample document with a CSFLE-enabled client to view the
     decrypted fields.
   - Attempts to retrieve the sample document with a normal client on an
     encrypted field, and is unsuccessful.
   - Retrieves the sample document with a normal client on a normal
     field. The retrieved document contains encrypted fields that are not
     human-readable.

6. Update the code to insert a document with the regular client. What happens?
