# Phase 8 security review — 2026-11-16

Scope: C:\handyman-site source, reachable Git history, resolved dependencies, built React assets, and a disposable production-profile Docker instance on PostgreSQL 18. No live Render/Neon/Cloudinary settings or assets were changed. No migration or schema change was added.

## Results by requested item

| # | Area | Status | Severity | Evidence / disposition |
|---|---|---|---|---|
| 1 | Secrets/config/history | Fixed + open | Medium | Expanded ignore rules for local YAML and audit artifacts. All 10 reachable commits / 136 unique history blobs and 94 tracked paths reviewed with redacted pattern checks; known local BCrypt/Cloudinary values had zero matches in tracked files/history. Old public dev-default password exists in history; no production secret detected. Cannot prove absence of arbitrary unknown secrets. Prod required-variable checks remain in place, no prod dev fallback, no Actuator dependency, H2 console explicitly off globally. |
| 2 | Auth/sessions | OK + open | Medium | ADMIN rule precedes all public rules and covers storage health. BCrypt/DaoAuthenticationProvider includes unknown-user timing mitigation. Session ID rotates, logout invalidates, 30-minute idle timeout, HttpOnly/Secure-in-prod/SameSite=Strict cookies; all mutations require CSRF. Native forwarded headers and getRemoteAddr only remain. Existing tests cover auth/CSRF/logout/rate limits; prior container tests verified proxy IP separation. Actual Render edge trust still needs live verification. Global synchronized BCrypt limiter and process-local state remain potential load/availability concerns, not changed. |
| 3 | Uploads/metadata | Fixed + open | High | File signatures, 10 MiB/file, 10 files, random UUID IDs and no client filename preserved. Incoming transformation now requests force_strip + strip_profile; delivered URLs explicitly strip profiles and retain f_auto/q_auto. Tests assert incoming stripping flags and delivered variant. This proves SDK options, not a real Cloudinary byte-level EXIF test. Existing originals, derivatives/backups and Cloudinary's DigitalSourceType retention require manual treatment; do not claim ALL metadata has been removed. |
| 4 | Input/data/errors | Fixed | Medium | Input DTO string/list limits and bound repository queries reviewed; React renders text without dangerous HTML insertion. Server errors explicitly exclude messages, binding errors, traces and exception names. Generic unexpected responses no longer fall back to default exception rendering. Known upload/error statuses preserved. |
| 5 | Headers/CSP | Fixed + open | Medium | Enforced production CSP: self-hosted JS/CSS, exact JSON-LD SHA-256, Cloudinary images, blob previews, self API; no unsafe-inline/eval, objects/base/frame ancestors blocked. DENY framing, nosniff, no-referrer, restrictive permissions and HTTPS HSTS verified in container. Built JS/CSS and deep links returned 200. Browser connector unavailable; native Edge request timed out, so actual rendering and no-CSP-violation claim remain UNVERIFIED. |
| 6 | Dependencies | Fixed + open | High | npm audit: 0 vulnerabilities. Maven OSV scan: 106 resolved package versions; after compatible fixes, five advisory IDs remain in four artifacts (below). Flyway core/PostgreSQL module both 11.20.3; both migrations succeeded on PostgreSQL 18.7. No risky framework major upgrade performed. |
| 7 | Entity/proxy correctness | Fixed | Medium | Added entity getters/setters and replaced Java field reads/writes in GalleryService and affected integration tests. Remaining c.id text is bound JPQL, not Java proxy access. Caption regression test still passes. No schema changes. |
| 8 | Logging | Fixed + open | Medium | Root/web/security at INFO, SQL/bind logs off, Hikari WARN, request-detail logging off. Cloudinary diagnostics retained with URL/signature/token/password redaction; JDBC URLs scrubbed. Unexpected stack frames retained but exception payloads omitted to prevent customer data leakage; tests verify this. Live historical logs and operator logging overrides were not inspected. |
| 9 | Container/deploy | OK | Low | Docker build succeeds. Runtime uses Java 21 JRE / UID 10001, contains no Maven/javac/Node, no test classes/test libraries or PasswordHash utility. Local config/env/logs/audit artifacts excluded. Production-profile container tested against PostgreSQL 18; temporary containers/network removed. Runtime image/package CVE scan was not performed. |

## Compatible dependency changes

- Flyway core + PostgreSQL module: 11.7.2 → 11.20.3.
- Jackson BOM: 2.21.4 → 2.21.8.
- Tomcat: 10.1.55 → 10.1.62.
- PostgreSQL JDBC: 42.7.11 → 42.7.14.
- Commons Lang: 3.17.0 → 3.18.0.
- Log4j API/BOM: 2.24.3 → 2.25.5.

The remaining OSV findings:

| Artifact | Version | Advisory / upstream severity | Disposition |
|---|---|---|---|
| httpclient5 | 5.5.2 | GHSA-hjcp-jmpx-g3qm — Moderate, content-decoding connection leak | Fix reported in 5.6.3; coordinate transport upgrade with Cloudinary SDK integration testing. |
| httpcore5 | 5.3.6 | GHSA-hf6x-8p5f-cgmf — High, HTTP/1 header resource exhaustion | Fix reported in 5.4.3; minor-line transport upgrade deferred. |
| httpcore5-h2 | 5.3.6 | GHSA-v3jc-474w-2wm6 — High, HPACK header limits | Fix reported in 5.4.3; HTTP/2 dependency is transitive, assess use and upgrade together. |
| spring-webmvc | 6.2.19 | GHSA-j9f9-w8pj-32f8 and GHSA-pc63-qcmh-9cmg — Critical upstream | SSE-fragment and XsltView features are not used by this app. Latest checked public 6.2 line remains 6.2.19; assess supported backport or plan a separately tested framework upgrade. Findings are not silently suppressed. |

This is a dependency/advisory check, not a penetration test or exploitability proof. Maven package names/versions were checked with OSV; no repository, credentials or customer data was sent. The initial scan flagged 10 artifacts; the final scan retained five advisory IDs across four artifacts. Reports under ignored .security/ contain package metadata only and are not application sources.

## Verification

- Maven verify: 63 tests, zero failures.
- npm run build: pass.
- npm run lint -- --deny-warnings: pass, zero warnings.
- npm run verify:seo: pass.
- Docker build: pass (also runs Maven/frontend checks).
- Production-profile container on PostgreSQL 18.7: V1/V2 migrations succeeded.
- HTTP checks: homepage, admin login deep-link, JS/CSS assets and health served; CSP JSON-LD hash verified against served HTML; all requested headers present under simulated HTTPS proxy.
- JAR/container inspection: UID 10001, no build executables, test classes, JUnit/Mockito libraries or password utility.
- Browser validation: incomplete. Browser access host unavailable; native Edge approval timed out. Do not interpret static source/header checks as a clean browser console.
- Metadata validation: mocked Cloudinary option/URL regression tests only; no real uploads, deletions or asset reprocessing.

## Required manual actions

1. **Render:** review current production env values without sharing them; ensure prod only, no debug/trace/SQL/request-body logging overrides, HTTPS-only access and one instance while sessions/rate counters are in memory. Verify remote-IP trust against Render's current proxy chain; never trust arbitrary forwarded headers. Check Secure cookie flags, CSRF, logout, rate limits from distinct clients and every admin operation after a future separately authorized deploy.
2. **Browser:** run the built app with the production CSP, inspect Console/Network for violations, and check the gallery/lightbox, HEIC/blob previews, caption save, WhatsApp form and tel links on phone and desktop. File chooser camera capture is distinct from the disabled getUserMedia permission.
3. **Cloudinary:** inspect a real GPS-tagged test image after upload, both stored original and delivered variants. Reprocess existing originals with incoming stripping and invalidate/delete old derivatives/backups as appropriate, after reviewing destructive effects. Cloudinary force_strip preserves DigitalSourceType: if literally zero metadata is mandatory, choose a separately reviewed re-encoding pipeline/provider-supported removal method. Do not remove provenance claims blindly.
4. **Neon:** confirm direct (non-pooler) JDBC connection, SSL required, narrow database role and backups/retention appropriate to the business. Stronger TLS identity verification / credential rotation requires configuration review; no live change made here.
5. **Dependencies:** schedule Cloudinary/HTTP Components minor upgrades and a supported Spring security patch/upgrade, with real outbound transport integration tests. Repeat audits regularly and scan the final base image with a container vulnerability scanner.
6. **History/credentials:** the old dev-default password is public in reachable history. Rotate it anywhere it was reused; do not reuse the current dev password in prod. No history rewrite performed. Scan limits mean absence of unknown secret formats is not guaranteed.
7. **Logging:** review existing Render/Cloudinary logs and retention for earlier exposures; do not paste secrets/customer data into support chats. Decide retention and cleanup separately.
8. **Load behavior:** consider edge throttling and a non-serial/shared limiter/session store only after testing and a separate behavior-change decision.

## Changed files

- .gitignore, .dockerignore
- backend/pom.xml
- backend/src/main/resources/application.yml
- backend/src/main/java/com/handyman/config/SecurityConfig.java
- backend/src/main/java/com/handyman/config/SecurityHeaders.java (new)
- backend/src/main/java/com/handyman/controller/GalleryErrors.java
- backend/src/main/java/com/handyman/model/Category.java
- backend/src/main/java/com/handyman/model/Photo.java
- backend/src/main/java/com/handyman/service/CloudinaryStorage.java
- backend/src/main/java/com/handyman/service/GalleryService.java
- backend/src/main/java/com/handyman/service/StorageDiagnostics.java
- backend/src/test/java/com/handyman/GalleryIntegrationTest.java
- backend/src/test/java/com/handyman/service/StorageDiagnosticsTest.java
- backend/src/test/java/com/handyman/config/SecurityHeadersTest.java (new)
- SECURITY_REVIEW.md (this report)

No frontend source behavior, DB schema, deployment platform, commit, push or deployment changed.

## Additional scan limitation

Automatic approval review rejected the optional Gitleaks Docker scan because mounting the repository/history would expose it to a third-party image. It was not retried or bypassed. The local redacted history/pattern and known-local-secret comparisons completed independently; an additional approved secret-scanner run remains optional.

## Primary references

- [Cloudinary force_strip](https://cloudinary.com/documentation/transformation_reference#fl_force_strip)
- [Cloudinary strip_profile](https://cloudinary.com/documentation/transformation_reference#fl_strip_profile)
- [Flyway PostgreSQL support](https://documentation.red-gate.com/flyway/reference/database-driver-reference/postgresql-database)
- [OSV vulnerability database](https://osv.dev/)
- [Spring SSE advisory](https://github.com/advisories/GHSA-j9f9-w8pj-32f8)
- [Spring XSLT advisory](https://github.com/advisories/GHSA-pc63-qcmh-9cmg)
