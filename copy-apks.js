import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const srcDebug = path.join(__dirname, 'app/build/outputs/apk/debug/app-debug.apk');
const srcRelease = path.join(__dirname, 'app/build/outputs/apk/release/app-release.apk');
const srcAab = path.join(__dirname, 'app/build/outputs/bundle/release/app-release.aab');

const destDebug = path.join(__dirname, '.build-outputs/app-debug.apk');
const destRelease = path.join(__dirname, '.build-outputs/app-release.apk');
const destAab = path.join(__dirname, '.build-outputs/app-release.aab');

const destDebugPublic = path.join(__dirname, 'build-outputs/app-debug.apk');
const destReleasePublic = path.join(__dirname, 'build-outputs/app-release.apk');
const destAabPublic = path.join(__dirname, 'build-outputs/app-release.aab');

// Ensure destination folders exist
[destDebug, destDebugPublic, destAab, destAabPublic, srcDebug, srcRelease, srcAab].forEach(dest => {
  const dir = path.dirname(dest);
  if (!fs.existsSync(dir)) {
    fs.mkdirSync(dir, { recursive: true });
  }
});

// Sync debug APK
if (fs.existsSync(srcDebug)) {
  fs.copyFileSync(srcDebug, destDebug);
  fs.copyFileSync(srcDebug, destDebugPublic);
  console.log('Successfully copied debug APK to build outputs');
} else if (fs.existsSync(destDebugPublic)) {
  fs.copyFileSync(destDebugPublic, srcDebug);
  fs.copyFileSync(destDebugPublic, destDebug);
  console.log('Restored debug APK from public build-outputs to source');
} else if (fs.existsSync(destDebug)) {
  fs.copyFileSync(destDebug, srcDebug);
  fs.copyFileSync(destDebug, destDebugPublic);
  console.log('Restored debug APK from .build-outputs to source');
} else {
  console.log('Debug APK not found');
}

// Sync release APK
if (fs.existsSync(srcRelease)) {
  fs.copyFileSync(srcRelease, destRelease);
  fs.copyFileSync(srcRelease, destReleasePublic);
  console.log('Successfully copied release APK to build outputs');
} else if (fs.existsSync(destReleasePublic)) {
  fs.copyFileSync(destReleasePublic, srcRelease);
  fs.copyFileSync(destReleasePublic, destRelease);
  console.log('Restored release APK from public build-outputs to source');
} else if (fs.existsSync(destRelease)) {
  fs.copyFileSync(destRelease, srcRelease);
  fs.copyFileSync(destRelease, destReleasePublic);
  console.log('Restored release APK from .build-outputs to source');
} else {
  console.log('Release APK not found');
}

// Sync release AAB (required by AI Studio for Android deploy/archive)
if (fs.existsSync(srcAab)) {
  fs.copyFileSync(srcAab, destAab);
  fs.copyFileSync(srcAab, destAabPublic);
  console.log('Successfully copied release AAB to build outputs');
} else if (fs.existsSync(destAabPublic)) {
  fs.copyFileSync(destAabPublic, srcAab);
  fs.copyFileSync(destAabPublic, destAab);
  console.log('Restored release AAB from public build-outputs to source');
} else if (fs.existsSync(destAab)) {
  fs.copyFileSync(destAab, srcAab);
  fs.copyFileSync(destAab, destAabPublic);
  console.log('Restored release AAB from .build-outputs to source');
} else {
  console.log('Release AAB not found');
}
