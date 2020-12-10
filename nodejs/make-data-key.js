const kms = require("./kms");
require("dotenv").config();

async function main(unencryptedClient) {
  try {
    const kmsClient = kms.localCsfleHelper();

    unencryptedClient = await kmsClient.getRegularClient();

    const dataKey = await kmsClient.findOrCreateDataKey(unencryptedClient);
    console.log(
      `Base64 data key. Copy and paste this into clients.js:

${dataKey}
`
    );
  } finally {
    if (unencryptedClient) await unencryptedClient.close();
  }
}
let unencryptedClient = null;
main(unencryptedClient).catch(console.dir);
