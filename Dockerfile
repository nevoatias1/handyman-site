# syntax=docker/dockerfile:1
FROM node:22-bookworm-slim AS frontend
WORKDIR /build/frontend
RUN apt-get update && apt-get install -y --no-install-recommends fonts-dejavu-core && rm -rf /var/lib/apt/lists/*
COPY frontend/package*.json ./
RUN npm ci
COPY frontend/ ./
ARG VITE_SITE_URL
RUN VITE_SITE_URL="$VITE_SITE_URL" node scripts/require-site-url.mjs \
 && VITE_SITE_URL="$VITE_SITE_URL" npm run build \
 && VITE_SITE_URL="$VITE_SITE_URL" npm run verify:seo \
 && npm run lint -- --deny-warnings

FROM maven:3.9.11-eclipse-temurin-21 AS backend
WORKDIR /build/backend
COPY backend/pom.xml ./
COPY backend/src ./src
COPY --from=frontend /build/frontend/dist/ ./src/main/resources/static/
RUN mvn -B verify

FROM eclipse-temurin:21-jre-alpine AS runtime
RUN addgroup -S app && adduser -S -G app -u 10001 app
WORKDIR /app
COPY --from=backend --chown=app:app /build/backend/target/handyman-backend-0.0.1-SNAPSHOT.jar /app/app.jar
ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=50.0 -XX:InitialRAMPercentage=20.0 -XX:MaxMetaspaceSize=128m -XX:ReservedCodeCacheSize=32m -Xss512k -XX:+ExitOnOutOfMemoryError"
USER app
EXPOSE 10000
ENTRYPOINT ["java","-jar","/app/app.jar"]
