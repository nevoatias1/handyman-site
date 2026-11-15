# הכנה לפריסה: Render + Neon + Cloudinary

הקבצים מכינים פריסה בלבד. לא בוצעה פריסה. אין להכניס סודות אמיתיים ל־Git, לקובצי הפרויקט, ל־Dockerfile או ל־build args.

## 1. יצירת מסד ב־Neon

1. פתחו פרויקט PostgreSQL חדש באזור Frankfurt (אירופה), קרוב לשירות Render.
2. בחלון Connect בחרו **Connection pooling: OFF**. השתמשו בכתובת הישירה, ללא `-pooler` בשם המארח. Hikari באפליקציה מוגבל ל־3 חיבורים.
3. שמרו את שם מסד הנתונים, המשתמש והסיסמה במנהל סודות או ישירות ב־Render.
4. הגדירו את `DB_URL` בפורמט JDBC, בלי משתמש וסיסמה בתוך הכתובת:

```text
jdbc:postgresql://<direct-neon-host>:5432/<database>?sslmode=require
```

5. הגדירו `DB_USERNAME` ו־`DB_PASSWORD` בנפרד. אין להשתמש בכתובת `postgresql://...` ללא הקידומת `jdbc:`.
6. Flyway ירוץ אוטומטית בזמן עליית השירות וייצור את הטבלאות והקטגוריות. השתמשו במסד ריק בפריסה הראשונה. Hibernate יבדוק את הסכמה עם `ddl-auto: validate`; אין baseline אוטומטי.

מקור: [חיבור ל־Neon](https://neon.com/docs/connect/connect-from-any-app).

## 2. Cloudinary

העתיקו מתוך סביבת Cloudinary את כתובת החיבור והגדירו **רק** את `CLOUDINARY_URL` בסביבת השירות. המבנה הוא:

```text
cloudinary://<api-key>:<api-secret>@<cloud-name>
```

זו כתובת סודית. אין להגדיר אותה כמשתנה VITE, כ־build arg או בתוך קובץ. הקובץ המקומי של dev אינו מועתק לתמונת Docker ואינו מיובא ב־prod.

## 3. מנהל ייצור וסיסמה חדשה

בחרו שם משתמש וסיסמת ייצור חדשה וחזקה, למשל סיסמה אקראית ארוכה ממנהל סיסמאות. אין להשתמש בסיסמת הפיתוח. יצרו את ה־BCrypt hash מקומית מהמסוף, בהקלדה נסתרת:

```powershell
cd C:\handyman-site\backend
.\mvnw.cmd test-compile dependency:build-classpath '-Dmdep.outputFile=target/classpath.txt'
$dependencyClasspath = Get-Content target/classpath.txt -Raw
java -cp "target/test-classes;$dependencyClasspath" com.handyman.config.PasswordHash
```

שמרו את הפלט ב־`ADMIN_PASSWORD_HASH` ואת שם המשתמש ב־`ADMIN_USERNAME` ב־Render בלבד. הדביקו את ה־hash כפי שהוא, כולל סימני הדולר וללא מרכאות נוספות. הסיסמה עצמה אינה נדרשת בשרת; אל תכניסו אותה לקובץ.

## 4. יצירת השירות ב־Render

1. כשהמאגר מוכן לפריסה ובאישור נפרד, חברו אותו ל־Render ובחרו New → Blueprint עם `render.yaml`. אפשר לחלופין ליצור Web Service מסוג Docker.
2. הגדירו שירות יחיד, אזור **Frankfurt**, תוכנית **Starter**, מופע אחד. ה־Dockerfile נמצא בשורש המאגר וה־build context הוא `.`.
3. Health Check Path הוא `/api/health`. ה־Blueprint משבית פריסות אוטומטיות; פריסה ראשונית עדיין מתחילה בעת יצירת השירות. אין ליצור את השירות לפני שכל המשתנים מוכנים.
4. רשמו את כתובת HTTPS שהוקצתה לשירות. הגדירו אותה כ־`VITE_SITE_URL`, לדוגמה בפורמט `https://<service>.onrender.com`.
5. Render מעביר משתני סביבה כ־build args בעלי אותו שם. ה־Dockerfile מצהיר **רק** על `VITE_SITE_URL`, שאינו סוד. סודות נשארים משתני זמן ריצה בלבד.

מקורות: [Blueprint](https://render.com/docs/blueprint-spec), [Docker ומשתני סביבה](https://render.com/docs/docker-secrets).

## 5. משתני הסביבה

| משתנה | ערך שיש לספק |
| --- | --- |
| SPRING_PROFILES_ACTIVE | prod (כבר מוגדר ב־Blueprint) |
| VITE_SITE_URL | כתובת HTTPS הציבורית המדויקת; משמשת בזמן build |
| DB_URL | כתובת JDBC ישירה של Neon עם sslmode=require |
| DB_USERNAME | משתמש Neon |
| DB_PASSWORD | סיסמת Neon |
| ADMIN_USERNAME | שם משתמש מנהל ייצור |
| ADMIN_PASSWORD_HASH | BCrypt של סיסמת הייצור החדשה |
| CLOUDINARY_URL | כתובת החיבור הסודית של Cloudinary |

ששת משתני השרת החיוניים נבדקים לפני יצירת beans וחיבור למסד. משתנה חסר או ריק עוצר את העלייה, בלי להדפיס את ערכו. ה־hash וכתובת Cloudinary נבדקים גם מבחינת פורמט. `VITE_SITE_URL` חסר או placeholder עוצר את בניית התמונה.

Render מספק `PORT` אוטומטית; האפליקציה מאזינה ב־`0.0.0.0`, עם ברירת מחדל 10000. אין צורך להגדיר משתנה נוסף לסודות JVM. הדגלים ב־Dockerfile מגבילים heap ל־50% מהזיכרון, metaspace ל־128MB ו־code cache ל־32MB, עם מחסניות קטנות. תקציב זה מיועד למופע 512MB; יש לעקוב אחרי זיכרון בזמן העלאת תמונות.

## 6. פריסה ראשונה ובדיקות

1. הפעילו deploy ידני אחרי אישור. הבנייה מריצה frontend build, בדיקת SEO, lint ו־Maven verify.
2. בדקו שהלוגים מציגים הצלחת Flyway ועליית Spring בפרופיל prod. אל תפעילו debug logging של credentials, HTTP bodies או Cloudinary.
3. פתחו `/api/health` ובדקו `{"status":"ok"}`. זהו מדד זמינות האפליקציה, לא בדיקת Cloudinary.
4. בדקו `/`, קבצי CSS/JS, `/robots.txt` ו־`/sitemap.xml`. בדקו ב־View Source שה־canonical, ה־OG, ה־JSON-LD וה־sitemap משתמשים בכתובת הנכונה.
5. פתחו ישירות את `/admin` ואת `/admin/login` ורעננו את הדף. יש לקבל את אפליקציית React, לא 404. `/api/does-not-exist` חייב להחזיר JSON 404; קובץ חסר לא יחזיר HTML.
6. התחברו עם מנהל הייצור ובדקו `/api/auth/me`, העלאה ומחיקה של תמונת בדיקה והתנתקות.
7. ב־DevTools בדקו cookie של session עם HttpOnly, Secure ו־SameSite=Strict. ודאו שה־POST כולל X-XSRF-TOKEN. בדקו גישה אנונימית ו־POST ללא CSRF.
8. `server.forward-headers-strategy=native` מפעיל את RemoteIpValve של Tomcat. קוד rate limit ממשיך להשתמש רק ב־getRemoteAddr(). ב־Render ודאו שהפרוקסי מספק X-Forwarded-Proto ו־X-Forwarded-For ושהוא נחשב trusted לפי הגדרות Tomcat. אין להרחיב trusted proxies ל־`.*`; אין לחשוף את פורט הקונטיינר ישירות מחוץ לפרוקסי.
9. אין CORS ב־prod; React וה־API מוגשים מאותו origin. sessions וספירת ניסיונות נמצאים בזיכרון ומתאפסים באתחול/פריסה. יש לשמור מופע אחד כל עוד מנגנון זה נשאר כך.

## 7. בדיקת Docker מקומית ללא סודות אמיתיים

```powershell
cd C:\handyman-site
docker build --build-arg VITE_SITE_URL=https://<public-host> -t handyman-site:local-test .
```

בדקו עם PostgreSQL מקומי זמני, רשת Docker ומסד ריק; העבירו ל־container משתני בדיקה באמצעות `docker run -e NAME` מתוך סביבת המסוף. שמרו את הערכים בזיכרון התהליך בלבד, לא בקובץ. למבחן PostgreSQL מקומי ללא TLS אפשר להשתמש ב־JDBC URL של המסד הזמני ללא sslmode; מול Neon תמיד sslmode=require.

הריצו עם `--memory=512m`, מיפוי `127.0.0.1:18080:10000` ופרופיל prod. שם המשתמש והסיסמה לבדיקה צריכים להיות זמניים; Cloudinary יכול להיות כתובת דמה תקינה אם לא מעלים קבצים. ב־HTTP מקומי cookies מסוג Secure לא נשלחים אוטומטית בדפדפן. השתמשו ב־TLS proxy מקומי או בבדיקת HTTP שמדמה X-Forwarded-Proto: https ומחזירה cookies במפורש; אין לכבות Secure ב־prod.

## 8. דומיין אישי בהמשך

1. הוסיפו את הדומיין דרך Settings → Custom Domains ב־Render ופעלו לפי רשומות ה־DNS שמוצגות.
2. חכו לאימות DNS ולתעודת TLS תקינה.
3. עדכנו `VITE_SITE_URL` לכתובת הראשית החדשה ובחרו **rebuild and deploy**. שינוי runtime בלבד לא מעדכן את ה־HTML הסטטי.
4. בדקו שוב canonical/OG, robots/sitemap, cookie והתחברות. הגדירו הפניה מהדומיין הישן לראשי לפי האפשרויות ב־Render.

מקור: [דומיינים אישיים ב־Render](https://render.com/docs/custom-domains).
