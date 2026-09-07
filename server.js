import http from 'http';
import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

// Port configuration: AI Studio dev environment requires port 3000, while Cloud Run uses PORT (e.g. 8080)
const DEV_PORT = 3000;
const CLOUD_RUN_PORT = process.env.PORT ? parseInt(process.env.PORT, 10) : null;

// Path to the output APKs with fallback across build-outputs and local build folders
const getDebugApkPath = () => {
  const p1 = path.join(__dirname, 'build-outputs/app-debug.apk');
  const p2 = path.join(__dirname, '.build-outputs/app-debug.apk');
  const p3 = path.join(__dirname, 'app/build/outputs/apk/debug/app-debug.apk');
  if (fs.existsSync(p1)) return p1;
  if (fs.existsSync(p2)) return p2;
  return p3;
};

const getReleaseApkPath = () => {
  const p1 = path.join(__dirname, 'build-outputs/app-release.apk');
  const p2 = path.join(__dirname, '.build-outputs/app-release.apk');
  const p3 = path.join(__dirname, 'app/build/outputs/apk/release/app-release.apk');
  if (fs.existsSync(p1)) return p1;
  if (fs.existsSync(p2)) return p2;
  return p3;
};

const MIME_TYPES = {
  '.html': 'text/html; charset=utf-8',
  '.css': 'text/css',
  '.js': 'text/javascript',
  '.json': 'application/json',
  '.apk': 'application/vnd.android.package-archive',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.svg': 'image/svg+xml',
};

const requestHandler = (req, res) => {
  const url = req.url || '/';
  const DEBUG_APK_PATH = getDebugApkPath();
  const RELEASE_APK_PATH = getReleaseApkPath();
  
  // Set CORS headers for any external embedding
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type');

  if (req.method === 'OPTIONS') {
    res.writeHead(200);
    res.end();
    return;
  }

  // Handle Cloud Run health checks
  if (url === '/healthz' || url === '/health' || url === '/_health') {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ status: 'ok', time: new Date().toISOString() }));
    return;
  }

  // Handle APK Downloads
  if (url === '/download/debug' || url === '/app-debug.apk') {
    if (fs.existsSync(DEBUG_APK_PATH)) {
      const stat = fs.statSync(DEBUG_APK_PATH);
      res.writeHead(200, {
        'Content-Type': MIME_TYPES['.apk'],
        'Content-Length': stat.size,
        'Content-Disposition': 'attachment; filename="KosherM36Manager-Debug.apk"',
      });
      const readStream = fs.createReadStream(DEBUG_APK_PATH);
      readStream.pipe(res);
    } else {
      res.writeHead(404, { 'Content-Type': 'text/html; charset=utf-8' });
      res.end('<h1>גרסת פיתוח לא נמצאה</h1><p>אנא הרץ את תהליך הבנייה ראשית.</p>');
    }
    return;
  }

  if (url === '/download/release' || url === '/app-release.apk') {
    if (fs.existsSync(RELEASE_APK_PATH)) {
      const stat = fs.statSync(RELEASE_APK_PATH);
      res.writeHead(200, {
        'Content-Type': MIME_TYPES['.apk'],
        'Content-Length': stat.size,
        'Content-Disposition': 'attachment; filename="KosherM36Manager-Release.apk"',
      });
      const readStream = fs.createReadStream(RELEASE_APK_PATH);
      readStream.pipe(res);
    } else {
      res.writeHead(404, { 'Content-Type': 'text/html; charset=utf-8' });
      res.end('<h1>גרסת ייצור לא נמצאה</h1><p>אנא הרץ את תהליך הבנייה ראשית.</p>');
    }
    return;
  }

  // Handle Status API
  if (url === '/api/status') {
    const debugExists = fs.existsSync(DEBUG_APK_PATH);
    const releaseExists = fs.existsSync(RELEASE_APK_PATH);
    const debugSize = debugExists ? fs.statSync(DEBUG_APK_PATH).size : 0;
    const releaseSize = releaseExists ? fs.statSync(RELEASE_APK_PATH).size : 0;

    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({
      appName: "מנהל נגן m36 כשר",
      status: "online",
      compilation: "success",
      apks: {
        debug: {
          exists: debugExists,
          path: DEBUG_APK_PATH,
          sizeBytes: debugSize,
          sizeMb: (debugSize / (1024 * 1024)).toFixed(2) + ' MB'
        },
        release: {
          exists: releaseExists,
          path: RELEASE_APK_PATH,
          sizeBytes: releaseSize,
          sizeMb: (releaseSize / (1024 * 1024)).toFixed(2) + ' MB'
        }
      }
    }));
    return;
  }

  // Serve primary high-contrast companion landing page
  if (url === '/' || url === '/index.html') {
    const debugExists = fs.existsSync(DEBUG_APK_PATH);
    const releaseExists = fs.existsSync(RELEASE_APK_PATH);
    const debugSizeMb = debugExists ? (fs.statSync(DEBUG_APK_PATH).size / (1024 * 1024)).toFixed(2) : '0';
    const releaseSizeMb = releaseExists ? (fs.statSync(RELEASE_APK_PATH).size / (1024 * 1024)).toFixed(2) : '0';

    const html = `<!DOCTYPE html>
<html lang="he" dir="rtl">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>מנהל מערכת M36 כשר - לוח בקרה</title>
  <script src="https://cdn.tailwindcss.com"></script>
  <link href="https://fonts.googleapis.com/css2?family=Rubik:wght@300;400;500;700;900&family=Space+Grotesk:wght@500;700&display=swap" rel="stylesheet">
  <style>
    body {
      font-family: 'Rubik', sans-serif;
    }
    .space-font {
      font-family: 'Space Grotesk', sans-serif;
    }
  </style>
  <script>
    tailwind.config = {
      theme: {
        extend: {
          colors: {
            kosherTeal: {
              50: '#f0fdfa',
              100: '#ccfbf1',
              500: '#14b8a6',
              600: '#0d9488',
              700: '#0f766e',
              900: '#115e59',
            },
            kosherBlue: {
              50: '#f0f9ff',
              500: '#0ea5e9',
              600: '#0284c7',
              700: '#0369a1',
            }
          }
        }
      }
    }
  </script>
</head>
<body class="bg-slate-900 text-slate-100 min-h-screen relative flex flex-col justify-between overflow-x-hidden">

  <!-- Background Decorative Mesh/Gradients -->
  <div class="absolute top-0 left-1/4 w-96 h-96 bg-kosherTeal-900/20 rounded-full blur-3xl pointer-events-none"></div>
  <div class="absolute bottom-10 right-1/4 w-80 h-80 bg-kosherBlue-700/10 rounded-full blur-3xl pointer-events-none"></div>

  <!-- Header -->
  <header class="border-b border-slate-800 bg-slate-900/80 backdrop-blur-md sticky top-0 z-50">
    <div class="max-w-6xl mx-auto px-4 py-4 flex items-center justify-between">
      <div class="flex items-center gap-3">
        <!-- Brand Icon -->
        <div class="w-10 h-10 rounded-xl bg-gradient-to-tr from-kosherTeal-600 to-kosherBlue-500 flex items-center justify-center shadow-lg shadow-kosherTeal-900/30">
          <svg class="w-6 h-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 11c0 3.517-1.009 6.799-2.753 9.571m-3.44-2.04l.054-.09A13.916 13.916 0 008 11a4 4 0 118 0c0 1.017-.07 2.019-.203 3m-2.118 6.844A21.88 21.88 0 0015.171 17m3.839 1.132c.645-2.266.99-4.659.99-7.132A8 8 0 008 4.07M3 15.364c.64-1.319 1-2.8 1-4.364 0-1.457.39-2.823 1.07-4" />
          </svg>
        </div>
        <div>
          <span class="text-lg font-bold text-slate-100 tracking-wide">מנהל נגן M36 כשר</span>
          <span class="text-xs block text-kosherTeal-500 font-medium">מערכת בקרה וניהול סיסטם</span>
        </div>
      </div>
      <div class="flex items-center gap-4">
        <span class="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-medium bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
          <span class="w-2 h-2 rounded-full bg-emerald-400 animate-pulse"></span>
          שרת פעיל
        </span>
      </div>
    </div>
  </header>

  <!-- Main Content -->
  <main class="max-w-6xl mx-auto px-4 py-12 w-full flex-grow">
    
    <!-- Hero Section -->
    <div class="text-center max-w-3xl mx-auto mb-16">
      <h1 class="text-4xl md:text-5xl font-black text-white leading-tight mb-4 bg-clip-text text-transparent bg-gradient-to-r from-slate-100 via-kosherTeal-100 to-kosherBlue-400">
        אפליקציית ניהול מתקדמת לנגן אנדרואיד כשר
      </h1>
      <p class="text-lg text-slate-400 font-light max-w-2xl mx-auto">
        ברוכים הבאים לפורטל ההורדה והניהול הרשמי לגרסה הייחודית של המנהל המערכתי לנגני <span class="text-kosherTeal-500 font-bold space-font">M36</span>. דרך כאן תוכלו להשיג את קובצי ה-APK המעודכנים ולהתקינם באופן ישיר במכשיר שלכם.
      </p>
    </div>

    <!-- Download Bento Grid -->
    <div class="grid grid-cols-1 md:grid-cols-2 gap-8 mb-16">
      
      <!-- Card 1: Production Version -->
      <div class="bg-gradient-to-b from-slate-800/80 to-slate-900/90 rounded-2xl border border-slate-700/60 p-8 shadow-xl relative overflow-hidden flex flex-col justify-between group hover:border-kosherTeal-500/50 transition-all duration-350">
        <div class="absolute -top-10 -right-10 w-40 h-40 bg-kosherTeal-600/10 rounded-full blur-2xl group-hover:bg-kosherTeal-600/15 transition-all"></div>
        <div>
          <div class="flex items-center justify-between mb-6">
            <span class="bg-kosherTeal-500/10 text-kosherTeal-400 border border-kosherTeal-500/20 text-xs font-bold px-3 py-1 rounded-full">גרסה יציבה</span>
            <svg class="w-8 h-8 text-kosherTeal-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
            </svg>
          </div>
          <h2 class="text-2xl font-bold text-white mb-2">גרסת ייצור (Release APK)</h2>
          <p class="text-slate-400 text-sm leading-relaxed mb-6">
            גרסה אופטימלית ומוכנה להפעלה ללא צורך בכלי פיתוח. גרסה זו יציבה יותר ומיועדת לשימוש הצרכני השוטף בנגן הכשר. חתומה בחתימת ההפעלה הסטנדרטית של המכשיר.
          </p>
          <div class="flex flex-wrap gap-4 text-xs text-slate-500 mb-6 bg-slate-950/40 p-3 rounded-lg border border-slate-800/80">
            <div><span class="text-slate-400 font-bold">גודל קובץ:</span> ${releaseExists ? releaseSizeMb : '---'} MB</div>
            <div>•</div>
            <div><span class="text-slate-400 font-bold">סוג קובץ:</span> APK</div>
            <div>•</div>
            <div><span class="text-slate-400 font-bold">גרסה:</span> stable_1.0.0</div>
          </div>
        </div>
        <div>
          ${releaseExists ? `
            <a href="/download/release" class="w-full flex items-center justify-center gap-2 bg-gradient-to-r from-kosherTeal-600 to-kosherTeal-500 hover:from-kosherTeal-700 hover:to-kosherTeal-600 text-white font-bold py-3.5 px-6 rounded-xl shadow-lg shadow-kosherTeal-900/35 transition-all text-center">
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"></path></svg>
              הורד גרסת ייצור יציבה
            </a>
          ` : `
            <button disabled class="w-full bg-slate-800 text-slate-500 border border-slate-700/50 font-bold py-3.5 px-6 rounded-xl cursor-not-allowed text-center">
              גרסת ייצור ממתינה לבנייה
            </button>
          `}
        </div>
      </div>

      <!-- Card 2: Debug Version -->
      <div class="bg-gradient-to-b from-slate-800/80 to-slate-900/90 rounded-2xl border border-slate-700/60 p-8 shadow-xl relative overflow-hidden flex flex-col justify-between group hover:border-kosherBlue-500/50 transition-all duration-350">
        <div class="absolute -top-10 -right-10 w-40 h-40 bg-kosherBlue-600/10 rounded-full blur-2xl group-hover:bg-kosherBlue-600/15 transition-all"></div>
        <div>
          <div class="flex items-center justify-between mb-6">
            <span class="bg-kosherBlue-500/10 text-kosherBlue-400 border border-kosherBlue-500/20 text-xs font-bold px-3 py-1 rounded-full">פיתוח וניפוי שגיאות</span>
            <svg class="w-8 h-8 text-kosherBlue-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M10 20l4-16m4 4l4 4-4 4M6 16l-4-4 4-4" />
            </svg>
          </div>
          <h2 class="text-2xl font-bold text-white mb-2">גרסת פיתוח (Debug APK)</h2>
          <p class="text-slate-400 text-sm leading-relaxed mb-6">
            כוללת כלי ניפוי שגיאות נרחבים ואפשרות סימולציה מורחבת (Simulation Mode) לבדיקת שינויים במדדי המכשיר גם על גבי התקנים שאינם בעלי שרשיי מערכת (non-rooted) ובאמולטורים.
          </p>
          <div class="flex flex-wrap gap-4 text-xs text-slate-500 mb-6 bg-slate-950/40 p-3 rounded-lg border border-slate-800/80">
            <div><span class="text-slate-400 font-bold">גודל קובץ:</span> ${debugExists ? debugSizeMb : '---'} MB</div>
            <div>•</div>
            <div><span class="text-slate-400 font-bold">סוג קובץ:</span> APK</div>
            <div>•</div>
            <div><span class="text-slate-400 font-bold">גרסה:</span> dev_1.0.0</div>
          </div>
        </div>
        <div>
          ${debugExists ? `
            <a href="/download/debug" class="w-full flex items-center justify-center gap-2 bg-gradient-to-r from-kosherBlue-600 to-kosherBlue-500 hover:from-kosherBlue-700 hover:to-kosherBlue-600 text-white font-bold py-3.5 px-6 rounded-xl shadow-lg shadow-kosherBlue-900/35 transition-all text-center">
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4"></path></svg>
              הורד גרסת פיתוח (Debug)
            </a>
          ` : `
            <button disabled class="w-full bg-slate-800 text-slate-500 border border-slate-700/50 font-bold py-3.5 px-6 rounded-xl cursor-not-allowed text-center">
              גרסת פיתוח ממתינה לבנייה
            </button>
          `}
        </div>
      </div>

    </div>

    <!-- Features Overview -->
    <div class="bg-slate-850 rounded-2xl border border-slate-800 p-8 md:p-10 mb-16">
      <h3 class="text-2xl font-bold text-white mb-8 text-center md:text-right">תצורות ומדדי מפתח באפליקציה</h3>
      
      <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
        
        <div class="bg-slate-900/50 p-6 rounded-xl border border-slate-800/50">
          <div class="w-10 h-10 rounded-lg bg-kosherTeal-500/10 flex items-center justify-center mb-4">
            <svg class="w-6 h-6 text-kosherTeal-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
            </svg>
          </div>
          <h4 class="text-lg font-bold text-white mb-2">מערכת קוד אבטחה</h4>
          <p class="text-slate-400 text-sm leading-relaxed">
            נעילת קוד אבטחה מובנית להגנה על תפריטי הניהול המתקדמים (Version Management) כדי למנוע שינויים לא רצויים בהגדרות הנגן.
          </p>
        </div>

        <div class="bg-slate-900/50 p-6 rounded-xl border border-slate-800/50">
          <div class="w-10 h-10 rounded-lg bg-kosherBlue-500/10 flex items-center justify-center mb-4">
            <svg class="w-6 h-6 text-kosherBlue-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11.93 5H1.5A1.5 1.5 0 000 6.5v11A1.5 1.5 0 001.5 19h10.43m-1.02-14h11.59A1.5 1.5 0 0124 6.5v11a1.5 1.5 0 01-1.5 1.5H10.91M11.93 5a3 3 0 010 6m0-6a3 3 0 000 6m0 8a3 3 0 010-6m0 6a3 3 0 000-6" />
            </svg>
          </div>
          <h4 class="text-lg font-bold text-white mb-2">מצב סימולציה מהיר</h4>
          <p class="text-slate-400 text-sm leading-relaxed">
            מאפשר תפעול ובדיקת התנהגות מלאה של האפליקציה גם בסביבת בדיקות מרוחקת או באמולטורים אינם מחזיקי הרשאות Root.
          </p>
        </div>

        <div class="bg-slate-900/50 p-6 rounded-xl border border-slate-800/50">
          <div class="w-10 h-10 rounded-lg bg-purple-500/10 flex items-center justify-center mb-4">
            <svg class="w-6 h-6 text-purple-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 5h12M9 3v2m1.048 9.516a10.005 10.005 0 01-3.048-2.616m-3.048 2.616a10.005 10.005 0 003.048-2.616m0 0A10.005 10.005 0 019 11M3 11h18M12 11a9.967 9.967 0 01-1.5 3.516" />
            </svg>
          </div>
          <h4 class="text-lg font-bold text-white mb-2">תמיכה עברית ויידיש</h4>
          <p class="text-slate-400 text-sm leading-relaxed">
            עיצוב מיושר באופן מלא מימין לשמאל (RTL) עם התאמה טבעית לשפות עברית, יידיש ואנגלית עם מערך גופנים מובנה ומושלם.
          </p>
        </div>

      </div>
    </div>

    <!-- Active API Probe / Live Data -->
    <div class="max-w-md mx-auto text-center bg-slate-950/30 p-4 rounded-xl border border-slate-800/60 shadow-inner">
      <div class="text-xs text-slate-500 flex items-center justify-center gap-2">
        <span class="w-2 h-2 rounded-full bg-cyan-400 animate-ping"></span>
        <span class="font-mono text-slate-400" id="api-status-label">טוען מדדי שרת בלייב...</span>
      </div>
    </div>

  </main>

  <!-- Footer -->
  <footer class="border-t border-slate-805/90 bg-slate-950/60 py-6 text-center text-xs text-slate-500">
    <div class="max-w-6xl mx-auto px-4 flex flex-col sm:flex-row items-center justify-between gap-4">
      <p>כל הזכויות שמורות למנהל נגן M36 כשר © 2026</p>
      <div class="flex gap-4">
        <a href="https://ai.studio/build" class="hover:text-kosherTeal-400 transition" target="_blank">Google AI Studio Build</a>
        <span>•</span>
        <span class="font-mono">v1.2.0</span>
      </div>
    </div>
  </footer>

  <script>
    // Live update of server properties
    fetch('/api/status')
      .then(r => r.json())
      .then(data => {
        const label = document.getElementById('api-status-label');
        if (data && data.status === 'online') {
          label.innerHTML = "מערכת תקינה • קובץ ייצור: " + 
            (data.apks.release.exists ? (data.apks.release.sizeMb) : "לא זוהה") + 
            " • קובץ פיתוח: " + 
            (data.apks.debug.exists ? (data.apks.debug.sizeMb) : "לא זוהה");
        }
      })
      .catch(() => {
        document.getElementById('api-status-label').innerText = "לא ניתן להתחבר לניטור השרת";
      })
  </script>
</body>
</html>`;

    res.writeHead(200, { 'Content-Type': MIME_TYPES['.html'] });
    res.end(html);
    return;
  }

  // Fallback for missing resources
  res.writeHead(404, { 'Content-Type': 'text/plain; charset=utf-8' });
  res.end('משאב לא נמצא');
};

// Start primary server on port 3000 (required for AI Studio dev reverse proxy)
const devServer = http.createServer(requestHandler);
devServer.listen(DEV_PORT, '0.0.0.0', () => {
  console.log(`Server is listening on port ${DEV_PORT}`);
});

// Start secondary listener on Cloud Run's designated PORT (e.g. 8080 in production)
if (CLOUD_RUN_PORT && CLOUD_RUN_PORT !== DEV_PORT) {
  const prodServer = http.createServer(requestHandler);
  prodServer.on('error', (err) => {
    if (err.code === 'EADDRINUSE') {
      console.log(`Port ${CLOUD_RUN_PORT} is in use (dev proxy active); serving on port ${DEV_PORT}`);
    } else {
      console.error(`Port ${CLOUD_RUN_PORT} error:`, err.message);
    }
  });
  prodServer.listen(CLOUD_RUN_PORT, '0.0.0.0', () => {
    console.log(`Cloud Run server is listening on port ${CLOUD_RUN_PORT}`);
  });
}
