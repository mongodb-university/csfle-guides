const { readMasterKey, CsfleHelper } = require("./helpers")

async function main() {
  const localMasterKey = readMasterKey()

  const csfleHelper = new CsfleHelper({
    kmsProviders: {
      local: {
        key: localMasterKey
      }
    }
  })

  const client = await csfleHelper.getRegularClient()

  const dataKey = await csfleHelper.findOrCreateDataKey(client)
  console.log("Base64 data key. Copy and paste this into clients.js\t", dataKey)

  client.close()
}

main().catch(console.dir)
