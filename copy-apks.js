import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const srcDebug = path.join(__dirname, 'app/build/outputs/apk/debug/app-debug.apk');
const srcRelease = path.join(__dirname, 'app/build/outputs/apk/release/app-release.apk');

const destDebug = path.join(__dirname, '.build-outputs/app-debug.apk');
const destRelease = path.join(__dirname, '.build-outputs/app-release.apk');

// Ensure destination folder exists
if (!fs.existsSync(path.dirname(destDebug))) {
  fs.mkdirSync(path.dirname(destDebug), { recursive: true });
}

if (fs.existsSync(srcDebug)) {
  fs.copyFileSync(srcDebug, destDebug);
  console.log('Successfully copied debug APK to .build-outputs');
} else {
  console.log('Debug APK not found in app/build/outputs');
}

if (fs.existsSync(srcRelease)) {
  fs.copyFileSync(srcRelease, destRelease);
  console.log('Successfully copied release APK to .build-outputs');
} else {
  console.log('Release APK not found in app/build/outputs');
}
