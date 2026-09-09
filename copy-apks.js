import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const srcDebug = path.join(__dirname, 'app/build/outputs/apk/debug/app-debug.apk');
const srcRelease = path.join(__dirname, 'app/build/outputs/apk/release/app-release.apk');

const destDebug = path.join(__dirname, '.build-outputs/app-debug.apk');
const destRelease = path.join(__dirname, '.build-outputs/app-release.apk');
const destDebugPublic = path.join(__dirname, 'build-outputs/app-debug.apk');
const destReleasePublic = path.join(__dirname, 'build-outputs/app-release.apk');

// Ensure destination folders exist
[destDebug, destDebugPublic].forEach(dest => {
  const dir = path.dirname(dest);
  if (!fs.existsSync(dir)) {
    fs.mkdirSync(dir, { recursive: true });
  }
});

if (fs.existsSync(srcDebug)) {
  fs.copyFileSync(srcDebug, destDebug);
  fs.copyFileSync(srcDebug, destDebugPublic);
  console.log('Successfully copied debug APK to build outputs');
} else if (fs.existsSync(destDebugPublic) && !fs.existsSync(destDebug)) {
  fs.copyFileSync(destDebugPublic, destDebug);
  console.log('Synchronized debug APK from public build-outputs');
} else if (fs.existsSync(destDebug) && !fs.existsSync(destDebugPublic)) {
  fs.copyFileSync(destDebug, destDebugPublic);
  console.log('Synchronized debug APK to public build-outputs');
} else if (fs.existsSync(destDebugPublic)) {
  console.log('Debug APK verified in build-outputs');
} else {
  console.log('Debug APK not found');
}

if (fs.existsSync(srcRelease)) {
  fs.copyFileSync(srcRelease, destRelease);
  fs.copyFileSync(srcRelease, destReleasePublic);
  console.log('Successfully copied release APK to build outputs');
} else if (fs.existsSync(destReleasePublic) && !fs.existsSync(destRelease)) {
  fs.copyFileSync(destReleasePublic, destRelease);
  console.log('Synchronized release APK from public build-outputs');
} else if (fs.existsSync(destRelease) && !fs.existsSync(destReleasePublic)) {
  fs.copyFileSync(destRelease, destReleasePublic);
  console.log('Synchronized release APK to public build-outputs');
} else if (fs.existsSync(destReleasePublic)) {
  console.log('Release APK verified in build-outputs');
} else {
  console.log('Release APK not found');
}
