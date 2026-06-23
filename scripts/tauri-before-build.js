const path = require('path');
const { spawnSync } = require('child_process');

function shouldSkipFrontendBuild(env) {
  return env && env.DB_SYNC_SKIP_TAURI_UI_BUILD === '1';
}

function runFrontendBuild(env) {
  if (shouldSkipFrontendBuild(env || process.env)) {
    console.log('Skipping Tauri beforeBuildCommand frontend build because DB_SYNC_SKIP_TAURI_UI_BUILD=1');
    return;
  }

  const result = spawnSync('npm', ['--prefix', path.join(__dirname, '..', 'app-ui'), 'run', 'build'], {
    stdio: 'inherit',
    env: env || process.env
  });
  if (result.status !== 0) {
    throw new Error('Command failed: npm --prefix ../app-ui run build');
  }
}

if (require.main === module) {
  runFrontendBuild(process.env);
}

module.exports = {
  runFrontendBuild,
  shouldSkipFrontendBuild
};
