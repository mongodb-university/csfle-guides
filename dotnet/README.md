# .NET CSFLE Example

## Windows Only

Currently, remote KMS support is Windows only.

## Steps

1. Clone this repository:

   ```
     git clone https://github.com/mongodb-university/csfle-guides.git
   ```

   Navigate to the CSFLE directory. Work from this directory for the remainder of these
   instructions.

2. Start a `mongod` instance (Enterprise version >= 4.2) running on port 27017

3. Make sure you have the `master-key.txt` file in the root of your
   execution environment if you are using a local KMS. This is a 96-byte cryptographically-secure generated
   master encryption key required to run this example project. To generate your
   own master key or use a KMS, refer to the 
   [CSFLE Use Case Guide](https://www.mongodb.com/docs/drivers/security/client-side-field-level-encryption-guide/).

4. Configure variables for your preferred remote KMS, if
   necessary. Out of the box, this application looks for KMS credentials and data key options
   via the following environmental variables:

   - AWS

     - KMS Credentials

       - FLE_AWS_ACCESS_KEY
       - FLE_AWS_SECRET_ACCESS_KEY

     - Master Key Options

       - FLE_AWS_KEY_ARN
       - FLE_AWS_KEY_REGION
       - FLE_AWS_ENDPOINT (Optional)

   - GCP

     - KMS Credentials

       - FLE_GCP_EMAIL
       - FLE_GCP_PRIVATE_KEY
       - FLE_GCP_IDENTITY_ENDPOINT (Optional)

     - Master Key Options

       - FLE_GCP_KEY_NAME
       - FLE_GCP_KEY_RING
       - FLE_GCP_PROJ_ID
       - FLE_GCP_KEY_LOC (Optional)
       - FLE_GCP_KEY_VERSION (Optional)
       - FLE_GCP_KMS_ENDPOINT (Optional)

   - Azure

     - KMS Credentials

       - FLE_AZURE_CLIENT_ID
       - FLE_AZURE_CLIENT_SECRET
       - FLE_AZURE_TENANT_ID
       - FLE_AZURE_IDENTIFY_PLATFORM_ENPDOINT (Optional)

     - Master Key Options

       - FLE_AZURE_KEY_NAME
       - FLE_AZURE_KEYVAULT_ENDPOINT
       - FLE_AZURE_KEY_VERSION (Optional)

5. In `Program.cs` you'll find methods for the supported KMS providers. Uncomment
   your preferred provider and run the program.
