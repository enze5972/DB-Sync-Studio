const fs = require('fs');
const path = require('path');
const os = require('os');
const { spawnSync } = require('child_process');

const ROOT = path.resolve(__dirname, '..');
const PLATFORM = normalizePlatform(process.argv[2] || process.platform);
const RELEASE_DIR = path.join(ROOT, 'release', PLATFORM);
const BACKEND_DIR = path.join(RELEASE_DIR, 'backend');
const BACKEND_LIB_DIR = path.join(BACKEND_DIR, 'lib');
const UI_DIR = path.join(RELEASE_DIR, 'ui');
const TAURI_BUNDLE_DIR = path.join(RELEASE_DIR, 'tauri-bundle');
const TAURI_RESOURCE_BACKEND_DIR = path.join(ROOT, 'app-shell', 'src-tauri', 'resources', 'backend');
const JAVA_VERSION_REQUIRED = 17;
const JAVA_HOME_REQUIRED = resolveJavaHome(JAVA_VERSION_REQUIRED);

main();

function main() {
  cleanDir(RELEASE_DIR);
  ensureDir(BACKEND_LIB_DIR);
  ensureDir(UI_DIR);

  verifyJavaToolchain();
  buildBackend();
  buildFrontend();
  copyBackendArtifacts();
  copyFrontendArtifacts();
  createLaunchers();
  buildRuntimeImage();
  ensureTauriIcons();
  prepareTauriResources();
  buildTauriBundle();
  writeManifest();

  console.log('');
  console.log('Packaging complete: ' + RELEASE_DIR);
}

function verifyJavaToolchain() {
  const javaResult = runCapture('java -version');
  const majorVersion = parseJavaMajorVersion(javaResult.stderr || javaResult.stdout || '');
  if (majorVersion && majorVersion < JAVA_VERSION_REQUIRED) {
    console.warn('Warning: Java ' + majorVersion + ' detected. Packaging is designed for Java 17+ runtime image creation.');
  }
  if (!majorVersion) {
    console.warn('Warning: Unable to detect Java version. Packaging will continue, but runtime image checks are less reliable.');
  }
  if (JAVA_HOME_REQUIRED) {
    console.log('Using Java ' + JAVA_VERSION_REQUIRED + ' from ' + JAVA_HOME_REQUIRED);
    return;
  }
  throw new Error('Java ' + JAVA_VERSION_REQUIRED + ' not found. Please install JDK ' + JAVA_VERSION_REQUIRED + ' or set JAVA_HOME to a compatible JDK.');
}

function buildBackend() {
  const testFlag = shouldRunPackageTests() ? 'test ' : '-DskipTests ';
  run('mvn -q -f pom.xml -pl app-core -am ' + testFlag + 'package dependency:copy-dependencies -DincludeScope=runtime -DoutputDirectory="' + escapePath(BACKEND_LIB_DIR) + '"', ROOT, buildJavaEnv());
}

function buildFrontend() {
  run('npm --prefix app-ui run build', ROOT);
}

function copyBackendArtifacts() {
  const jarFiles = findFiles(path.join(ROOT, 'app-core', 'target'), /^app-core-.*\.jar$/);
  if (jarFiles.length === 0) {
    throw new Error('Backend jar not found after Maven build.');
  }
  copyFile(jarFiles[0], path.join(BACKEND_DIR, 'app-core.jar'));

  const modelJar = findFiles(path.join(ROOT, 'app-model', 'target'), /^app-model-.*\.jar$/);
  const storeJar = findFiles(path.join(ROOT, 'app-store', 'target'), /^app-store-.*\.jar$/);
  if (modelJar.length > 0) {
    copyFile(modelJar[0], path.join(BACKEND_LIB_DIR, path.basename(modelJar[0])));
  }
  if (storeJar.length > 0) {
    copyFile(storeJar[0], path.join(BACKEND_LIB_DIR, path.basename(storeJar[0])));
  }
}

function copyFrontendArtifacts() {
  const distDir = path.join(ROOT, 'app-ui', 'dist');
  if (!fs.existsSync(distDir)) {
    throw new Error('Frontend dist directory not found: ' + distDir);
  }
  copyDir(distDir, path.join(UI_DIR, 'dist'));
}

function createLaunchers() {
  const shLauncher = [
    '#!/usr/bin/env bash',
    'set -euo pipefail',
    'DIR="$(cd "$(dirname "$0")" && pwd)"',
    'if [ -x "$DIR/runtime/bin/java" ]; then',
    '  JAVA_BIN="$DIR/runtime/bin/java"',
    'elif [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/java" ]; then',
    '  JAVA_BIN="$JAVA_HOME/bin/java"',
    'else',
    '  JAVA_BIN="java"',
    'fi',
    'exec "$JAVA_BIN" -cp "$DIR/app-core.jar:$DIR/lib/*" com.dbsyncstudio.core.DbSyncStudioApplication "$@"'
  ].join('\n');
  writeFile(path.join(BACKEND_DIR, 'run-backend.sh'), shLauncher);
  chmod(path.join(BACKEND_DIR, 'run-backend.sh'), 0o755);

  const batLauncher = [
    '@echo off',
    'setlocal',
    'set DIR=%~dp0',
    'if exist "%DIR%runtime\\bin\\java.exe" (',
    '  set JAVA_BIN=%DIR%runtime\\bin\\java.exe',
    ') else if defined JAVA_HOME (',
    '  if exist "%JAVA_HOME%\\bin\\java.exe" set JAVA_BIN=%JAVA_HOME%\\bin\\java.exe',
    ') else (',
    '  set JAVA_BIN=java',
    ')',
    '"%JAVA_BIN%" -cp "%DIR%app-core.jar;%DIR%lib/*" com.dbsyncstudio.core.DbSyncStudioApplication %*'
  ].join('\r\n');
  writeFile(path.join(BACKEND_DIR, 'run-backend.bat'), batLauncher);
}

function buildRuntimeImage() {
  const jlink = resolveJlink(JAVA_HOME_REQUIRED);
  if (!jlink) {
    console.warn('Warning: jlink not found. Runtime image was not generated.');
    return;
  }

  const runtimeDir = path.join(RELEASE_DIR, 'runtime');
  fs.rmSync(runtimeDir, { recursive: true, force: true });

  const modules = 'java.base,java.logging,java.sql,java.naming,jdk.httpserver,jdk.unsupported';
  run('"' + escapePath(jlink) + '" --add-modules ' + modules + ' --strip-debug --no-header-files --no-man-pages --compress=2 --output "' + escapePath(runtimeDir) + '"', ROOT, buildJavaEnv());
}

function ensureTauriIcons() {
  const requiredFiles = [
    path.join(ROOT, 'app-shell', 'src-tauri', 'icons', 'icon.ico'),
    path.join(ROOT, 'app-shell', 'src-tauri', 'icons', 'icon.icns'),
    path.join(ROOT, 'app-shell', 'src-tauri', 'icons', 'icon.png')
  ];
  if (requiredFiles.every(function (filePath) {
    return fs.existsSync(filePath);
  })) {
    return;
  }

  if (process.platform !== 'darwin') {
    console.warn('Warning: Tauri icons are missing and this platform cannot regenerate them automatically.');
    return;
  }

  run('node scripts/generate-tauri-icons.js', ROOT);
}

function prepareTauriResources() {
  cleanDir(TAURI_RESOURCE_BACKEND_DIR);
  copyDir(BACKEND_DIR, TAURI_RESOURCE_BACKEND_DIR);
  const runtimeDir = path.join(RELEASE_DIR, 'runtime');
  if (fs.existsSync(runtimeDir)) {
    copyDir(runtimeDir, path.join(TAURI_RESOURCE_BACKEND_DIR, 'runtime'));
  }
}

function buildTauriBundle() {
  const cargo = resolveCargo();
  if (!cargo) {
    console.warn('Warning: cargo not found. Tauri bundle was not generated.');
    return;
  }

  validateBundleInputs();

  const tauriEnv = Object.assign({}, process.env, {
    PATH: buildToolPath(),
    DB_SYNC_SKIP_TAURI_UI_BUILD: '1'
  });
  applyLinuxRuntimeLibraryPath(tauriEnv);

  const result = spawnSync(cargo, ['tauri', 'build'], {
    cwd: path.join(ROOT, 'app-shell'),
    env: tauriEnv,
    stdio: 'inherit'
  });

  const bundleDir = path.join(ROOT, 'app-shell', 'src-tauri', 'target', 'release', 'bundle');
  if (fs.existsSync(bundleDir)) {
    injectBackendIntoAppBundle(bundleDir);
    finalizeMacosDmg(bundleDir);
    copyDir(bundleDir, TAURI_BUNDLE_DIR);
    writeTauriBundleManifest(bundleDir);
    ensureDmgPresent();
  }

  if (result.status !== 0) {
    if (fs.existsSync(bundleDir) && listFilesRecursive(TAURI_BUNDLE_DIR).some(function (filePath) {
      return /\.dmg$/i.test(filePath);
    })) {
      console.warn('Warning: cargo tauri build exited with code ' + result.status + ', but bundle artifacts were recovered.');
      return;
    }
    dumpLinuxAppImageDiagnostics(bundleDir);
    throw new Error('Command failed: cargo tauri build');
  }
}

function dumpLinuxAppImageDiagnostics(bundleDir) {
  if (PLATFORM !== 'linux') {
    return;
  }

  const appimageDir = path.join(bundleDir, 'appimage');
  const buildScriptPath = path.join(appimageDir, 'build_appimage.sh');
  console.log('');
  console.log('=== Linux AppImage diagnostics ===');
  console.log('bundle directory: ' + bundleDir);
  console.log('appimage directory: ' + appimageDir);

  if (!fs.existsSync(appimageDir)) {
    console.warn('Warning: AppImage directory not found, cannot collect extra diagnostics.');
    return;
  }

  printDirectorySnapshot(appimageDir);

  if (!fs.existsSync(buildScriptPath)) {
    console.warn('Warning: build_appimage.sh not found, cannot rerun AppImage bundling with tracing.');
    return;
  }

  console.log('--- build_appimage.sh ---');
  process.stdout.write(fs.readFileSync(buildScriptPath, 'utf8'));
  if (!fs.readFileSync(buildScriptPath, 'utf8').endsWith('\n')) {
    process.stdout.write('\n');
  }

  cleanupAppImageArtifacts(appimageDir);
  console.log('--- rerun: bash -x build_appimage.sh ---');
  const rerun = spawnSync('bash', ['-x', 'build_appimage.sh'], {
    cwd: appimageDir,
    env: Object.assign({}, process.env, {
      PATH: buildToolPath()
    }),
    stdio: 'inherit'
  });
  console.log('--- AppImage diagnostic rerun exit code: ' + rerun.status + ' ---');
}

function printDirectorySnapshot(dir) {
  console.log('--- appimage directory snapshot ---');
  fs.readdirSync(dir).sort().forEach(function (entry) {
    const entryPath = path.join(dir, entry);
    const stat = fs.statSync(entryPath);
    const suffix = stat.isDirectory() ? '/' : '';
    console.log(entry + suffix);
  });
}

function cleanupAppImageArtifacts(appimageDir) {
  fs.readdirSync(appimageDir).forEach(function (entry) {
    if (entry === 'build_appimage.sh') {
      return;
    }
    if (!/\.AppDir$/.test(entry) && !/\.AppImage$/.test(entry)) {
      return;
    }
    fs.rmSync(path.join(appimageDir, entry), { recursive: true, force: true });
  });
}

function applyLinuxRuntimeLibraryPath(env) {
  if (PLATFORM !== 'linux') {
    return env;
  }

  const runtimeLibDir = path.join(TAURI_RESOURCE_BACKEND_DIR, 'runtime', 'lib');
  const runtimeServerDir = path.join(runtimeLibDir, 'server');
  const candidates = [runtimeServerDir, runtimeLibDir].filter(function (dirPath) {
    return fs.existsSync(dirPath);
  });
  if (candidates.length === 0) {
    return env;
  }

  const currentValue = env.LD_LIBRARY_PATH || '';
  env.LD_LIBRARY_PATH = candidates.concat(currentValue ? [currentValue] : []).join(path.delimiter);
  return env;
}

function finalizeMacosDmg(bundleDir) {
  if (PLATFORM !== 'macos') {
    return;
  }

  const dmgDir = path.join(bundleDir, 'dmg');
  ensureDir(dmgDir);

  const appBundlePath = findMacosAppBundle(bundleDir);
  if (!appBundlePath) {
    return;
  }

  const finalDmgName = resolveFinalDmgName(bundleDir);
  const finalDmgPath = path.join(dmgDir, finalDmgName);
  const stagingDir = path.join(os.tmpdir(), 'db-sync-studio-dmg-stage');
  cleanDir(stagingDir);

  copyDir(appBundlePath, path.join(stagingDir, path.basename(appBundlePath)));
  createApplicationsLink(stagingDir);

  if (fs.existsSync(finalDmgPath)) {
    fs.rmSync(finalDmgPath, { force: true });
  }

  run('hdiutil create -volname "DB Sync Studio" -srcfolder "' + escapePath(stagingDir) + '" -ov -format UDZO "' + escapePath(finalDmgPath) + '"', ROOT, {
    PATH: buildToolPath()
  });
}

function injectBackendIntoAppBundle(bundleDir) {
  if (PLATFORM !== 'macos') {
    return;
  }

  const macosDir = path.join(bundleDir, 'macos');
  if (!fs.existsSync(macosDir)) {
    return;
  }

  const appBundles = findFiles(macosDir, /\.app$/i).filter(function (filePath) {
    return /\.app$/i.test(filePath);
  });
  if (appBundles.length === 0) {
    return;
  }

  const appBundlePath = appBundles[0];
  const resourcesDir = path.join(appBundlePath, 'Contents', 'Resources');
  const targetBackendDir = path.join(resourcesDir, 'backend');
  cleanDir(targetBackendDir);
  copyDir(TAURI_RESOURCE_BACKEND_DIR, targetBackendDir);
}

function findMacosAppBundle(bundleDir) {
  const macosDir = path.join(bundleDir, 'macos');
  if (!fs.existsSync(macosDir)) {
    return null;
  }

  const appBundles = findFiles(macosDir, /\.app$/i).filter(function (filePath) {
    return /\.app$/i.test(filePath);
  });
  return appBundles.length > 0 ? appBundles[0] : null;
}

function resolveFinalDmgName(bundleDir) {
  const finalDmgFiles = findFiles(path.join(bundleDir, 'dmg'), /^DB Sync Studio_.*\.dmg$/i);
  if (finalDmgFiles.length > 0) {
    return path.basename(finalDmgFiles[0]);
  }
  const rwDmgFiles = findFiles(path.join(bundleDir, 'macos'), /^rw\..*\.dmg$/i);
  if (rwDmgFiles.length > 0) {
    return path.basename(rwDmgFiles[0]).replace(/^rw\./, '');
  }
  return 'DB Sync Studio_0.1.0_' + (process.arch === 'arm64' ? 'aarch64' : 'x64') + '.dmg';
}

function createApplicationsLink(targetDir) {
  const linkPath = path.join(targetDir, 'Applications');
  try {
    if (fs.existsSync(linkPath)) {
      fs.rmSync(linkPath, { recursive: true, force: true });
    }
    fs.symlinkSync('/Applications', linkPath);
  } catch (err) {
    // The dmg still works without the shortcut, so keep packaging resilient.
  }
}

function patchMountedDiskImageBackend(imagePath) {
  const mountInfo = attachDiskImage(imagePath);
  try {
    if (!mountInfo || !mountInfo.mountPoint) {
      return;
    }
    const appBundlePath = path.join(mountInfo.mountPoint, 'DB Sync Studio.app');
    const resourcesDir = path.join(appBundlePath, 'Contents', 'Resources');
    const targetBackendDir = path.join(resourcesDir, 'backend');
    if (!fs.existsSync(resourcesDir)) {
      return;
    }
    cleanDir(targetBackendDir);
    copyDir(TAURI_RESOURCE_BACKEND_DIR, targetBackendDir);
  } finally {
    if (mountInfo && mountInfo.device) {
      detachMountedDiskImage(mountInfo.device);
    }
  }
}

function attachDiskImage(imagePath) {
  const result = runCapture('hdiutil attach -readwrite -noverify -noautoopen "' + escapePath(imagePath) + '"', {
    allowFailure: true,
    cwd: ROOT,
    env: Object.assign({}, process.env, {
      PATH: buildToolPath()
    })
  });
  if (result.status !== 0) {
    throw new Error('Failed to attach disk image: ' + imagePath);
  }

  const output = (result.stdout || '') + '\n' + (result.stderr || '');
  const lines = output.split(/\r?\n/);
  for (let i = 0; i < lines.length; i += 1) {
    const line = lines[i].trim();
    if (!line.startsWith('/dev/disk')) {
      continue;
    }
    const parts = line.split(/\t+/);
    if (parts.length >= 3) {
      return {
        device: parts[0],
        mountPoint: parts[2]
      };
    }
  }

  throw new Error('Failed to determine mount point for disk image: ' + imagePath);
}

function detachMountedDiskImage(imagePath) {
  if (/^\/dev\/disk\d+/.test(imagePath)) {
    run('hdiutil detach "' + imagePath + '"', ROOT, {
      PATH: buildToolPath()
    });
    return;
  }

  const result = runCapture('hdiutil info', { allowFailure: true });
  const output = (result.stdout || '') + '\n' + (result.stderr || '');
  const blocks = output.split('================================================');
  for (let i = 0; i < blocks.length; i += 1) {
    const block = blocks[i];
    if (block.indexOf('image-path      : ' + imagePath) === -1 && block.indexOf('image-alias     : ' + imagePath) === -1) {
      continue;
    }
    const deviceMatch = block.match(/\/dev\/disk[0-9]+/);
    if (deviceMatch) {
      run('hdiutil detach ' + deviceMatch[0], ROOT, {
        PATH: buildToolPath()
      });
      return;
    }
  }
}

function writeTauriBundleManifest(bundleDir) {
  const files = listFilesRecursive(TAURI_BUNDLE_DIR).map(function (filePath) {
    return path.relative(TAURI_BUNDLE_DIR, filePath);
  });
  const dmgFiles = files.filter(function (fileName) {
    return /\.dmg$/i.test(fileName);
  });
  const manifest = {
    platform: PLATFORM,
    sourceDir: path.relative(ROOT, bundleDir),
    files: files,
    dmgFiles: dmgFiles
  };
  writeFile(path.join(TAURI_BUNDLE_DIR, 'manifest.json'), JSON.stringify(manifest, null, 2) + '\n');
}

function ensureDmgPresent() {
  if (PLATFORM !== 'macos') {
    return;
  }
  const dmgFiles = listFilesRecursive(TAURI_BUNDLE_DIR).filter(function (filePath) {
    return /\.dmg$/i.test(filePath);
  });
  if (dmgFiles.length === 0) {
    console.warn('Warning: Tauri bundle completed, but no .dmg artifact was found under ' + TAURI_BUNDLE_DIR);
  }
}

function validateBundleInputs() {
  const checks = [
    { label: 'backend jar', filePath: path.join(BACKEND_DIR, 'app-core.jar') },
    { label: 'backend launcher sh', filePath: path.join(BACKEND_DIR, 'run-backend.sh') },
    { label: 'backend launcher bat', filePath: path.join(BACKEND_DIR, 'run-backend.bat') },
    { label: 'backend runtime image', filePath: path.join(RELEASE_DIR, 'runtime') },
    { label: 'frontend dist', filePath: path.join(UI_DIR, 'dist') },
    { label: 'tauri config', filePath: path.join(ROOT, 'app-shell', 'src-tauri', 'tauri.conf.json') },
    { label: 'tauri icon png', filePath: path.join(ROOT, 'app-shell', 'src-tauri', 'icons', 'icon.png') },
    { label: 'tauri icon ico', filePath: path.join(ROOT, 'app-shell', 'src-tauri', 'icons', 'icon.ico') },
    { label: 'tauri icon icns', filePath: path.join(ROOT, 'app-shell', 'src-tauri', 'icons', 'icon.icns') }
  ];
  const missing = checks.filter(function (item) {
    return !fs.existsSync(item.filePath);
  });
  if (missing.length > 0) {
    throw new Error('Packaging inputs missing: ' + missing.map(function (item) {
      return item.label + ' (' + item.filePath + ')';
    }).join(', '));
  }
}

function writeManifest() {
  const manifest = {
    platform: PLATFORM,
    generatedAt: new Date().toISOString(),
    javaVersionRequired: JAVA_VERSION_REQUIRED,
    backend: {
      jar: 'backend/app-core.jar',
      launcherSh: 'backend/run-backend.sh',
      launcherBat: 'backend/run-backend.bat',
      libDir: 'backend/lib',
      runtimeDir: fs.existsSync(path.join(RELEASE_DIR, 'runtime')) ? 'runtime' : null
    },
    frontend: {
      distDir: 'ui/dist'
    },
    tauriBundleDir: fs.existsSync(path.join(RELEASE_DIR, 'tauri-bundle')) ? 'tauri-bundle' : null
  };
  writeFile(path.join(RELEASE_DIR, 'manifest.json'), JSON.stringify(manifest, null, 2) + '\n');
}

function resolveCargo() {
  const candidates = [];
  if (process.platform === 'win32') {
    candidates.push(path.join(process.env.USERPROFILE || '', '.cargo', 'bin', 'cargo.exe'));
    candidates.push(path.join('C:\\', 'Program Files', 'Rust stable GNU 64-bit', 'bin', 'cargo.exe'));
  } else {
    candidates.push(path.join(os.homedir(), '.cargo', 'bin', 'cargo'));
    candidates.push('/usr/local/opt/rustup/bin/cargo');
    candidates.push('/opt/homebrew/bin/cargo');
  }

  for (let i = 0; i < candidates.length; i += 1) {
    const candidate = candidates[i];
    if (candidate && fs.existsSync(candidate)) {
      return candidate;
    }
  }

  const command = process.platform === 'win32' ? 'where cargo' : 'command -v cargo';
  const result = runCapture(command, { allowFailure: true });
  if (result.status !== 0) {
    return null;
  }
  const output = (result.stdout || '').trim().split(/\r?\n/).filter(Boolean);
  return output.length > 0 ? output[0] : null;
}

function resolveJlink(javaHome) {
  if (!javaHome) {
    return null;
  }
  const candidate = path.join(javaHome, 'bin', process.platform === 'win32' ? 'jlink.exe' : 'jlink');
  return fs.existsSync(candidate) ? candidate : null;
}

function parseJavaMajorVersion(output) {
  const match = output.match(/version \"([0-9]+)(?:\.([0-9]+))?/i);
  if (!match) {
    return null;
  }
  if (match[1] === '1' && match[2]) {
    return parseInt(match[2], 10);
  }
  return parseInt(match[1], 10);
}

function resolveJavaHome(requiredMajor) {
  const candidates = [];
  if (process.env.JAVA_HOME) {
    candidates.push(process.env.JAVA_HOME);
  }
  const commandHome = resolveJavaHomeFromCommand(requiredMajor);
  if (commandHome) {
    candidates.push(commandHome);
  }
  candidates.push.apply(candidates, resolveJavaHomeDirectories());

  const seen = new Set();
  for (let i = 0; i < candidates.length; i += 1) {
    const javaHome = candidates[i];
    if (!javaHome || seen.has(javaHome)) {
      continue;
    }
    seen.add(javaHome);
    if (isJavaHomeCompatible(javaHome, requiredMajor)) {
      return javaHome;
    }
  }
  return null;
}

function resolveJavaHomeFromCommand(requiredMajor) {
  if (process.platform !== 'darwin') {
    return null;
  }
  const result = runCapture('/usr/libexec/java_home -v ' + requiredMajor, { allowFailure: true });
  if (result.status !== 0) {
    return null;
  }
  const output = (result.stdout || '').trim();
  return output.length > 0 ? output : null;
}

function resolveJavaHomeDirectories() {
  const roots = [];
  if (process.platform === 'darwin') {
    roots.push('/Library/Java/JavaVirtualMachines');
  } else if (process.platform === 'win32') {
    roots.push('C:\\Program Files\\Java');
    roots.push('C:\\Program Files\\Eclipse Adoptium');
    roots.push('C:\\Program Files\\Microsoft\\jdk');
  } else {
    roots.push('/usr/lib/jvm');
    roots.push('/usr/java');
    roots.push('/opt/java');
  }

  const homes = [];
  for (let i = 0; i < roots.length; i += 1) {
    const root = roots[i];
    if (!fs.existsSync(root)) {
      continue;
    }
    const entries = fs.readdirSync(root);
    for (let j = 0; j < entries.length; j += 1) {
      const entry = entries[j];
      const candidate = path.join(root, entry);
      if (fs.existsSync(path.join(candidate, 'bin', process.platform === 'win32' ? 'java.exe' : 'java'))) {
        homes.push(candidate);
      }
      if (process.platform === 'darwin') {
        const macHome = path.join(candidate, 'Contents', 'Home');
        if (fs.existsSync(path.join(macHome, 'bin', 'java'))) {
          homes.push(macHome);
        }
      }
    }
  }
  return homes;
}

function isJavaHomeCompatible(javaHome, requiredMajor) {
  const javaBin = path.join(javaHome, 'bin', process.platform === 'win32' ? 'java.exe' : 'java');
  if (!fs.existsSync(javaBin)) {
    return false;
  }
  const result = spawnSync(javaBin, ['-version'], {
    cwd: ROOT,
    encoding: 'utf8',
    shell: false,
    stdio: ['ignore', 'pipe', 'pipe']
  });
  if (result.status !== 0) {
    return false;
  }
  const majorVersion = parseJavaMajorVersion((result.stderr || '') + '\n' + (result.stdout || ''));
  return majorVersion !== null && majorVersion >= requiredMajor;
}

function buildJavaEnv() {
  if (!JAVA_HOME_REQUIRED) {
    return process.env;
  }
  const env = Object.assign({}, process.env);
  env.JAVA_HOME = JAVA_HOME_REQUIRED;
  const javaBinDir = path.join(JAVA_HOME_REQUIRED, 'bin');
  const existingPath = getEnvPath(env);
  setEnvPath(env, javaBinDir + path.delimiter + existingPath);
  return env;
}

function shouldRunPackageTests() {
  const value = String(process.env.PACKAGE_RUN_TESTS || '').trim().toLowerCase();
  return value === '1' || value === 'true' || value === 'yes';
}

function normalizePlatform(platform) {
  if (platform === 'darwin') {
    return 'macos';
  }
  if (platform === 'win32') {
    return 'windows';
  }
  if (platform === 'linux') {
    return 'linux';
  }
  return platform;
}

function buildToolPath() {
  const extras = [];
  extras.push(path.join(os.homedir(), '.cargo', 'bin'));
  extras.push('/usr/local/opt/rustup/bin');
  extras.push('/opt/homebrew/bin');
  const existing = getEnvPath(process.env).split(path.delimiter).filter(Boolean);
  return extras.concat(existing).filter(Boolean).join(path.delimiter);
}

function getEnvPath(env) {
  if (!env) {
    return '';
  }
  return env.PATH || env.Path || '';
}

function setEnvPath(env, value) {
  env.PATH = value;
  env.Path = value;
}

function run(command, cwd, extraEnv) {
  const result = spawnSync(command, {
    cwd: cwd,
    shell: true,
    env: extraEnv ? Object.assign({}, process.env, extraEnv) : process.env,
    stdio: 'inherit'
  });
  if (result.status !== 0) {
    throw new Error('Command failed: ' + command);
  }
}

function runCapture(command, options) {
  const result = spawnSync(command, {
    cwd: options && options.cwd ? options.cwd : ROOT,
    shell: true,
    env: options && options.env ? Object.assign({}, process.env, options.env) : process.env,
    encoding: 'utf8',
    stdio: ['ignore', 'pipe', 'pipe']
  });
  if (!options || !options.allowFailure) {
    if (result.status !== 0) {
      throw new Error('Command failed: ' + command);
    }
  }
  return result;
}

function findFiles(dir, pattern) {
  if (!fs.existsSync(dir)) {
    return [];
  }
  return fs.readdirSync(dir)
    .map(function (entry) {
      return path.join(dir, entry);
    })
    .filter(function (file) {
      return pattern.test(path.basename(file));
    });
}

function cleanDir(dir) {
  fs.rmSync(dir, { recursive: true, force: true });
  ensureDir(dir);
}

function ensureDir(dir) {
  fs.mkdirSync(dir, { recursive: true });
}

function copyFile(source, target) {
  ensureDir(path.dirname(target));
  fs.copyFileSync(source, target);
}

function copyDir(source, target) {
  ensureDir(target);
  for (const entry of fs.readdirSync(source)) {
    const sourcePath = path.join(source, entry);
    const targetPath = path.join(target, entry);
    const stat = fs.statSync(sourcePath);
    if (stat.isDirectory()) {
      copyDir(sourcePath, targetPath);
    } else {
      copyFile(sourcePath, targetPath);
    }
  }
}

function listFilesRecursive(dir) {
  if (!fs.existsSync(dir)) {
    return [];
  }

  const files = [];
  const entries = fs.readdirSync(dir);
  for (let i = 0; i < entries.length; i += 1) {
    const entry = entries[i];
    const sourcePath = path.join(dir, entry);
    const stat = fs.statSync(sourcePath);
    if (stat.isDirectory()) {
      const nested = listFilesRecursive(sourcePath);
      for (let j = 0; j < nested.length; j += 1) {
        files.push(nested[j]);
      }
    } else {
      files.push(sourcePath);
    }
  }
  return files;
}

function writeFile(filePath, content) {
  ensureDir(path.dirname(filePath));
  fs.writeFileSync(filePath, content, 'utf8');
}

function chmod(filePath, mode) {
  try {
    fs.chmodSync(filePath, mode);
  } catch (err) {
    // ignore on platforms that do not support chmod semantics well
  }
}

function escapePath(filePath) {
  return filePath.replace(/\\/g, '\\\\');
}
