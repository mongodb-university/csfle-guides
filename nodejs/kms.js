const { CsfleHelper } = require("./helpers");
const fs = require("fs");

const envToKey = {
  // aws credentials
  FLE_AWS_ACCESS_KEY: "accessKeyId",
  FLE_AWS_SECRET_ACCESS_KEY: "secretAccessKey",
  // aws masterKey information
  FLE_AWS_KEY_ARN: "key",
  FLE_AWS_KEY_REGION: "region",
  FLE_AWS_KEY_ENDPOINT: "endpoint", // optional, AWS KMS URL

  // gcp credentials
  FLE_GCP_EMAIL: "email",
  FLE_GCP_PRIVATE_KEY: "privateKey",
  FLE_GCP_CRED_ENDPOINT: "endpoint", // optional, defaults to "oauth2.googleapis.com"
  // gcp masterKey information
  FLE_GCP_PROJ_ID: "projectId",
  FLE_GCP_LOCATION: "location",
  FLE_GCP_KEY_RING: "keyRing",
  FLE_GCP_KEY_NAME: "keyName",
  FLE_GCP_KEY_ENDPOINT: "endpoint", // optional
  FLE_GCP_KEY_VERSION: "keyVersion", // optional, defaults to "googleapis.com/auth/cloudkms"

  // azure credentials
  FLE_AZURE_TENANT_ID: "tenantId",
  FLE_AZURE_CLIENT_ID: "clientId",
  FLE_AZURE_CLIENT_SECRET: "clientSecret",
  FLE_AZURE_IDENTITY_PLATFORM_ENDPOINT: "identityPlatformEndpoint", // optional, defaults to "login.microsoftonline.com"
  // azure masterKey information
  FLE_AZURE_KEY_NAME: "keyName",
  FLE_AZURE_KEYVAULT_ENDPOINT: "keyVaultEndpoint",
  FLE_AZURE_KEY_VERSION: "keyVersion", // optional
};

function getCheckedEnv(...envVars) {
  if (envVars.some((elem) => !process.env[elem])) {
    throw new Error(
      `Found no environmental variables set for the following values: ${envVars
        .filter((elem) => !process.env[elem])
        .join(", ")}`
    );
  } else {
    return envVars.reduce((acc, curr) => {
      acc[envToKey[curr]] = process.env[curr];
      return acc;
    }, {});
  }
}

function readMasterKey(path = "./master-key.txt") {
  return fs.readFileSync(path);
}

module.exports = {
  localCsfleHelper: (path = "./master-key.txt") => {
    return new CsfleHelper({
      provider: "local",
      kmsProviders: {
        local: {
          key: readMasterKey(path),
        },
      },
    });
  },

  awsCsfleHelper: () => {
    const { accessKeyId, secretAccessKey, key, region } = getCheckedEnv(
      "FLE_AWS_ACCESS_KEY",
      "FLE_AWS_SECRET_ACCESS_KEY",
      "FLE_AWS_KEY_ARN",
      "FLE_AWS_KEY_REGION"
    );
    return new CsfleHelper({
      provider: "aws",
      kmsProviders: {
        aws: {
          accessKeyId,
          secretAccessKey,
        },
      },
      masterKey: {
        key,
        region,
      },
    });
  },

  gcpCsfleHelper: () => {
    const {
      email,
      privateKey,
      projectId,
      location,
      keyRing,
      keyName,
    } = getCheckedEnv(
      "FLE_GCP_EMAIL",
      "FLE_GCP_PRIVATE_KEY",
      "FLE_GCP_PROJ_ID",
      "FLE_GCP_LOCATION",
      "FLE_GCP_KEY_RING",
      "FLE_GCP_KEY_NAME"
    );
    return new CsfleHelper({
      provider: "gcp",
      kmsProviders: {
        gcp: {
          email,
          privateKey,
        },
      },
      masterKey: {
        projectId,
        location,
        keyRing,
        keyName,
      },
    });
  },

  azureCsfleHelper: () => {
    const {
      tenantId,
      clientId,
      clientSecret,
      keyVaultEndpoint,
      keyName,
    } = getCheckedEnv(
      "FLE_AZURE_TENANT_ID",
      "FLE_AZURE_CLIENT_ID",
      "FLE_AZURE_CLIENT_SECRET",
      "FLE_AZURE_KEYVAULT_ENDPOINT",
      "FLE_AZURE_KEY_NAME"
    );
    return new CsfleHelper({
      provider: "azure",
      kmsProviders: {
        azure: {
          tenantId,
          clientId,
          clientSecret,
        },
      },
      masterKey: {
        keyVaultEndpoint,
        keyName,
      },
    });
  },
};
