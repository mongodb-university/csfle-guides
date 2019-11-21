# Node CSFLE Example

## Steps

1. Start a locally running `mongod` version >= 4.2 running on port 27017
2. Install dependencies with npm

   ```js
   npm install
   ```

3. Run the `make-data-key.js` script to make a data key. If there is an
   existing data key in the **encryption.\_\_keyVault** collection this script
   will not create a duplicate data key.

   ```js
   node make-data-key.js
   ```

4. Run the `client.js` script to insert a document with the CSFLE-enabled client
   and then read that document with both a regular and CSFLE-enabled client. You
   will see that the CSFLE-enabled client prints the document out in plaintext,
   and the regular client prints the document out with encrypted fields in
   binary format. This is safe to run multiple times as the insert operation
   used is an update with upsert specified.

   ```js
   node clients.js
   ```

5. Suggestion: Try inserting a document with the regular client. What happens?
