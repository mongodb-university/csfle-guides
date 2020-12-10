const kms = require("./kms");
require("dotenv").config();

async function main() {
  const kmsClient = kms.localCsfleHelper();

  const unencryptedClient = await kmsClient.getRegularClient();

  const dataKey = await kmsClient.findOrCreateDataKey(unencryptedClient);
  console.log(
    "Base64 data key. Copy and paste this into clients.js\t",
    dataKey
  );
  unencryptedClient.close();
}

main().catch(console.dir);
