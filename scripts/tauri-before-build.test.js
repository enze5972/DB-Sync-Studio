const assert = require('assert/strict');
const test = require('node:test');

const { shouldSkipFrontendBuild } = require('./tauri-before-build');

test('shouldSkipFrontendBuild is true only when DB_SYNC_SKIP_TAURI_UI_BUILD=1', function () {
  assert.equal(shouldSkipFrontendBuild({ DB_SYNC_SKIP_TAURI_UI_BUILD: '1' }), true);
  assert.equal(shouldSkipFrontendBuild({ DB_SYNC_SKIP_TAURI_UI_BUILD: '0' }), false);
  assert.equal(shouldSkipFrontendBuild({}), false);
});
