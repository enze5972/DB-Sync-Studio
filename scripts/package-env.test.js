const assert = require('assert/strict');
const fs = require('fs');
const path = require('path');
const vm = require('vm');
const test = require('node:test');

const SCRIPT_PATH = path.join(__dirname, 'package.js');

function loadPackageScript(envOverrides) {
  const source = fs.readFileSync(SCRIPT_PATH, 'utf8')
    .replace('main();', '')
    .replace('const JAVA_HOME_REQUIRED = resolveJavaHome(JAVA_VERSION_REQUIRED);', 'const JAVA_HOME_REQUIRED = "C:/Java";');
  const context = vm.createContext({
    console,
    require,
    module: {},
    exports: {},
    __dirname: __dirname,
    __filename: SCRIPT_PATH,
    process: {
      argv: ['node', 'package.js', 'windows'],
      platform: 'win32',
      env: Object.assign({}, envOverrides || {})
    }
  });
  vm.runInContext(source, context, { filename: SCRIPT_PATH });
  return context;
}

test('buildJavaEnv preserves existing Windows Path entries', function () {
  const script = loadPackageScript({
    Path: 'C:/ProgramData/chocolatey/bin'
  });
  const env = script.buildJavaEnv();

  assert.equal(env.JAVA_HOME, 'C:/Java');
  assert.equal(
    env.PATH,
    'C:/Java/bin' + path.delimiter + 'C:/ProgramData/chocolatey/bin'
  );
  assert.equal(env.Path, env.PATH);
});

test('buildToolPath reads Windows Path entries', function () {
  const script = loadPackageScript({
    Path: 'C:/ProgramData/chocolatey/bin'
  });
  const toolPath = script.buildToolPath();

  assert.ok(toolPath.includes('C:/ProgramData/chocolatey/bin'));
});

test('cleanupAppImageArtifacts removes generated AppImage outputs but keeps script and unrelated files', function () {
  const script = loadPackageScript();
  const tmpDir = fs.mkdtempSync(path.join(__dirname, 'package-env-'));
  const appDir = path.join(tmpDir, 'db-sync-studio.AppDir');
  const appImage = path.join(tmpDir, 'db-sync-studio_0.1.0_amd64.AppImage');
  const buildScript = path.join(tmpDir, 'build_appimage.sh');
  const notesFile = path.join(tmpDir, 'notes.txt');

  fs.mkdirSync(appDir);
  fs.writeFileSync(appImage, 'binary');
  fs.writeFileSync(buildScript, '#!/usr/bin/env bash\n');
  fs.writeFileSync(notesFile, 'keep me');

  script.cleanupAppImageArtifacts(tmpDir);

  assert.equal(fs.existsSync(appDir), false);
  assert.equal(fs.existsSync(appImage), false);
  assert.equal(fs.existsSync(buildScript), true);
  assert.equal(fs.existsSync(notesFile), true);

  fs.rmSync(tmpDir, { recursive: true, force: true });
});
