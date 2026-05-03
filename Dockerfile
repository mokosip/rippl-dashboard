# Stage 1: Build frontend
FROM node:22-alpine AS frontend
WORKDIR /app/frontend
COPY frontend/package*.json ./
RUN npm ci
COPY frontend/ .
RUN npm run build

# Stage 2: Build backend
FROM gradle:9.5-jdk25 AS backend
WORKDIR /app
COPY settings.gradle.kts .
COPY backend/ backend/
COPY --from=frontend /app/frontend/dist backend/src/main/resources/static/
WORKDIR /app/backend
RUN gradle bootJar --no-daemon -x test

# Stage 3: Runtime
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY --from=backend /app/backend/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
