Intro
---

This is a guide for ...
https://docs.mongodb.com/ecosystem/use-cases/client-side-field-level-encryption-guide/

Requirements
---

Similar to CSFLE guide requirements w/additional dependencies

MongoDB Server 4.2 Enterprise
-mongocryptd
something similar to:
The 4.2-compatible drivers use the Enterprise-only mongocryptd process to parse the JSON schema and apply the encryption rules when reading or writing documents. Automatic encryption requires the mongocryptd process.

MongoDB Driver Compatible with CSFLE (NodeJS)

(Additional) Dependencies

Setup
---
install deps


-Identify different runnable script/files
master-key.txt

Run this Example
---

- start up 
  - mongocryptd
  - mongod --dbPath or config file
- generate the key
- user should insert value of key id into the script that creates the
  client with JSON schema
- run encrypted client to execute insert of sample data (needs to be
  created)
- run both clients to demonstrate retrieved data format


