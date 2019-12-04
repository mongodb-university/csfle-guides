# Python CSFLE Example

## Steps

1. Clone this repository and navigate to the **python** directory.

   ```sh
   git clone https://github.com/mongodb-university/csfle-guides.git
   cd python
   ```

   Work from the **python** directory for the remainder of these instructions.

2. Start a locally running `mongod` instance (Enterprise version >= 4.2) running on port 27017

3. We recommend that you create a local environment to encapsulate the
   dependencies. We use [**pyenv**](https://realpython.com/intro-to-pyenv/)

   ```python
   pyenv virtualenv 3.7.3 csfle
   pyenv local csfle
   pip install -r requirements.txt
   ```

4. Make sure you have the `master-key.txt` file in the root of your execution
   environment. This is a 96-byte cryptographically-secure generated master
   encryption key required to run this example project. To generate your own
   master key or use a KMS, refer to the [CSFLE Use Case Guide](https://docs.mongodb.com/ecosystem/use-cases/client-side-field-level-encryption-guide/).

5. Run the `make_data_key.py` script to make a data key. If there is an
   existing data key in the **encryption.__keyVault** collection this script
   will not create a duplicate data key.

   ```python
   python make_data_key.py
   ```

   This outputs a base64 encoded string of the UUID of your newly created data key. Paste
   this into `app.py` where you see this line

   ```python
   data_key = None  # replace with your base64 data key!
   ```

6. Run the `app.py` script to insert a document with the CSFLE-enabled client
   and then read that document with it as well as a regular client. You will see
   that the CSFLE-enabled client prints the document out in plaintext,
   and the regular client prints the document out with encrypted fields in
   binary format. This is safe to run multiple times as the insert operation
   used is an update with upsert specified.

   ```python
   python app.py
   ```

7. Suggestion: Try inserting a document with the regular client. What happens?
