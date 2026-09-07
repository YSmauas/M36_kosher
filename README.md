# Remix מנהל נגן M36 כשר 📱

אפליקציית ניהול והתאמה מתקדמת למערכת ההפעלה של נגני אנדרואיד כשרים (M36 ומכשירים מקבילים). האפליקציה מאפשרת שליטה מדויקת, מאובטחת ומודולרית על רכיבי הסיסטם, תוך מעבר קל בין פרופילי כשרות, ניהול יכולות וידאו, התקנת חבילות ושירותי גוגל.

---

## 🌟 תכונות עיקריות

- 🔒 **אבטחה מוגברת וקוד גישה (Passcode Protection)**
  - נעילת מסך ניהול הגרסאות באמצעות קוד סודי אישי.
  - אימות קוד חוזר בעת שמירת שינויים למניעת שינוי תצורה לא מורשה.

- 🎛️ **פרופילי כשרות מוגדרים מראש (Presets)**
  - **פרופיל 1 – מלא:** כולל נגני וידאו ואפשרות להתקנת אפליקציות.
  - **פרופיל 2 – וידאו ללא התקנות:** צפייה בסרטונים בלבד, עם חסימה מוחלטת של התקנת קובצי APK חיצוניים.
  - **פרופיל 3 – כשר מהודר:** ללא וידאו וללא התקנות כלל (סגירה הרמטית).
  - **התאמה אישית (Custom):** שליטה פרטנית על כל רכיב ומודול מערכת בנפרד.

- ⚙️ **ניהול רכיבי מערכת (Root Level System Management)**
  - **חסימת/פתיחת התקנות:** החלפת מנהל החבילות (`GooglePackageInstaller`) בין גרסה מאפשרת לגרסה חוסמת וניהול דגל `installs_allowed.flag`.
  - **שליטה בווידאו:** שינוי שמות קבצי מערכת (`.apk` ⇄ `.apkr`) עבור יישומי וידאו ב-`/system/preinstall/` ו-FilesGoogle.
  - **ניהול שירותי Google:** הפעלה או נטרול של `GmsCore`.

- 🧪 **מצב סימולציה מובנה (Simulation Mode)**
  - מאפשר בדיקה מלאה של כלל הממשק והפונקציות גם במכשירי פיתוח ללא Root או באמולטור, מבלי לסכן קובצי מערכת.

- 🎨 **ממשק משתמש מודרני בעברית (RTL)**
  - נבנה ב-**Jetpack Compose** ו-**Material Design 3**.
  - תמיכה בערכת נושא בהירה, כהה או לפי הגדרות המערכת.
  - ממשק נוח, מותאם מסכי מגע קטנים וגדולים של נגנים.

- 🌐 **שרת ליווי מקוון (Web Companion)**
  - שרת Node.js קל-משקל המשולב בפרויקט, מאפשר צפייה בסטטוס המערכת והורדה נוחה של קובצי ה-APK שקומפלו ישירות מהדפדפן.

---

## 📂 מיפוי קובצי מערכת (System Mapping)

האפליקציה מבצעת שינויים בקובצי המערכת הבאים (במכשיר בעל הרשאות Root):

| רכיב | נתיב מקור / פעיל | נתיב מנוטרל | תיאור |
| :--- | :--- | :--- | :--- |
| **נגן וידאו 1** | `/system/preinstall/app5.apk` | `/system/preinstall/app5.apkr` | אפליקציית וידאו |
| **נגן וידאו 2** | `/system/preinstall/app4.apk` | `/system/preinstall/app4.apkr` | אפליקציית וידאו |
| **נגן וידאו 3** | `/system/preinstall/app2.apk` | `/system/preinstall/app2.apkr` | אפליקציית מדיה |
| **סייר קבצים** | `/system/product/priv-app/FilesGoogle/FilesGoogle.apk` | `.../FilesGoogle.apkr` | מנהל הקבצים של גוגל |
| **Google Play Services** | `/system/product/priv-app/GmsCore/GmsCore.apk` | `.../GmsCore.apkr` | שירותי ליבה של גוגל |
| **מתקין החבילות** | `/system/priv-app/GooglePackageInstaller/GooglePackageInstaller.apk` | החלפה בין גרסאות מ-`/system/apps/` | מניעת/אישור התקנת APK |
| **דגל התקנות** | `/system/priv-app/GooglePackageInstaller/installs_allowed.flag` | מחיקה בעת חסימה | אינדיקציית הרשאות |

---

## 🛠️ דרישות סביבה וטכנולוגיות

- **מערכת הפעלה למכשיר היעד:** Android 7.0 (API 24) ומעלה (מותאם במיוחד ל-Android 11-14 בנגני M36).
- **הרשאות:** נדרש Root (`su`) עבור החלת שינויים בפועל (עבור בדיקות קיים מצב סימולציה).
- **Kotlin:** 2.2+
- **Compose BOM:** 2024.09+
- **Gradle:** 9.3+ (Android Gradle Plugin 9.1+)
- **Java:** JDK 21 (Eclipse Temurin / OpenJDK)
- **Node.js:** 18+ (עבור שרת ההפצה והדשבורד)

---

## 🚀 הוראות בנייה והרצה (Build & Run)

### 1. שיבוט הפרויקט
```bash
git clone https://github.com/USERNAME/kosher-m36-manager.git
cd kosher-m36-manager
```

### 2. בניית קובצי APK באמצעות Gradle
ודא שמשתנה הסביבה `JAVA_HOME` מפנה ל-Java 21, והרץ:

- **בניית גרסת Debug:**
  ```bash
  ./gradlew assembleDebug
  ```
  הקובץ ייווצר בנתיב: `app/build/outputs/apk/debug/app-debug.apk`

- **בניית גרסת Release:**
  ```bash
  ./gradlew assembleRelease
  ```
  הקובץ ייווצר בנתיב: `app/build/outputs/apk/release/app-release.apk`

- **הרצת בדיקות יחידה:**
  ```bash
  ./gradlew testDebugUnitTest
  ```

### 3. העתקת קובצי ה-APK לתיקיית הפלט והפעלת השרת המקומי
```bash
# העתקת ה-APKs לתיקיית build-outputs
node copy-apks.js

# הפעלת שרת הדשבורד המקומי
npm start
```
השרת יעלה בכתובת: `http://localhost:3000` (או לפי הפורט המוגדר במשתנה `PORT`).

---

## 🤖 אוטומציה ו-CI/CD ב-GitHub Actions

הפרויקט כולל תהליך עבודה מובנה בקובץ `.github/workflows/build.yml`:
1. בכל `push` או `pull_request` לענפים `main` או `master`:
   - הגדרת סביבת Java 21 ו-Gradle Cache.
   - הרצת כל בדיקות היחידה (`testDebugUnitTest`).
   - קימפול אוטומטי של גרסאות Debug ו-Release.
   - העלאת קובצי ה-APK המוכנים כ-Artifacts הניתנים להורדה ישירות מלשונית ה-Actions ב-GitHub.
2. בכל יצירת תגית גרסה (למשל `git tag v1.0.0 && git push --tags`):
   - יצירת **GitHub Release** אוטומטי עם צירוף קובצי ה-APK להורדה ישירה עבור המשתמשים.

---

## 📄 רישיון ושימוש
הפרויקט מיועד למטרות לימודיות, תחזוקה והתאמת נגנים אישיים. כל שינוי בקובצי מערכת באחריות המשתמש בלבד.
