const kms = require("./kms");
require("dotenv").config();

const kmsClient = kms.localCsfleHelper();

async function main(regularClient, csfleClient) {
  try {
    let dataKey = null; // change this to the base64 encoded data key generated from make-data-key.js
    if (dataKey === null) {
      let err = new Error(
        `dataKey is required.
Run make-data-key.js and ensure you copy and paste the output into client.js
      `
      );
      throw err;
    }

    regularClient = await kmsClient.getRegularClient();
    let schemaMap = kmsClient.createJsonSchemaMap(dataKey);
    csfleClient = await kmsClient.getCsfleEnabledClient(schemaMap);

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
    };

    const regularClientPatientsColl = regularClient
      .db("medicalRecords")
      .collection("patients");
    const csfleClientPatientsColl = csfleClient
      .db("medicalRecords")
      .collection("patients");

    // Performs the insert operation with the csfle-enabled client
    // We're using an update with an upsert so that subsequent runs of this script
    // don't insert new documents
    await csfleClientPatientsColl.updateOne(
      { ssn: exampleDocument["ssn"] },
      { $set: exampleDocument },
      { upsert: true }
    );

    // Performs a read using the encrypted client, querying on an encrypted field
    const csfleFindResult = await csfleClientPatientsColl.findOne({
      ssn: exampleDocument["ssn"],
    });
    console.log(
      "Document retrieved with csfle enabled client:\n",
      csfleFindResult
    );

    // Performs a read using the regular client. We must query on a field that is
    // not encrypted.
    // Try - query on the ssn field. What is returned?
    const regularFindResult = await regularClientPatientsColl.findOne({
      name: "Jon Doe",
    });
    console.log("Document retrieved with regular client:\n", regularFindResult);
  } finally {
    if (regularClient) await regularClient.close();
    if (csfleClient) await csfleClient.close();
  }
}

let regularClient = null;
let csfleClient = null;
main(regularClient, csfleClient).catch(console.dir);
