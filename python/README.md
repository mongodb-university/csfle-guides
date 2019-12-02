# Python CSFLE Example

This repository is meant as a companion project to our
[CSFLE Use Case Guide](https://docs.mongodb.com/ecosystem/use-cases/client-side-field-level-encryption-guide/).

## Steps

1. Clone this repository.
2. Make sure you have [`mongocryptd`](https://www.mongodb.com/download-center/enterprise) available on your system.
3. Start a locally running `mongod` version >= 4.2 running on port 27017
4. We recommend that you create a local environment to encapsulate the
   dependencies. We use [**pyenv**](https://realpython.com/intro-to-pyenv/)

   ```python
   pyenv virtualenv 3.7.3 csfle
   pyenv local csfle
   pip install -r requirements.txt
   ```

5. Make sure you have the `master-key.txt` file in the root of your execution
   environment. This is a 96-byte cryptographically-secure generated master
   encryption key required to run this example project. To generate your own
   master key or use a KMS, refer to the [CSFLE Use Case Guide](https://docs.mongodb.com/ecosystem/use-cases/client-side-field-level-encryption-guide/).

6. Run the `make_data_key.py` script to make a data key. If there is an
   existing data key in the **encryption.\_\_keyVault** collection this script
   will not create a duplicate data key.

   ```python
   python make_data_key.py
   ```

   Paste the output of this script into `app.py`, line **39**.

7. Run the `app.py` script to insert a document with the CSFLE-enabled client
   and then read that document with both a regular and CSFLE-enabled client. You
   will see that the CSFLE-enabled client prints the document out in plaintext,
   and the regular client prints the document out with encrypted fields in
   binary format. This is safe to run multiple times as the insert operation
   used is an update with upsert specified.

   ```python
   python app.py
   ```

8. Suggestion: Try inserting a document with the regular client. What happens?
