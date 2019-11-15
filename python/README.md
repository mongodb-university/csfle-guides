# Python CSFLE Example

## Steps

1. Start a locally running `mongod` version >= 4.2 running on port 27017
2. It is recommended you create a local environment to encapsulate the
   dependencies. We use **pyenv**

   ```python
   pyenv virtualenv 3.7.3 csfle
   pyenv local csfle
   pip install -r requirements.txt
   ```

3. Run the `make-local-data-key.py` script to make a data key. If there is an
   existing data key in the **encryption.__keyVault** collection this script
   will not create a duplicate data key.

   ```python
   python make-local-data-key.py
   ```

4.
