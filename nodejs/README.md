# Node CSFLE Example

## Steps

1. Clone this repository and navigate to the **nodejs** directory.

   ```sh
   git clone https://github.com/mongodb-university/csfle-guides.git
   cd nodejs
   ```

   Work from the **nodejs** directory for the remainder of these instructions.

2. Start a locally running `mongod` instance (Enterprise version >= 4.2) running on port 27017

3. Install the dependencies in `package.json`

   ```js
   npm install
   ```

4. Make sure you have the `master-key.txt` file in the root of your execution
   environment. This is a 96-byte cryptographically-secure generated master
   encryption key required to run this example project. To generate your own
   master key or use a KMS, refer to the [CSFLE Use Case Guide](https://docs.mongodb.com/ecosystem/use-cases/client-side-field-level-encryption-guide/).

5. Run the `make-data-key.js` script to make a data key. If there is an
   existing data key in the **encryption.\_\_keyVault** collection this script
   will not create a duplicate data key.

   ```js
   node make-data-key.js
   ```

   This outputs a base64 encoded string of the UUID of your newly created data key. Paste
   this into `clients.js` where you see this line

   ```js
   let dataKey = null // change this!
   ```

6. Run the `clients.js` script to insert a document with the CSFLE-enabled client
   and then read that document with it as well as a regular client. You
   will see that the CSFLE-enabled client prints the document out in plaintext,
   and the regular client prints the document out with encrypted fields in
   binary format. This is safe to run multiple times as the insert operation
   used is an update with `upsert` specified.

   ```js
   node clients.js
   ```

7. Suggestion: Try inserting a document with the regular client. What happens?
