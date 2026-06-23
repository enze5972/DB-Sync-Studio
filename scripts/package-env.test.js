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
