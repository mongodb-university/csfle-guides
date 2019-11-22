const { readMasterKey, CsfleHelper } = require("./helpers")

const localMasterKey = readMasterKey()

const csfleHelper = new CsfleHelper({
  kmsProviders: {
    local: {
      key: localMasterKey,
    },
  },
})

csfleHelper.findOrCreateDataKey().then(key => {
  console.log(
    "Base64 data key. Copy and paste this into clients.js\t",
    key["_id"].toString("base64"),
  )
})
