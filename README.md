# M36 Kosher – אפליקציית ניהול עם ראוטינג מתקדם

אפליקציית Android לניהול, הכוללת שרת Node.js נלווה (Companion Server) שמטרתו לבנות, לארוז ולהגיש את קובץ ה-APK להורדה.

הפרויקט נוצר מתוך התבנית [`google-gemini/aistudio-repository-template`](https://github.com/google-gemini/aistudio-repository-template) ומשלב שימוש ב-Gemini API.

## מבנה הפרויקט

```
M36_kosher/
├── app/                  # קוד האפליקציה (Android / Kotlin, Gradle)
├── assets/.aistudio/     # קבצי תצורה של AI Studio
├── gradle/               # Gradle wrapper
├── server.js             # שרת Node.js שמגיש/מוריד את ה-APK
├── copy-apks.js          # סקריפט להעתקת קבצי APK בנויים
├── package.json          # תלויות והרצת השרת
├── build.gradle.kts      # קונפיגורציית בנייה (Gradle/Kotlin)
├── settings.gradle.kts
├── gradle.properties
├── .env.example          # דוגמת משתני סביבה
└── metadata.json
```

## דרישות מוקדמות

- **Node.js** גרסה 18 ומעלה
- **Java JDK** ו-**Android SDK** (לבניית האפליקציה עם Gradle)
- מפתח **Gemini API** (לצורך קריאות ל-AI)

## התקנה

1. שכפלו את המאגר:
   ```bash
   git clone https://github.com/YSmauas/M36_kosher.git
   cd M36_kosher
   ```
2. התקינו את תלויות ה-Node:
   ```bash
   npm install
   ```
3. הגדירו משתני סביבה על בסיס `.env.example`:
   ```bash
   cp .env.example .env
   ```
   ועדכנו את `GEMINI_API_KEY` במפתח האישי שלכם.

## הרצה

הפעלת שרת ה-Node.js המגיש את ה-APK:
```bash
npm start
```

בניית האפליקציה עם Gradle:
```bash
./gradlew build
```

העתקת קבצי ה-APK שנבנו למיקום ההגשה של השרת:
```bash
npm run copy-apks
```

## סקריפטים זמינים (package.json)

| פקודה              | תיאור                                      |
|---------------------|---------------------------------------------|
| `npm start`         | מפעיל את שרת ה-Node.js                      |
| `npm run copy-apks` | מעתיק את קבצי ה-APK הבנויים                 |
| `npm run build`     | ללא שלב build נפרד עבור שרת ה-Node          |

## טכנולוגיות

- **Android / Kotlin** – לוגיקת האפליקציה, נבנית עם Gradle Kotlin DSL
- **Node.js** – שרת ההגשה וההורדה של קובצי ה-APK
- **Gemini API** – יכולות AI באפליקציה

## תרומה לפרויקט

מוזמנים לפתוח Issues או Pull Requests במאגר.

## רישיון

לא הוגדר רישיון במאגר זה. ניתן להוסיף קובץ `LICENSE` בהתאם לצורך.
