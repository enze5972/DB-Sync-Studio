const fs = require('fs');
const path = require('path');
const { spawnSync } = require('child_process');

const ROOT = path.resolve(__dirname, '..');
const TAURI_DIR = path.join(ROOT, 'app-shell', 'src-tauri');
const ICON_DIR = path.join(TAURI_DIR, 'icons');
const SOURCE_SVG = path.join(TAURI_DIR, 'app-icon.svg');
const SOURCE_PNG = path.join(TAURI_DIR, 'app-icon.png');

main();

function main() {
  ensureSourceExists(SOURCE_SVG);
  cleanDir(ICON_DIR);
  ensureDir(ICON_DIR);

  renderSourcePng();
  generatePngVariants();
  generateIco();
  generateIcns();

  console.log('Generated Tauri icons in: ' + ICON_DIR);
}

function renderSourcePng() {
  run('sips -s format png "' + escapePath(SOURCE_SVG) + '" --out "' + escapePath(SOURCE_PNG) + '"', ROOT);
}

function generatePngVariants() {
  const sizes = [16, 32, 48, 64, 128, 256, 512, 1024];
  for (let i = 0; i < sizes.length; i += 1) {
    const size = sizes[i];
    const target = path.join(ICON_DIR, size + 'x' + size + '.png');
    resizePng(SOURCE_PNG, target, size);
  }

  copyFile(path.join(ICON_DIR, '1024x1024.png'), path.join(ICON_DIR, 'icon.png'));
}

function generateIco() {
  const sizes = [32, 16, 48, 64, 128, 256];
  const pngFiles = [];
  for (let i = 0; i < sizes.length; i += 1) {
    pngFiles.push(path.join(ICON_DIR, sizes[i] + 'x' + sizes[i] + '.png'));
  }
  writeIco(path.join(ICON_DIR, 'icon.ico'), pngFiles);
}

function generateIcns() {
  const iconsetDir = path.join(ICON_DIR, 'icon.iconset');
  cleanDir(iconsetDir);

  copyFile(path.join(ICON_DIR, '16x16.png'), path.join(iconsetDir, 'icon_16x16.png'));
  copyFile(path.join(ICON_DIR, '32x32.png'), path.join(iconsetDir, 'icon_16x16@2x.png'));
  copyFile(path.join(ICON_DIR, '32x32.png'), path.join(iconsetDir, 'icon_32x32.png'));
  copyFile(path.join(ICON_DIR, '64x64.png'), path.join(iconsetDir, 'icon_32x32@2x.png'));
  copyFile(path.join(ICON_DIR, '128x128.png'), path.join(iconsetDir, 'icon_128x128.png'));
  copyFile(path.join(ICON_DIR, '256x256.png'), path.join(iconsetDir, 'icon_128x128@2x.png'));
  copyFile(path.join(ICON_DIR, '256x256.png'), path.join(iconsetDir, 'icon_256x256.png'));
  copyFile(path.join(ICON_DIR, '512x512.png'), path.join(iconsetDir, 'icon_256x256@2x.png'));
  copyFile(path.join(ICON_DIR, '512x512.png'), path.join(iconsetDir, 'icon_512x512.png'));
  copyFile(path.join(ICON_DIR, '1024x1024.png'), path.join(iconsetDir, 'icon_512x512@2x.png'));

  run('iconutil -c icns "' + escapePath(iconsetDir) + '" -o "' + escapePath(path.join(ICON_DIR, 'icon.icns')) + '"', ROOT);
  fs.rmSync(iconsetDir, { recursive: true, force: true });
}

function resizePng(source, target, size) {
  run('sips -z ' + size + ' ' + size + ' "' + escapePath(source) + '" --out "' + escapePath(target) + '"', ROOT);
}

function writeIco(target, pngFiles) {
  const images = pngFiles.map(function (filePath) {
    const data = fs.readFileSync(filePath);
    const size = parseInt(path.basename(filePath).split('x')[0], 10);
    return {
      data: data,
      size: size
    };
  });

  const header = Buffer.alloc(6);
  header.writeUInt16LE(0, 0);
  header.writeUInt16LE(1, 2);
  header.writeUInt16LE(images.length, 4);

  const directory = Buffer.alloc(images.length * 16);
  let offset = header.length + directory.length;
  for (let i = 0; i < images.length; i += 1) {
    const image = images[i];
    const entryOffset = i * 16;
    const dimension = image.size >= 256 ? 0 : image.size;
    directory.writeUInt8(dimension, entryOffset);
    directory.writeUInt8(dimension, entryOffset + 1);
    directory.writeUInt8(0, entryOffset + 2);
    directory.writeUInt8(0, entryOffset + 3);
    directory.writeUInt16LE(1, entryOffset + 4);
    directory.writeUInt16LE(32, entryOffset + 6);
    directory.writeUInt32LE(image.data.length, entryOffset + 8);
    directory.writeUInt32LE(offset, entryOffset + 12);
    offset += image.data.length;
  }

  const dataParts = [header, directory];
  for (let j = 0; j < images.length; j += 1) {
    dataParts.push(images[j].data);
  }
  fs.writeFileSync(target, Buffer.concat(dataParts));
}

function ensureSourceExists(filePath) {
  if (!fs.existsSync(filePath)) {
    throw new Error('Source icon not found: ' + filePath);
  }
}

function run(command, cwd) {
  const result = spawnSync(command, {
    cwd: cwd,
    shell: true,
    stdio: 'inherit'
  });
  if (result.status !== 0) {
    throw new Error('Command failed: ' + command);
  }
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

function escapePath(filePath) {
  return filePath.replace(/\\/g, '\\\\');
}
