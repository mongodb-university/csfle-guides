const { readMasterKey, CsfleHelper } = require("./helpers")

const localMasterKey = readMasterKey()

const csfleHelper = new CsfleHelper({
  kmsProviders: {
    local: {
      key: localMasterKey,
    },
  },
})
// Wrapping everything in an async iife to allow use of async/await
;(async () => {
  // change this to the base64 encoded data key generated from make-data-key.js
  let dataKey = null // change this!

  let regularClient = await csfleHelper.getRegularClient()
  let schemeMap = csfleHelper.createJsonSchemaMap(dataKey)
  let csfleClient = await csfleHelper.getCsfleEnabledClient(schemeMap)

  let exampleDocument = {
    name: "Jon Doe",
    ssn: 241014209,
    bloodType: "AB+",
    medicalRecords: [
      {
        weight: 180,
        bloodPressure: "120/80",
      },
    ],
    insurance: {
      provider: "MaestCare",
      policyNumber: 123142,
    },
  }

  const regularClientPatientsColl = regularClient
    .db("medicalRecords")
    .collection("patients")
  const csfleClientPatientsColl = csfleClient
    .db("medicalRecords")
    .collection("patients")

  // Performs the insert operation with the csfle-enabled client
  // We're using an update with an upsert so that subsequent runs of this script
  // don't insert new documents
  await csfleClientPatientsColl.updateOne(
    { ssn: exampleDocument["ssn"] },
    { $set: exampleDocument },
    { upsert: true },
  )

  // Performs a read using the encrypted client, querying on an encrypted field
  const csfleFindResult = await csfleClientPatientsColl.findOne({
    ssn: exampleDocument["ssn"],
  })
  console.log(
    "Document retreived with csfle enabled client:\n",
    csfleFindResult,
  )

  // Performs a read using the regular client. We must query on a field that is
  // not encrypted.
  // Try - query on the ssn field. What is returned?
  const regularFindResult = await regularClientPatientsColl.findOne({
    name: "Jon Doe",
  })
  console.log("Document retreived with regular client:\n", regularFindResult)

  regularClient.close()
  csfleClient.close()
})()
