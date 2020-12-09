module.exports = {
  env: {
    node: true,
    es6: true,
  },
  parserOptions: {
    sourceType: "module",
    allowImportExportEverywhere: true,
    ecmaVersion: 8,
  },
  root: true,
  extends: ["eslint:recommended", "plugin:prettier/recommended"],
};
