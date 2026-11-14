# Handyman site scaffold

Spring Boot 3 / Java 21 / Maven backend and React / Vite / TypeScript frontend with React Router. The UI defaults to Hebrew and RTL. This phase includes layout placeholders and a health check only.

## Prerequisites

- JDK 21 with JAVA_HOME pointing to the JDK.
- Node.js 22.12+ (or another current Vite-supported release) and npm.
- Internet access for the initial Maven and npm dependency downloads.
- PostgreSQL is needed only for the prod profile.

## Run locally

Open two terminals from this project directory.

Backend (PowerShell):

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

On macOS/Linux use `./mvnw spring-boot:run` (run `chmod +x mvnw` once if needed). With Maven installed, `mvn spring-boot:run` also works.

The default profile is `dev`: an in-memory H2 database, no external database setup, and port **8080**. H2 data is discarded when the process stops. `GET http://localhost:8080/api/health` returns `{"status":"ok"}`; it reports application availability, not database readiness.

Frontend:

```powershell
cd frontend
npm install
npm run dev
```

On Windows, use `npm.cmd` if PowerShell blocks `npm.ps1`.

Visit **http://localhost:5173**. The Home page requests `/api/health` and displays **מצב השרת: ok** after a successful response. Vite proxies `/api` to `http://localhost:8080`; no browser API URL or frontend environment file is required. Loading and connection error states are also shown.

Vite uses a strict port so its origin stays `http://localhost:5173`. The backend allows cross-origin API GET requests only from that origin in `dev`. No cross-origin access is configured for `prod`.

## Verify

```powershell
cd backend
.\mvnw.cmd verify
# Equivalent with Maven installed: mvn verify
cd ../frontend
npm run build
npm run lint
```

Backend integration tests load the dev application with H2 and check the health JSON, allowed Vite CORS preflight, rejected origins, and rejected methods. To verify the live proxy with both servers running:

```powershell
Invoke-RestMethod http://localhost:5173/api/health
```

Open the Home page in a browser to verify the rendered status and Hebrew RTL layout. `npm run preview` serves only the built frontend; the API dev proxy belongs to `npm run dev`.

## Production database profile

Set these environment variables in the backend process environment:

- `SPRING_PROFILES_ACTIVE=prod`
- `DB_URL`: a PostgreSQL JDBC URL, for example `jdbc:postgresql://localhost:5432/handyman`
- `DB_USERNAME`: the database user
- `DB_PASSWORD`: the database password

Then run `./mvnw spring-boot:run`, or build with `./mvnw verify` and run `java -jar target/handyman-backend-0.0.1-SNAPSHOT.jar`. Use `.\mvnw.cmd` on Windows. Supply environment values through your shell or runtime secret manager; Spring Boot does not automatically load .env files. No production credentials are stored in this project. Production schema handling uses `validate`; migrations can be introduced when entities are added.

For a future production frontend, serve the frontend and `/api` from the same origin through a reverse proxy. Deployment configuration is outside this scaffold.

## Structure

```text
handyman-site/
├── .gitignore
├── README.md
├── backend/
│   ├── .mvn/wrapper/maven-wrapper.properties
│   ├── mvnw / mvnw.cmd
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/com/handyman/
│       │   │   ├── HandymanApplication.java
│       │   │   ├── config/
│       │   │   ├── controller/
│       │   │   ├── dto/
│       │   │   ├── model/
│       │   │   ├── repository/
│       │   │   └── service/
│       │   └── resources/application.yml
│       └── test/java/com/handyman/HealthIntegrationTest.java
└── frontend/
    ├── index.html
    ├── package.json / package-lock.json
    ├── .oxlintrc.json
    ├── tsconfig*.json
    ├── vite.config.ts
    └── src/
        ├── App.tsx
        ├── main.tsx
        ├── index.css
        ├── components/Layout.tsx
        └── pages/Home.tsx
```

The model and repository packages contain documentation placeholders. The public homepage and single-admin authentication are implemented; gallery work remains for a later phase.

## Local development login

There are no built-in dev credentials. Run the backend from the `backend/` directory. With the `dev` profile (the default), it imports `backend/config/application-dev-local.yml` if present. The import is disabled whenever `prod` is active, including `dev,prod`.

Generate a BCrypt hash using the interactive helper documented below, then create this local file yourself:

```yaml
local:
  admin:
    username: '<your local username>'
    password-hash: '<your generated BCrypt hash>'
```

Store only the BCrypt hash, never the plaintext password. This file is gitignored; do not stage it. Check with `git check-ignore config/application-dev-local.yml` and `git diff --cached --name-only` from `backend/`. Restart the backend after changing it. Open `http://localhost:5173/admin/login` with both servers running.

`ADMIN_USERNAME` and `ADMIN_PASSWORD_HASH` override local values in dev. Missing or invalid credentials fail startup. Production reads these credentials strictly from environment variables; the local file is never a production fallback.

If login fails after changing the file, restart the backend from `backend/` and confirm the active server is the rebuilt project. A server that was already running will retain its previously loaded credentials.
## Single-admin authentication

Admin pages are available directly at `/admin/login` and `/admin`, with no public navigation links. They set a robots noindex meta tag; this is an indexing hint, while backend role checks enforce access.

Credentials are configured explicitly as described above. Production requires environment variables even if the dev profile is also active. No users table or registration exists.

Generate a BCrypt hash locally from an interactive terminal. The helper lives in test sources and is excluded from the production JAR. It prompts with hidden input; never supply a password in a command-line argument:

```powershell
cd backend
.\mvnw.cmd test-compile dependency:build-classpath '-Dmdep.outputFile=target/classpath.txt'
$dependencyClasspath = Get-Content target/classpath.txt -Raw
java -cp "target/test-classes;$dependencyClasspath" com.handyman.config.PasswordHash
```

On macOS/Linux:

```sh
./mvnw test-compile dependency:build-classpath -Dmdep.outputFile=target/classpath.txt
java -cp "target/test-classes:$(cat target/classpath.txt)" com.handyman.config.PasswordHash
```

Store the generated hash in your runtime secret manager as `ADMIN_PASSWORD_HASH`, and set `ADMIN_USERNAME`. For local PowerShell, assign the hash in single quotes so its dollar signs remain literal:

```powershell
$env:ADMIN_USERNAME = 'your-admin-name'
$env:ADMIN_PASSWORD_HASH = '<paste the generated BCrypt hash>'
$env:SPRING_PROFILES_ACTIVE = 'prod'
# Also provide DB_URL, DB_USERNAME, DB_PASSWORD as documented above.
.\mvnw.cmd spring-boot:run
```

Do not place credentials in tracked files. The helper prints the newly generated hash intentionally; authentication code does not log passwords or hashes.

Security behavior:

- JSON login at POST `/api/auth/login`, current username at GET `/api/auth/me`, POST logout at `/api/auth/logout`, and ADMIN-only GET `/api/admin/ping`.
- GET `/api/auth/csrf` issues the CSRF cookie and returns its token. The API helper sends credentials and an `X-XSRF-TOKEN` header on state-changing requests, fetching a fresh token first. Login and logout require CSRF too. Tokens are cleared on login/logout.
- Session cookies are HttpOnly, SameSite=Strict, cookie-only (no URL session IDs), and Secure in prod. Production requires HTTPS and a same-origin frontend/API. Sessions expire after 30 minutes of inactivity. Login changes any existing session ID; logout invalidates the session.
- The CSRF cookie is readable by JavaScript, SameSite=Strict, and Secure in prod. It is separate from the HttpOnly session cookie.
- Only the Vite dev origin has CORS access: GET for public APIs, and GET/POST plus credentials and the CSRF header for auth APIs. Admin requests use the Vite proxy; prod does not enable CORS.
- Five failed credential attempts per connection IP within a rolling 15 minutes are allowed; subsequent attempts receive JSON 429 and Retry-After. Success clears the failure count for that IP. Counts are in memory, per process, and reset on restart. Expired entries are evicted on each login attempt. At the hard cap of 10,000 tracked IPs, new IPs receive 429 until capacity is freed by expiry or a tracked IP logging in successfully; active limits are never evicted to admit new IPs. Forwarded headers are not trusted. A reverse proxy must preserve distinct client IPs through trusted server configuration before deployment.
- Anonymous protected requests return JSON 401, forbidden/invalid CSRF requests JSON 403. There is no HTML login page or redirect.
- Public API GET routes remain public, with admin routes and current-user lookup explicitly protected first. Other unmatched requests are denied.

Verification: `.\mvnw.cmd verify`, `npm run build`, and `npm run lint`. Auth integration tests exercise real CSRF cookies/headers, successful login, wrong passwords, anonymous access, session ID rotation, rate limits, and logout invalidation.
