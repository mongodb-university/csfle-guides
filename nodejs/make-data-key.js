const kms = require("./kms");
require("dotenv").config();

async function main(unencryptedClient) {
  const kmsClient = kms.localCsfleHelper();

  unencryptedClient = await kmsClient.getRegularClient();

  const dataKey = await kmsClient.findOrCreateDataKey(unencryptedClient);
  console.log(
    `Base64 data key. Copy and paste this into clients.js:

${dataKey}
`
  );
}
let unencryptedClient = null;
main()
  .catch(console.dir)
  .finally(async () => {
    console.log("closing client");
    if (unencryptedClient) await unencryptedClient.close();
    console.log("closed client");
  });
