const { readMasterKey, ClientBuilder } = require("./helpers")

const localMasterKey = readMasterKey()

const clientBuilder = new ClientBuilder({
  kmsProviders: {
    local: {
      key: localMasterKey
    }
  }
})

clientBuilder.findOrCreateDataKey().then(key => {
  console.log(
    "Base64 data key. Copy and paste this into clients.js\t",
    key["_id"].toString("base64")
  )
})
