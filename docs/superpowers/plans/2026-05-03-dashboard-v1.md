# rippl Dashboard V1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a standalone analytics dashboard where users sign up, connect the Chrome extension as a data source, and get insights into their AI usage patterns — trends over time, cumulative time saved, and template-based "mirror moments."

**Architecture:** Spring Boot 4.0 (Kotlin) monorepo with React SPA (Vite + TypeScript) served from the same JAR. Postgres database with Flyway migrations. Magic link auth via Resend, JWT session cookies. Package-by-feature backend structure. Extension syncs data via REST API; dashboard computes analytics on demand.

**Tech Stack:** Spring Boot 4.0, Kotlin 2.1, Postgres 16, Flyway, JJWT, Resend HTTP API, React 19, Vite 6, TypeScript 5.7, Recharts, Tailwind CSS 4, Testcontainers, Vitest

---

## File Structure

```
rippl-dashboard/
├── .gitignore
├── settings.gradle.kts
├── backend/
│   ├── build.gradle.kts
│   └── src/
│       ├── main/
│       │   ├── kotlin/app/rippl/
│       │   │   ├── DashboardApplication.kt
│       │   │   ├── auth/
│       │   │   │   ├── AuthController.kt
│       │   │   │   ├── AuthService.kt
│       │   │   │   ├── JwtService.kt
│       │   │   │   ├── JwtAuthFilter.kt
│       │   │   │   ├── SecurityConfig.kt
│       │   │   │   ├── User.kt
│       │   │   │   ├── UserRepository.kt
│       │   │   │   ├── AuthToken.kt
│       │   │   │   └── AuthTokenRepository.kt
│       │   │   ├── sync/
│       │   │   │   ├── SyncController.kt
│       │   │   │   ├── SyncService.kt
│       │   │   │   ├── SyncDtos.kt
│       │   │   │   └── RateLimiter.kt
│       │   │   ├── sessions/
│       │   │   │   ├── Session.kt
│       │   │   │   ├── SessionRepository.kt
│       │   │   │   └── SessionUpsertRepository.kt
│       │   │   ├── trends/
│       │   │   │   ├── TrendsController.kt
│       │   │   │   ├── TrendsService.kt
│       │   │   │   └── TrendsDtos.kt
│       │   │   ├── insights/
│       │   │   │   ├── InsightsController.kt
│       │   │   │   └── InsightsService.kt
│       │   │   ├── collectors/
│       │   │   │   ├── CollectorsController.kt
│       │   │   │   ├── Collector.kt
│       │   │   │   └── CollectorRepository.kt
│       │   │   └── account/
│       │   │       └── AccountController.kt
│       │   └── resources/
│       │       ├── application.yml
│       │       ├── application-dev.yml
│       │       └── db/migration/
│       │           └── V1__initial_schema.sql
│       └── test/
│           └── kotlin/app/rippl/
│               ├── TestcontainersConfig.kt
│               ├── auth/
│               │   ├── JwtServiceTest.kt
│               │   └── AuthControllerTest.kt
│               ├── sync/
│               │   └── SyncControllerTest.kt
│               ├── trends/
│               │   └── TrendsServiceTest.kt
│               ├── insights/
│               │   └── InsightsServiceTest.kt
│               ├── collectors/
│               │   └── CollectorsControllerTest.kt
│               └── account/
│                   └── AccountControllerTest.kt
├── frontend/
│   ├── index.html
│   ├── package.json
│   ├── tsconfig.json
│   ├── tsconfig.node.json
│   ├── vite.config.ts
│   └── src/
│       ├── main.tsx
│       ├── index.css
│       ├── App.tsx
│       ├── types.ts
│       ├── data/
│       │   ├── domains.ts
│       │   └── comparisons.ts
│       ├── api/
│       │   ├── client.ts
│       │   ├── auth.ts
│       │   ├── trends.ts
│       │   ├── insights.ts
│       │   ├── collectors.ts
│       │   └── account.ts
│       ├── hooks/
│       │   └── useAuth.ts
│       ├── context/
│       │   └── AuthContext.tsx
│       ├── components/
│       │   ├── Layout.tsx
│       │   ├── TrendChart.tsx
│       │   ├── TimeSavedCard.tsx
│       │   ├── MirrorMomentCard.tsx
│       │   ├── CollectorCard.tsx
│       │   └── OnboardingChecklist.tsx
│       └── pages/
│           ├── Login.tsx
│           ├── Dashboard.tsx
│           ├── Trends.tsx
│           ├── Mirror.tsx
│           └── Settings.tsx
└── Dockerfile
```

---

## Task 1: Project Scaffolding

**Files:**
- Create: `.gitignore`, `settings.gradle.kts`, `backend/build.gradle.kts`
- Create: `backend/src/main/kotlin/app/rippl/DashboardApplication.kt`
- Create: `backend/src/main/resources/application.yml`, `backend/src/main/resources/application-dev.yml`
- Create: `frontend/` (via Vite scaffold + config)

- [ ] **Step 1: Initialize git repo**

```bash
cd /Users/konschack/Dev/personal/rippl-dashboard
git init
```

- [ ] **Step 2: Create .gitignore**

Create `.gitignore`:

```gitignore
# Gradle
.gradle/
build/
!gradle/wrapper/gradle-wrapper.jar

# IDE
.idea/
*.iml
.vscode/

# OS
.DS_Store

# Frontend
frontend/node_modules/
frontend/dist/

# Environment
.env
.env.local

# Kotlin
*.class

# Test
backend/src/main/resources/static/
```

- [ ] **Step 3: Create Gradle settings and build files**

Create `settings.gradle.kts`:

```kotlin
rootProject.name = "rippl-dashboard"
include("backend")
```

Create `backend/build.gradle.kts`:

```kotlin
plugins {
    kotlin("jvm") version "2.1.0"
    kotlin("plugin.spring") version "2.1.0"
    kotlin("plugin.jpa") version "2.1.0"
    id("org.springframework.boot") version "4.0.0"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "app.rippl"
version = "0.1.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    runtimeOnly("org.postgresql:postgresql")

    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.testcontainers:junit-jupiter")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
```

- [ ] **Step 4: Create Spring Boot application class**

Create `backend/src/main/kotlin/app/rippl/DashboardApplication.kt`:

```kotlin
package app.rippl

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class DashboardApplication

fun main(args: Array<String>) {
    runApplication<DashboardApplication>(*args)
}
```

- [ ] **Step 5: Create application config files**

Create `backend/src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: ${DATABASE_URL:jdbc:postgresql://localhost:5432/rippl}
    username: ${DB_USERNAME:rippl}
    password: ${DB_PASSWORD:rippl}
  jpa:
    hibernate:
      ddl-auto: validate
    open-in-view: false
  flyway:
    enabled: true

server:
  tomcat:
    max-swallow-size: 2MB

app:
  jwt:
    secret: ${JWT_SECRET:dev-secret-min-32-chars-long-for-hmac}
    session-expiry-days: 7
    magic-link-expiry-minutes: 15
  resend:
    api-key: ${RESEND_API_KEY:re_test}
    from: noreply@ripplup.app
  base-url: ${BASE_URL:http://localhost:8080}
  frontend-url: ${FRONTEND_URL:http://localhost:5173}
  extension-id: ${EXTENSION_ID:dev-extension-id}
  rate-limit:
    sync-per-minute: 10
```

Create `backend/src/main/resources/application-dev.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/rippl
    username: rippl
    password: rippl

app:
  base-url: http://localhost:8080
  frontend-url: http://localhost:5173
```

- [ ] **Step 6: Generate Gradle wrapper**

```bash
cd /Users/konschack/Dev/personal/rippl-dashboard
gradle wrapper --gradle-version 8.10
```

- [ ] **Step 7: Verify backend compiles**

```bash
./gradlew :backend:compileKotlin
```

Expected: BUILD SUCCESSFUL (no app startup — no DB yet)

- [ ] **Step 8: Scaffold frontend**

```bash
cd /Users/konschack/Dev/personal/rippl-dashboard
npm create vite@latest frontend -- --template react-ts
cd frontend
npm install
npm install react-router-dom recharts
npm install -D @tailwindcss/vite tailwindcss
```

- [ ] **Step 9: Configure Vite with proxy and Tailwind**

Replace `frontend/vite.config.ts`:

```typescript
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
  build: {
    outDir: 'dist',
  },
})
```

Replace `frontend/src/index.css`:

```css
@import "tailwindcss";
```

- [ ] **Step 10: Verify frontend starts**

```bash
cd frontend && npm run dev
```

Expected: Vite dev server starts on http://localhost:5173

- [ ] **Step 11: Commit**

```bash
git add .gitignore settings.gradle.kts backend/ frontend/ gradlew gradlew.bat gradle/
git commit -m "chore: scaffold Spring Boot + React project"
```

---

## Task 2: Database Schema + Test Infrastructure

**Files:**
- Create: `backend/src/main/resources/db/migration/V1__initial_schema.sql`
- Create: `backend/src/test/kotlin/app/rippl/TestcontainersConfig.kt`
- Test: `backend/src/test/kotlin/app/rippl/SchemaTest.kt`

- [ ] **Step 1: Write migration test**

Create `backend/src/test/kotlin/app/rippl/SchemaTest.kt`:

```kotlin
package app.rippl

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.JdbcTemplate

@SpringBootTest
@Import(TestcontainersConfig::class)
class SchemaTest {

    @Autowired
    lateinit var jdbc: JdbcTemplate

    @Test
    fun `migration creates all tables`() {
        val tables = jdbc.queryForList(
            "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'",
            String::class.java
        )
        assert(tables.containsAll(listOf("users", "auth_tokens", "collectors", "sessions"))) {
            "Missing tables. Found: $tables"
        }
    }

    @Test
    fun `sessions table has correct indexes`() {
        val indexes = jdbc.queryForList(
            "SELECT indexname FROM pg_indexes WHERE tablename = 'sessions'",
            String::class.java
        )
        assert(indexes.contains("idx_sessions_user_date")) { "Missing idx_sessions_user_date" }
        assert(indexes.contains("idx_sessions_user_domain")) { "Missing idx_sessions_user_domain" }
    }
}
```

- [ ] **Step 2: Create Testcontainers config**

Create `backend/src/test/kotlin/app/rippl/TestcontainersConfig.kt`:

```kotlin
package app.rippl

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.testcontainers.containers.PostgreSQLContainer

@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfig {

    @Bean
    @ServiceConnection
    fun postgres(): PostgreSQLContainer<*> =
        PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("rippl_test")
            .withReuse(true)
}
```

- [ ] **Step 3: Run test to verify it fails**

```bash
cd /Users/konschack/Dev/personal/rippl-dashboard
./gradlew :backend:test --tests "app.rippl.SchemaTest"
```

Expected: FAIL — no migration file yet, Security auto-config may block startup. We need a minimal SecurityConfig too. Create a placeholder:

Create `backend/src/main/kotlin/app/rippl/auth/SecurityConfig.kt`:

```kotlin
package app.rippl.auth

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.web.SecurityFilterChain

@Configuration
class SecurityConfig {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain =
        http
            .csrf { it.disable() }
            .authorizeHttpRequests { it.anyRequest().permitAll() }
            .build()
}
```

- [ ] **Step 4: Create Flyway migration**

Create `backend/src/main/resources/db/migration/V1__initial_schema.sql`:

```sql
CREATE TABLE users (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email       TEXT UNIQUE NOT NULL,
    created_at  TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE auth_tokens (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID REFERENCES users(id) ON DELETE CASCADE,
    token_hash  TEXT NOT NULL,
    expires_at  TIMESTAMPTZ NOT NULL,
    used_at     TIMESTAMPTZ
);

CREATE TABLE collectors (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID REFERENCES users(id) ON DELETE CASCADE,
    type        TEXT NOT NULL,
    enabled     BOOLEAN DEFAULT true,
    linked_at   TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE sessions (
    id                        TEXT PRIMARY KEY,
    user_id                   UUID REFERENCES users(id) ON DELETE CASCADE NOT NULL,
    domain                    TEXT NOT NULL,
    started_at                BIGINT NOT NULL,
    ended_at                  BIGINT NOT NULL,
    active_seconds            INT NOT NULL,
    date                      DATE NOT NULL,
    activity_type             TEXT,
    estimated_without_minutes INT,
    time_saved_minutes        INT,
    logged                    BOOLEAN DEFAULT false,
    synced_at                 TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX idx_sessions_user_date ON sessions(user_id, date);
CREATE INDEX idx_sessions_user_domain ON sessions(user_id, domain);
```

- [ ] **Step 5: Run test to verify it passes**

```bash
./gradlew :backend:test --tests "app.rippl.SchemaTest"
```

Expected: PASS — all tables and indexes created

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/resources/db/ backend/src/test/ backend/src/main/kotlin/app/rippl/auth/SecurityConfig.kt
git commit -m "feat: add database schema migration and test infrastructure"
```

---

## Task 3: JWT Service

**Files:**
- Create: `backend/src/main/kotlin/app/rippl/auth/JwtService.kt`
- Test: `backend/src/test/kotlin/app/rippl/auth/JwtServiceTest.kt`

- [ ] **Step 1: Write failing tests**

Create `backend/src/test/kotlin/app/rippl/auth/JwtServiceTest.kt`:

```kotlin
package app.rippl.auth

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

class JwtServiceTest {

    private lateinit var jwtService: JwtService

    @BeforeEach
    fun setup() {
        jwtService = JwtService(
            secret = "test-secret-that-is-at-least-32-characters-long",
            sessionExpiryDays = 7,
            magicLinkExpiryMinutes = 15
        )
    }

    @Test
    fun `generateSessionToken creates valid JWT`() {
        val userId = UUID.randomUUID()
        val token = jwtService.generateSessionToken(userId)
        assertNotNull(token)
        assertTrue(token.split(".").size == 3)
    }

    @Test
    fun `validateSessionToken returns userId for valid token`() {
        val userId = UUID.randomUUID()
        val token = jwtService.generateSessionToken(userId)
        val result = jwtService.validateSessionToken(token)
        assertEquals(userId, result)
    }

    @Test
    fun `validateSessionToken returns null for expired token`() {
        val jwtServiceShortExpiry = JwtService(
            secret = "test-secret-that-is-at-least-32-characters-long",
            sessionExpiryDays = 0,
            magicLinkExpiryMinutes = 0
        )
        val userId = UUID.randomUUID()
        val token = jwtServiceShortExpiry.generateSessionToken(userId)
        Thread.sleep(100)
        val result = jwtServiceShortExpiry.validateSessionToken(token)
        assertNull(result)
    }

    @Test
    fun `validateSessionToken returns null for malformed token`() {
        assertNull(jwtService.validateSessionToken("not.a.jwt"))
        assertNull(jwtService.validateSessionToken(""))
    }

    @Test
    fun `generateMagicLinkToken includes email and jti`() {
        val jti = UUID.randomUUID()
        val token = jwtService.generateMagicLinkToken("test@example.com", jti)
        assertNotNull(token)
    }

    @Test
    fun `validateMagicLinkToken returns claims for valid token`() {
        val jti = UUID.randomUUID()
        val token = jwtService.generateMagicLinkToken("test@example.com", jti)
        val claims = jwtService.validateMagicLinkToken(token)
        assertNotNull(claims)
        assertEquals("test@example.com", claims!!.subject)
        assertEquals(jti.toString(), claims.id)
    }

    @Test
    fun `validateMagicLinkToken returns null for invalid token`() {
        assertNull(jwtService.validateMagicLinkToken("garbage"))
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

```bash
./gradlew :backend:test --tests "app.rippl.auth.JwtServiceTest"
```

Expected: FAIL — `JwtService` class does not exist

- [ ] **Step 3: Implement JwtService**

Create `backend/src/main/kotlin/app/rippl/auth/JwtService.kt`:

```kotlin
package app.rippl.auth

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.Date
import java.util.UUID
import javax.crypto.spec.SecretKeySpec

@Service
class JwtService(
    @Value("\${app.jwt.secret}") private val secret: String,
    @Value("\${app.jwt.session-expiry-days}") private val sessionExpiryDays: Int,
    @Value("\${app.jwt.magic-link-expiry-minutes}") private val magicLinkExpiryMinutes: Int
) {
    private val key get() = SecretKeySpec(secret.toByteArray(), "HmacSHA256")

    fun generateSessionToken(userId: UUID): String =
        Jwts.builder()
            .subject(userId.toString())
            .issuedAt(Date())
            .expiration(Date(System.currentTimeMillis() + sessionExpiryDays * 86_400_000L))
            .signWith(key)
            .compact()

    fun validateSessionToken(token: String): UUID? =
        try {
            val claims = parse(token)
            UUID.fromString(claims.subject)
        } catch (_: Exception) {
            null
        }

    fun generateMagicLinkToken(email: String, jti: UUID): String =
        Jwts.builder()
            .subject(email)
            .id(jti.toString())
            .issuedAt(Date())
            .expiration(Date(System.currentTimeMillis() + magicLinkExpiryMinutes * 60_000L))
            .signWith(key)
            .compact()

    fun validateMagicLinkToken(token: String): Claims? =
        try {
            parse(token)
        } catch (_: Exception) {
            null
        }

    private fun parse(token: String): Claims =
        Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .payload
}
```

- [ ] **Step 4: Run tests to verify they pass**

```bash
./gradlew :backend:test --tests "app.rippl.auth.JwtServiceTest"
```

Expected: ALL PASS

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/kotlin/app/rippl/auth/JwtService.kt backend/src/test/kotlin/app/rippl/auth/JwtServiceTest.kt
git commit -m "feat: add JWT service for session and magic link tokens"
```

---

## Task 4: Auth Flow (Magic Link + Security Filter)

**Files:**
- Create: `backend/src/main/kotlin/app/rippl/auth/User.kt`
- Create: `backend/src/main/kotlin/app/rippl/auth/UserRepository.kt`
- Create: `backend/src/main/kotlin/app/rippl/auth/AuthToken.kt`
- Create: `backend/src/main/kotlin/app/rippl/auth/AuthTokenRepository.kt`
- Create: `backend/src/main/kotlin/app/rippl/auth/AuthService.kt`
- Create: `backend/src/main/kotlin/app/rippl/auth/AuthController.kt`
- Create: `backend/src/main/kotlin/app/rippl/auth/JwtAuthFilter.kt`
- Modify: `backend/src/main/kotlin/app/rippl/auth/SecurityConfig.kt`
- Test: `backend/src/test/kotlin/app/rippl/auth/AuthControllerTest.kt`

- [ ] **Step 1: Create User and AuthToken entities + repositories**

Create `backend/src/main/kotlin/app/rippl/auth/User.kt`:

```kotlin
package app.rippl.auth

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "users")
class User(
    @Column(unique = true, nullable = false)
    var email: String,

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @Column(name = "created_at")
    var createdAt: Instant = Instant.now()
)
```

Create `backend/src/main/kotlin/app/rippl/auth/UserRepository.kt`:

```kotlin
package app.rippl.auth

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface UserRepository : JpaRepository<User, UUID> {
    fun findByEmail(email: String): User?
}
```

Create `backend/src/main/kotlin/app/rippl/auth/AuthToken.kt`:

```kotlin
package app.rippl.auth

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "auth_tokens")
class AuthToken(
    @Column(name = "user_id", nullable = false)
    var userId: UUID,

    @Column(name = "token_hash", nullable = false)
    var tokenHash: String,

    @Column(name = "expires_at", nullable = false)
    var expiresAt: Instant,

    @Column(name = "used_at")
    var usedAt: Instant? = null,

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null
)
```

Create `backend/src/main/kotlin/app/rippl/auth/AuthTokenRepository.kt`:

```kotlin
package app.rippl.auth

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface AuthTokenRepository : JpaRepository<AuthToken, UUID> {
    fun findByTokenHash(tokenHash: String): AuthToken?
}
```

- [ ] **Step 2: Write auth controller integration tests**

Create `backend/src/test/kotlin/app/rippl/auth/AuthControllerTest.kt`:

```kotlin
package app.rippl.auth

import app.rippl.TestcontainersConfig
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfig::class)
class AuthControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var jwtService: JwtService

    @Autowired
    lateinit var userRepository: UserRepository

    @Test
    fun `POST magic-link returns 200 for valid email`() {
        mockMvc.post("/api/auth/magic-link") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"email": "test@example.com"}"""
        }.andExpect {
            status { isOk() }
        }
    }

    @Test
    fun `POST magic-link returns 400 for missing email`() {
        mockMvc.post("/api/auth/magic-link") {
            contentType = MediaType.APPLICATION_JSON
            content = """{}"""
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    fun `GET me returns 401 when not authenticated`() {
        mockMvc.get("/api/auth/me")
            .andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `GET me returns user when authenticated`() {
        val user = userRepository.save(User(email = "auth-test@example.com"))
        val token = jwtService.generateSessionToken(user.id!!)

        mockMvc.get("/api/auth/me") {
            cookie(jakarta.servlet.http.Cookie("session", token))
        }.andExpect {
            status { isOk() }
            jsonPath("$.email") { value("auth-test@example.com") }
        }
    }

    @Test
    fun `POST logout clears session cookie`() {
        val user = userRepository.save(User(email = "logout-test@example.com"))
        val token = jwtService.generateSessionToken(user.id!!)

        mockMvc.post("/api/auth/logout") {
            cookie(jakarta.servlet.http.Cookie("session", token))
        }.andExpect {
            status { isOk() }
            cookie { maxAge("session", 0) }
        }
    }
}
```

- [ ] **Step 3: Run tests to verify they fail**

```bash
./gradlew :backend:test --tests "app.rippl.auth.AuthControllerTest"
```

Expected: FAIL — AuthService, AuthController, JwtAuthFilter not yet created

- [ ] **Step 4: Implement AuthService**

Create `backend/src/main/kotlin/app/rippl/auth/AuthService.kt`:

```kotlin
package app.rippl.auth

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import java.security.MessageDigest
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val authTokenRepository: AuthTokenRepository,
    private val jwtService: JwtService,
    @Value("\${app.resend.api-key}") private val resendApiKey: String,
    @Value("\${app.resend.from}") private val fromEmail: String,
    @Value("\${app.base-url}") private val baseUrl: String,
    @Value("\${app.jwt.magic-link-expiry-minutes}") private val magicLinkExpiryMinutes: Int
) {
    private val restClient = RestClient.create()

    fun sendMagicLink(email: String) {
        val user = userRepository.findByEmail(email)
            ?: userRepository.save(User(email = email))

        val jti = UUID.randomUUID()
        val token = jwtService.generateMagicLinkToken(email, jti)

        authTokenRepository.save(
            AuthToken(
                userId = user.id!!,
                tokenHash = sha256(jti.toString()),
                expiresAt = Instant.now().plus(magicLinkExpiryMinutes.toLong(), ChronoUnit.MINUTES)
            )
        )

        val link = "$baseUrl/api/auth/verify?token=$token"

        restClient.post()
            .uri("https://api.resend.com/emails")
            .header("Authorization", "Bearer $resendApiKey")
            .header("Content-Type", "application/json")
            .body(
                mapOf(
                    "from" to fromEmail,
                    "to" to listOf(email),
                    "subject" to "Sign in to rippl",
                    "html" to """<p>Click <a href="$link">here</a> to sign in to rippl. This link expires in $magicLinkExpiryMinutes minutes.</p>"""
                )
            )
            .retrieve()
            .toBodilessEntity()
    }

    fun verifyMagicLink(token: String): User? {
        val claims = jwtService.validateMagicLinkToken(token) ?: return null
        val jti = claims.id ?: return null

        val authToken = authTokenRepository.findByTokenHash(sha256(jti)) ?: return null
        if (authToken.usedAt != null) return null
        if (authToken.expiresAt.isBefore(Instant.now())) return null

        authToken.usedAt = Instant.now()
        authTokenRepository.save(authToken)

        return userRepository.findById(authToken.userId).orElse(null)
    }

    fun findById(userId: UUID): User? = userRepository.findById(userId).orElse(null)

    private fun sha256(input: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(input.toByteArray())
            .joinToString("") { "%02x".format(it) }
}
```

- [ ] **Step 5: Implement AuthController**

Create `backend/src/main/kotlin/app/rippl/auth/AuthController.kt`:

```kotlin
package app.rippl.auth

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseCookie
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.time.Duration
import java.util.UUID

data class MagicLinkRequest(val email: String?)

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authService: AuthService,
    private val jwtService: JwtService,
    @Value("\${app.frontend-url}") private val frontendUrl: String
) {

    @PostMapping("/magic-link")
    fun sendMagicLink(@RequestBody request: MagicLinkRequest): ResponseEntity<Any> {
        val email = request.email?.takeIf { it.contains("@") }
            ?: return ResponseEntity.badRequest().body(mapOf("error" to "invalid_email"))
        authService.sendMagicLink(email)
        return ResponseEntity.ok(mapOf("sent" to true))
    }

    @GetMapping("/verify")
    fun verify(@RequestParam token: String): ResponseEntity<Void> {
        val user = authService.verifyMagicLink(token)
            ?: return ResponseEntity.status(302)
                .header(HttpHeaders.LOCATION, "$frontendUrl/login?error=invalid_token")
                .build()

        val sessionToken = jwtService.generateSessionToken(user.id!!)
        val cookie = ResponseCookie.from("session", sessionToken)
            .httpOnly(true)
            .path("/")
            .maxAge(Duration.ofDays(7))
            .sameSite("Lax")
            .build()

        return ResponseEntity.status(302)
            .header(HttpHeaders.SET_COOKIE, cookie.toString())
            .header(HttpHeaders.LOCATION, "$frontendUrl/")
            .build()
    }

    @GetMapping("/me")
    fun me(@AuthenticationPrincipal userId: UUID): ResponseEntity<Map<String, Any>> {
        val user = authService.findById(userId)
            ?: return ResponseEntity.status(401).build()
        return ResponseEntity.ok(mapOf("id" to user.id!!, "email" to user.email))
    }

    @PostMapping("/logout")
    fun logout(): ResponseEntity<Void> {
        val cookie = ResponseCookie.from("session", "")
            .httpOnly(true)
            .path("/")
            .maxAge(Duration.ZERO)
            .build()
        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, cookie.toString())
            .build()
    }
}
```

- [ ] **Step 6: Implement JwtAuthFilter**

Create `backend/src/main/kotlin/app/rippl/auth/JwtAuthFilter.kt`:

```kotlin
package app.rippl.auth

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthFilter(private val jwtService: JwtService) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        chain: FilterChain
    ) {
        val token = request.cookies?.find { it.name == "session" }?.value
        if (token != null) {
            val userId = jwtService.validateSessionToken(token)
            if (userId != null) {
                val auth = UsernamePasswordAuthenticationToken(userId, null, emptyList())
                SecurityContextHolder.getContext().authentication = auth
            }
        }
        chain.doFilter(request, response)
    }
}
```

- [ ] **Step 7: Update SecurityConfig**

Replace `backend/src/main/kotlin/app/rippl/auth/SecurityConfig.kt`:

```kotlin
package app.rippl.auth

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
class SecurityConfig(private val jwtAuthFilter: JwtAuthFilter) {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain =
        http
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests {
                it.requestMatchers("/api/auth/magic-link", "/api/auth/verify").permitAll()
                it.requestMatchers("/api/**").authenticated()
                it.anyRequest().permitAll()
            }
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter::class.java)
            .build()
}
```

- [ ] **Step 8: Run tests to verify they pass**

```bash
./gradlew :backend:test --tests "app.rippl.auth.AuthControllerTest"
```

Expected: ALL PASS (magic-link test may fail on Resend call — mock it or use a test profile that skips email sending)

If the Resend call fails in tests, extract the HTTP call into a separate `EmailService` interface and mock it in tests. Quick fix: add `@MockkBean` or `@MockBean` for `RestClient`. Simplest approach — make `AuthService.sendMagicLink` catch and log email errors in dev/test:

If tests fail due to Resend API call, wrap the `restClient` call in a try-catch that logs instead of throwing when `resendApiKey` starts with `re_test`.

- [ ] **Step 9: Commit**

```bash
git add backend/src/main/kotlin/app/rippl/auth/ backend/src/test/kotlin/app/rippl/auth/
git commit -m "feat: add magic link auth flow with JWT sessions"
```

---

## Task 5: Sync Endpoint

**Files:**
- Create: `backend/src/main/kotlin/app/rippl/sessions/Session.kt`
- Create: `backend/src/main/kotlin/app/rippl/sessions/SessionRepository.kt`
- Create: `backend/src/main/kotlin/app/rippl/sessions/SessionUpsertRepository.kt`
- Create: `backend/src/main/kotlin/app/rippl/sync/SyncDtos.kt`
- Create: `backend/src/main/kotlin/app/rippl/sync/SyncService.kt`
- Create: `backend/src/main/kotlin/app/rippl/sync/SyncController.kt`
- Create: `backend/src/main/kotlin/app/rippl/sync/RateLimiter.kt`
- Test: `backend/src/test/kotlin/app/rippl/sync/SyncControllerTest.kt`

- [ ] **Step 1: Create Session entity and DTOs**

Create `backend/src/main/kotlin/app/rippl/sessions/Session.kt`:

```kotlin
package app.rippl.sessions

import jakarta.persistence.*
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(name = "sessions")
class Session(
    @Id
    var id: String,

    @Column(name = "user_id", nullable = false)
    var userId: UUID,

    @Column(nullable = false)
    var domain: String,

    @Column(name = "started_at", nullable = false)
    var startedAt: Long,

    @Column(name = "ended_at", nullable = false)
    var endedAt: Long,

    @Column(name = "active_seconds", nullable = false)
    var activeSeconds: Int,

    @Column(nullable = false)
    var date: LocalDate,

    @Column(name = "activity_type")
    var activityType: String? = null,

    @Column(name = "estimated_without_minutes")
    var estimatedWithoutMinutes: Int? = null,

    @Column(name = "time_saved_minutes")
    var timeSavedMinutes: Int? = null,

    var logged: Boolean = false,

    @Column(name = "synced_at")
    var syncedAt: Instant = Instant.now()
)
```

Create `backend/src/main/kotlin/app/rippl/sessions/SessionRepository.kt`:

```kotlin
package app.rippl.sessions

import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate
import java.util.UUID

interface SessionRepository : JpaRepository<Session, String> {
    fun findByUserIdAndDateBetween(userId: UUID, start: LocalDate, end: LocalDate): List<Session>
    fun findByUserId(userId: UUID): List<Session>
    fun deleteByUserId(userId: UUID)
    fun countByUserId(userId: UUID): Long
}
```

Create `backend/src/main/kotlin/app/rippl/sessions/SessionUpsertRepository.kt`:

```kotlin
package app.rippl.sessions

import app.rippl.sync.SyncSessionDto
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

data class UpsertResult(val accepted: Int, val duplicates: Int, val syncedAt: Long)

@Repository
class SessionUpsertRepository(private val jdbc: JdbcTemplate) {

    fun upsertSessions(userId: UUID, sessions: List<SyncSessionDto>): UpsertResult {
        var accepted = 0
        var duplicates = 0
        for (s in sessions) {
            val inserted = jdbc.queryForObject(
                """
                INSERT INTO sessions (id, user_id, domain, started_at, ended_at, active_seconds, date,
                                     activity_type, estimated_without_minutes, time_saved_minutes, logged)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (id) DO UPDATE SET synced_at = NOW()
                RETURNING (xmax = 0)
                """,
                Boolean::class.java,
                s.id, userId, s.domain, s.startedAt, s.endedAt, s.activeSeconds,
                s.date, s.activityType, s.estimatedWithoutMinutes, s.timeSavedMinutes, s.logged
            )
            if (inserted == true) accepted++ else duplicates++
        }
        return UpsertResult(accepted, duplicates, Instant.now().toEpochMilli())
    }
}
```

Create `backend/src/main/kotlin/app/rippl/sync/SyncDtos.kt`:

```kotlin
package app.rippl.sync

import java.time.LocalDate

data class SyncRequest(val sessions: List<SyncSessionDto>)

data class SyncSessionDto(
    val id: String,
    val domain: String,
    val startedAt: Long,
    val endedAt: Long,
    val activeSeconds: Int,
    val date: LocalDate,
    val activityType: String? = null,
    val estimatedWithoutMinutes: Int? = null,
    val timeSavedMinutes: Int? = null,
    val logged: Boolean = false
)

data class SyncResponse(val accepted: Int, val duplicates: Int, val syncedAt: Long)
```

- [ ] **Step 2: Write sync controller test**

Create `backend/src/test/kotlin/app/rippl/sync/SyncControllerTest.kt`:

```kotlin
package app.rippl.sync

import app.rippl.TestcontainersConfig
import app.rippl.auth.JwtService
import app.rippl.auth.User
import app.rippl.auth.UserRepository
import jakarta.servlet.http.Cookie
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfig::class)
class SyncControllerTest {

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var jwtService: JwtService
    @Autowired lateinit var userRepository: UserRepository

    private lateinit var sessionCookie: Cookie

    @BeforeEach
    fun setup() {
        val user = userRepository.findByEmail("sync-test@example.com")
            ?: userRepository.save(User(email = "sync-test@example.com"))
        sessionCookie = Cookie("session", jwtService.generateSessionToken(user.id!!))
    }

    @Test
    fun `POST sync sessions returns accepted count`() {
        mockMvc.post("/api/sync/sessions") {
            contentType = MediaType.APPLICATION_JSON
            cookie(sessionCookie)
            content = """
            {
              "sessions": [{
                "id": "sess-1714700000000-a3f8k2",
                "domain": "claude.ai",
                "startedAt": 1714700000000,
                "endedAt": 1714700720000,
                "activeSeconds": 680,
                "date": "2026-05-03",
                "activityType": "coding",
                "estimatedWithoutMinutes": 30,
                "timeSavedMinutes": 19,
                "logged": true
              }]
            }
            """
        }.andExpect {
            status { isOk() }
            jsonPath("$.accepted") { value(1) }
            jsonPath("$.duplicates") { value(0) }
        }
    }

    @Test
    fun `POST sync sessions deduplicates existing sessions`() {
        val body = """
        {
          "sessions": [{
            "id": "sess-dedup-test",
            "domain": "claude.ai",
            "startedAt": 1714700000000,
            "endedAt": 1714700720000,
            "activeSeconds": 680,
            "date": "2026-05-03"
          }]
        }
        """

        mockMvc.post("/api/sync/sessions") {
            contentType = MediaType.APPLICATION_JSON
            cookie(sessionCookie)
            content = body
        }.andExpect { status { isOk() } }

        mockMvc.post("/api/sync/sessions") {
            contentType = MediaType.APPLICATION_JSON
            cookie(sessionCookie)
            content = body
        }.andExpect {
            status { isOk() }
            jsonPath("$.accepted") { value(0) }
            jsonPath("$.duplicates") { value(1) }
        }
    }

    @Test
    fun `POST sync sessions returns 401 without auth`() {
        mockMvc.post("/api/sync/sessions") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"sessions": []}"""
        }.andExpect {
            status { isUnauthorized() }
        }
    }
}
```

- [ ] **Step 3: Run tests to verify they fail**

```bash
./gradlew :backend:test --tests "app.rippl.sync.SyncControllerTest"
```

Expected: FAIL — SyncController/SyncService not yet created

- [ ] **Step 4: Implement SyncService, SyncController, RateLimiter**

Create `backend/src/main/kotlin/app/rippl/sync/RateLimiter.kt`:

```kotlin
package app.rippl.sync

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Service
class RateLimiter(@Value("\${app.rate-limit.sync-per-minute}") private val maxPerMinute: Int) {
    private val requests = ConcurrentHashMap<UUID, MutableList<Instant>>()

    fun tryAcquire(userId: UUID): Boolean {
        val now = Instant.now()
        val userRequests = requests.computeIfAbsent(userId) { mutableListOf() }
        synchronized(userRequests) {
            userRequests.removeIf { it.isBefore(now.minusSeconds(60)) }
            if (userRequests.size >= maxPerMinute) return false
            userRequests.add(now)
            return true
        }
    }
}
```

Create `backend/src/main/kotlin/app/rippl/sync/SyncService.kt`:

```kotlin
package app.rippl.sync

import app.rippl.sessions.SessionUpsertRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class SyncService(private val sessionUpsertRepository: SessionUpsertRepository) {

    @Transactional
    fun sync(userId: UUID, sessions: List<SyncSessionDto>): SyncResponse {
        val result = sessionUpsertRepository.upsertSessions(userId, sessions)
        return SyncResponse(result.accepted, result.duplicates, result.syncedAt)
    }
}
```

Create `backend/src/main/kotlin/app/rippl/sync/SyncController.kt`:

```kotlin
package app.rippl.sync

import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/sync")
class SyncController(
    private val syncService: SyncService,
    private val rateLimiter: RateLimiter
) {

    @PostMapping("/sessions")
    fun syncSessions(
        @AuthenticationPrincipal userId: UUID,
        @RequestBody request: SyncRequest
    ): ResponseEntity<Any> {
        if (!rateLimiter.tryAcquire(userId)) {
            return ResponseEntity.status(429).body(mapOf("error" to "rate_limit_exceeded"))
        }
        val result = syncService.sync(userId, request.sessions)
        return ResponseEntity.ok(result)
    }
}
```

- [ ] **Step 5: Run tests to verify they pass**

```bash
./gradlew :backend:test --tests "app.rippl.sync.SyncControllerTest"
```

Expected: ALL PASS

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/kotlin/app/rippl/sessions/ backend/src/main/kotlin/app/rippl/sync/ backend/src/test/kotlin/app/rippl/sync/
git commit -m "feat: add sync endpoint with deduplication and rate limiting"
```

---

## Task 6: Trends API

**Files:**
- Create: `backend/src/main/kotlin/app/rippl/trends/TrendsDtos.kt`
- Create: `backend/src/main/kotlin/app/rippl/trends/TrendsService.kt`
- Create: `backend/src/main/kotlin/app/rippl/trends/TrendsController.kt`
- Test: `backend/src/test/kotlin/app/rippl/trends/TrendsServiceTest.kt`

- [ ] **Step 1: Create DTOs**

Create `backend/src/main/kotlin/app/rippl/trends/TrendsDtos.kt`:

```kotlin
package app.rippl.trends

import java.time.LocalDate

data class WeeklyTrend(val week: LocalDate, val domain: String, val totalSeconds: Long, val totalSaved: Int)
data class MonthlyTrend(val month: LocalDate, val domain: String, val totalSeconds: Long, val totalSaved: Int)
data class TimeSaved(val total: Int, val byDomain: Map<String, Int>, val byActivity: Map<String, Int>)
```

- [ ] **Step 2: Write trends service test**

Create `backend/src/test/kotlin/app/rippl/trends/TrendsServiceTest.kt`:

```kotlin
package app.rippl.trends

import app.rippl.TestcontainersConfig
import app.rippl.auth.User
import app.rippl.auth.UserRepository
import app.rippl.sessions.Session
import app.rippl.sessions.SessionRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import java.time.LocalDate
import java.util.UUID

@SpringBootTest
@Import(TestcontainersConfig::class)
class TrendsServiceTest {

    @Autowired lateinit var trendsService: TrendsService
    @Autowired lateinit var sessionRepository: SessionRepository
    @Autowired lateinit var userRepository: UserRepository

    private lateinit var userId: UUID

    @BeforeEach
    fun setup() {
        sessionRepository.deleteAll()
        val user = userRepository.findByEmail("trends-test@example.com")
            ?: userRepository.save(User(email = "trends-test@example.com"))
        userId = user.id!!

        sessionRepository.saveAll(listOf(
            Session("ts-1", userId, "claude.ai", 1000, 2000, 600, LocalDate.of(2026, 4, 28),
                "coding", 30, 19),
            Session("ts-2", userId, "chatgpt.com", 2000, 3000, 300, LocalDate.of(2026, 4, 28),
                "writing", 15, 8),
            Session("ts-3", userId, "claude.ai", 3000, 4000, 900, LocalDate.of(2026, 5, 1),
                "coding", 45, 25)
        ))
    }

    @Test
    fun `weekly returns data grouped by week and domain`() {
        val result = trendsService.weekly(userId, LocalDate.of(2026, 4, 1), LocalDate.of(2026, 5, 31))
        assertTrue(result.isNotEmpty())
        val claudeEntries = result.filter { it.domain == "claude.ai" }
        assertTrue(claudeEntries.isNotEmpty())
    }

    @Test
    fun `monthly returns data grouped by month and domain`() {
        val result = trendsService.monthly(userId, LocalDate.of(2026, 4, 1), LocalDate.of(2026, 5, 31))
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun `timeSaved returns correct totals`() {
        val result = trendsService.timeSaved(userId)
        assertEquals(52, result.total) // 19 + 8 + 25
        assertEquals(44, result.byDomain["claude.ai"]) // 19 + 25
        assertEquals(8, result.byDomain["chatgpt.com"])
        assertEquals(44, result.byActivity["coding"]) // 19 + 25
        assertEquals(8, result.byActivity["writing"])
    }

    @Test
    fun `timeSaved returns zeros for user with no sessions`() {
        val emptyUser = userRepository.save(User(email = "empty-trends@example.com"))
        val result = trendsService.timeSaved(emptyUser.id!!)
        assertEquals(0, result.total)
        assertTrue(result.byDomain.isEmpty())
    }
}
```

- [ ] **Step 3: Run tests to verify they fail**

```bash
./gradlew :backend:test --tests "app.rippl.trends.TrendsServiceTest"
```

Expected: FAIL — TrendsService not yet created

- [ ] **Step 4: Implement TrendsService and TrendsController**

Create `backend/src/main/kotlin/app/rippl/trends/TrendsService.kt`:

```kotlin
package app.rippl.trends

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.UUID

@Service
class TrendsService(private val jdbc: JdbcTemplate) {

    fun weekly(userId: UUID, from: LocalDate, to: LocalDate): List<WeeklyTrend> =
        jdbc.query(
            """
            SELECT date_trunc('week', date)::date AS week, domain,
                   SUM(active_seconds)::bigint AS total_seconds,
                   COALESCE(SUM(time_saved_minutes), 0)::int AS total_saved
            FROM sessions
            WHERE user_id = ? AND date >= ? AND date <= ?
            GROUP BY week, domain
            ORDER BY week
            """,
            { rs, _ ->
                WeeklyTrend(
                    rs.getDate("week").toLocalDate(),
                    rs.getString("domain"),
                    rs.getLong("total_seconds"),
                    rs.getInt("total_saved")
                )
            },
            userId, from, to
        )

    fun monthly(userId: UUID, from: LocalDate, to: LocalDate): List<MonthlyTrend> =
        jdbc.query(
            """
            SELECT date_trunc('month', date)::date AS month, domain,
                   SUM(active_seconds)::bigint AS total_seconds,
                   COALESCE(SUM(time_saved_minutes), 0)::int AS total_saved
            FROM sessions
            WHERE user_id = ? AND date >= ? AND date <= ?
            GROUP BY month, domain
            ORDER BY month
            """,
            { rs, _ ->
                MonthlyTrend(
                    rs.getDate("month").toLocalDate(),
                    rs.getString("domain"),
                    rs.getLong("total_seconds"),
                    rs.getInt("total_saved")
                )
            },
            userId, from, to
        )

    fun timeSaved(userId: UUID): TimeSaved {
        val total = jdbc.queryForObject(
            "SELECT COALESCE(SUM(time_saved_minutes), 0) FROM sessions WHERE user_id = ?",
            Int::class.java, userId
        ) ?: 0

        val byDomain = jdbc.query(
            """
            SELECT domain, COALESCE(SUM(time_saved_minutes), 0)::int AS saved
            FROM sessions WHERE user_id = ? AND time_saved_minutes IS NOT NULL
            GROUP BY domain ORDER BY saved DESC
            """,
            { rs, _ -> rs.getString("domain") to rs.getInt("saved") },
            userId
        ).toMap()

        val byActivity = jdbc.query(
            """
            SELECT activity_type, COALESCE(SUM(time_saved_minutes), 0)::int AS saved
            FROM sessions WHERE user_id = ? AND activity_type IS NOT NULL AND time_saved_minutes IS NOT NULL
            GROUP BY activity_type ORDER BY saved DESC
            """,
            { rs, _ -> rs.getString("activity_type") to rs.getInt("saved") },
            userId
        ).toMap()

        return TimeSaved(total, byDomain, byActivity)
    }
}
```

Create `backend/src/main/kotlin/app/rippl/trends/TrendsController.kt`:

```kotlin
package app.rippl.trends

import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.util.UUID

@RestController
@RequestMapping("/api/trends")
class TrendsController(private val trendsService: TrendsService) {

    @GetMapping("/weekly")
    fun weekly(
        @AuthenticationPrincipal userId: UUID,
        @RequestParam(required = false) from: LocalDate?,
        @RequestParam(required = false) to: LocalDate?
    ): List<WeeklyTrend> {
        val end = to ?: LocalDate.now()
        val start = from ?: end.minusWeeks(12)
        return trendsService.weekly(userId, start, end)
    }

    @GetMapping("/monthly")
    fun monthly(
        @AuthenticationPrincipal userId: UUID,
        @RequestParam(required = false) from: LocalDate?,
        @RequestParam(required = false) to: LocalDate?
    ): List<MonthlyTrend> {
        val end = to ?: LocalDate.now()
        val start = from ?: end.minusMonths(12)
        return trendsService.monthly(userId, start, end)
    }

    @GetMapping("/time-saved")
    fun timeSaved(@AuthenticationPrincipal userId: UUID): TimeSaved =
        trendsService.timeSaved(userId)
}
```

- [ ] **Step 5: Run tests to verify they pass**

```bash
./gradlew :backend:test --tests "app.rippl.trends.TrendsServiceTest"
```

Expected: ALL PASS

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/kotlin/app/rippl/trends/ backend/src/test/kotlin/app/rippl/trends/
git commit -m "feat: add trends API with weekly/monthly aggregation and time saved"
```

---

## Task 7: Insights API (Mirror Moments)

**Files:**
- Create: `backend/src/main/kotlin/app/rippl/insights/InsightsService.kt`
- Create: `backend/src/main/kotlin/app/rippl/insights/InsightsController.kt`
- Test: `backend/src/test/kotlin/app/rippl/insights/InsightsServiceTest.kt`

- [ ] **Step 1: Write insights service test**

Create `backend/src/test/kotlin/app/rippl/insights/InsightsServiceTest.kt`:

```kotlin
package app.rippl.insights

import app.rippl.TestcontainersConfig
import app.rippl.auth.User
import app.rippl.auth.UserRepository
import app.rippl.sessions.Session
import app.rippl.sessions.SessionRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.util.UUID

@SpringBootTest
@Import(TestcontainersConfig::class)
class InsightsServiceTest {

    @Autowired lateinit var insightsService: InsightsService
    @Autowired lateinit var sessionRepository: SessionRepository
    @Autowired lateinit var userRepository: UserRepository

    private lateinit var userId: UUID

    @BeforeEach
    fun setup() {
        sessionRepository.deleteAll()
        val user = userRepository.findByEmail("insights-test@example.com")
            ?: userRepository.save(User(email = "insights-test@example.com"))
        userId = user.id!!

        val thisMonday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val lastMonday = thisMonday.minusWeeks(1)

        sessionRepository.saveAll(listOf(
            Session("ins-1", userId, "claude.ai", 1000, 2000, 3600, thisMonday,
                "coding", 60, 40),
            Session("ins-2", userId, "claude.ai", 2000, 3000, 1800, lastMonday,
                "coding", 30, 20),
            Session("ins-3", userId, "chatgpt.com", 3000, 4000, 600, thisMonday,
                "writing", 10, 5),
            Session("ins-4", userId, "claude.ai", 4000, 5000, 900, thisMonday.with(DayOfWeek.FRIDAY),
                "coding", 20, 12)
        ))
    }

    @Test
    fun `generates weekly usage moment`() {
        val moments = insightsService.mirrorMoments(userId)
        val weeklyMoment = moments.find { it.type == "weekly_usage" }
        assertNotNull(weeklyMoment)
        assertTrue(weeklyMoment!!.message.contains("hours this week"))
    }

    @Test
    fun `generates top tool moment when multiple tools used`() {
        val moments = insightsService.mirrorMoments(userId)
        val toolMoment = moments.find { it.type == "top_tool" }
        assertNotNull(toolMoment)
        assertTrue(toolMoment!!.message.contains("claude.ai"))
    }

    @Test
    fun `generates time saving activity moment`() {
        val moments = insightsService.mirrorMoments(userId)
        val activityMoment = moments.find { it.type == "time_saving_activity" }
        assertNotNull(activityMoment)
        assertTrue(activityMoment!!.message.contains("coding"))
    }

    @Test
    fun `returns empty list for user with no sessions`() {
        val emptyUser = userRepository.save(User(email = "empty-insights@example.com"))
        val moments = insightsService.mirrorMoments(emptyUser.id!!)
        assertTrue(moments.isEmpty())
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

```bash
./gradlew :backend:test --tests "app.rippl.insights.InsightsServiceTest"
```

Expected: FAIL — InsightsService not yet created

- [ ] **Step 3: Implement InsightsService and InsightsController**

Create `backend/src/main/kotlin/app/rippl/insights/InsightsService.kt`:

```kotlin
package app.rippl.insights

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import java.util.UUID

data class MirrorMoment(val type: String, val message: String)

@Service
class InsightsService(private val jdbc: JdbcTemplate) {

    private val dayNames = arrayOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")

    fun mirrorMoments(userId: UUID): List<MirrorMoment> {
        val moments = mutableListOf<MirrorMoment>()

        weeklyUsage(userId)?.let { moments.add(it) }
        topTool(userId)?.let { moments.add(it) }
        timeSavingActivity(userId)?.let { moments.add(it) }
        busiestDay(userId)?.let { moments.add(it) }

        return moments
    }

    private fun weeklyUsage(userId: UUID): MirrorMoment? {
        val rows = jdbc.query(
            """
            SELECT
                COALESCE(SUM(CASE WHEN date >= date_trunc('week', CURRENT_DATE) THEN active_seconds END), 0) AS this_week,
                COALESCE(SUM(CASE WHEN date >= date_trunc('week', CURRENT_DATE) - 7
                                   AND date < date_trunc('week', CURRENT_DATE) THEN active_seconds END), 0) AS last_week
            FROM sessions WHERE user_id = ?
            """,
            { rs, _ -> rs.getLong("this_week") to rs.getLong("last_week") },
            userId
        )
        val (thisWeek, lastWeek) = rows.firstOrNull() ?: return null
        if (thisWeek == 0L) return null

        val hours = "%.1f".format(thisWeek / 3600.0)
        val comparison = when {
            lastWeek == 0L -> "your first tracked week"
            thisWeek > lastWeek -> "${(thisWeek - lastWeek) * 100 / lastWeek}% more than last week"
            thisWeek < lastWeek -> "${(lastWeek - thisWeek) * 100 / lastWeek}% less than last week"
            else -> "same as last week"
        }
        return MirrorMoment("weekly_usage", "You used AI for $hours hours this week — $comparison.")
    }

    private fun topTool(userId: UUID): MirrorMoment? {
        val tools = jdbc.query(
            """
            SELECT domain, SUM(active_seconds)::bigint AS total
            FROM sessions WHERE user_id = ?
            GROUP BY domain ORDER BY total DESC LIMIT 2
            """,
            { rs, _ -> rs.getString("domain") to rs.getLong("total") },
            userId
        )
        if (tools.size < 2) return null
        val ratio = "%.1f".format(tools[0].second.toDouble() / tools[1].second)
        return MirrorMoment(
            "top_tool",
            "${tools[0].first} is your most-used tool. You spend ${ratio}x more time there than ${tools[1].first}."
        )
    }

    private fun timeSavingActivity(userId: UUID): MirrorMoment? {
        val rows = jdbc.query(
            """
            SELECT activity_type, COALESCE(SUM(time_saved_minutes), 0)::int AS saved
            FROM sessions WHERE user_id = ? AND activity_type IS NOT NULL
              AND date >= date_trunc('month', CURRENT_DATE)
            GROUP BY activity_type ORDER BY saved DESC LIMIT 1
            """,
            { rs, _ -> rs.getString("activity_type") to rs.getInt("saved") },
            userId
        )
        val (activity, saved) = rows.firstOrNull() ?: return null
        if (saved == 0) return null
        val hours = "%.1f".format(saved / 60.0)
        return MirrorMoment("time_saving_activity", "$activity is where AI saves you the most time — $hours hours freed this month.")
    }

    private fun busiestDay(userId: UUID): MirrorMoment? {
        val byDay = jdbc.query(
            """
            SELECT EXTRACT(DOW FROM date)::int AS dow, SUM(active_seconds)::bigint AS total
            FROM sessions WHERE user_id = ?
            GROUP BY dow ORDER BY total DESC
            """,
            { rs, _ -> rs.getInt("dow") to rs.getLong("total") },
            userId
        )
        if (byDay.size < 2) return null
        return MirrorMoment(
            "busiest_day",
            "Your busiest AI day is ${dayNames[byDay.first().first]}. You barely touch AI on ${dayNames[byDay.last().first]}."
        )
    }
}
```

Create `backend/src/main/kotlin/app/rippl/insights/InsightsController.kt`:

```kotlin
package app.rippl.insights

import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/insights")
class InsightsController(private val insightsService: InsightsService) {

    @GetMapping("/mirror")
    fun mirror(@AuthenticationPrincipal userId: UUID): List<MirrorMoment> =
        insightsService.mirrorMoments(userId)
}
```

- [ ] **Step 4: Run tests to verify they pass**

```bash
./gradlew :backend:test --tests "app.rippl.insights.InsightsServiceTest"
```

Expected: ALL PASS

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/kotlin/app/rippl/insights/ backend/src/test/kotlin/app/rippl/insights/
git commit -m "feat: add mirror moments insight generation"
```

---

## Task 8: Collectors + Account API

**Files:**
- Create: `backend/src/main/kotlin/app/rippl/collectors/Collector.kt`
- Create: `backend/src/main/kotlin/app/rippl/collectors/CollectorRepository.kt`
- Create: `backend/src/main/kotlin/app/rippl/collectors/CollectorsController.kt`
- Create: `backend/src/main/kotlin/app/rippl/account/AccountController.kt`
- Test: `backend/src/test/kotlin/app/rippl/collectors/CollectorsControllerTest.kt`
- Test: `backend/src/test/kotlin/app/rippl/account/AccountControllerTest.kt`

- [ ] **Step 1: Create Collector entity and repository**

Create `backend/src/main/kotlin/app/rippl/collectors/Collector.kt`:

```kotlin
package app.rippl.collectors

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "collectors")
class Collector(
    @Column(name = "user_id", nullable = false)
    var userId: UUID,

    @Column(nullable = false)
    var type: String,

    var enabled: Boolean = true,

    @Column(name = "linked_at")
    var linkedAt: Instant = Instant.now(),

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null
)
```

Create `backend/src/main/kotlin/app/rippl/collectors/CollectorRepository.kt`:

```kotlin
package app.rippl.collectors

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface CollectorRepository : JpaRepository<Collector, UUID> {
    fun findByUserId(userId: UUID): List<Collector>
    fun findByIdAndUserId(id: UUID, userId: UUID): Collector?
    fun deleteByUserId(userId: UUID)
}
```

- [ ] **Step 2: Write controller tests**

Create `backend/src/test/kotlin/app/rippl/collectors/CollectorsControllerTest.kt`:

```kotlin
package app.rippl.collectors

import app.rippl.TestcontainersConfig
import app.rippl.auth.JwtService
import app.rippl.auth.User
import app.rippl.auth.UserRepository
import jakarta.servlet.http.Cookie
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.*

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfig::class)
class CollectorsControllerTest {

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var jwtService: JwtService
    @Autowired lateinit var userRepository: UserRepository
    @Autowired lateinit var collectorRepository: CollectorRepository

    private lateinit var sessionCookie: Cookie
    private lateinit var userId: java.util.UUID

    @BeforeEach
    fun setup() {
        val user = userRepository.findByEmail("collector-test@example.com")
            ?: userRepository.save(User(email = "collector-test@example.com"))
        userId = user.id!!
        sessionCookie = Cookie("session", jwtService.generateSessionToken(userId))
        collectorRepository.deleteByUserId(userId)
    }

    @Test
    fun `GET collectors returns empty list initially`() {
        mockMvc.get("/api/collectors") {
            cookie(sessionCookie)
        }.andExpect {
            status { isOk() }
            jsonPath("$.length()") { value(0) }
        }
    }

    @Test
    fun `POST collectors creates new collector`() {
        mockMvc.post("/api/collectors") {
            contentType = MediaType.APPLICATION_JSON
            cookie(sessionCookie)
            content = """{"type": "chrome_extension"}"""
        }.andExpect {
            status { isOk() }
            jsonPath("$.type") { value("chrome_extension") }
            jsonPath("$.enabled") { value(true) }
        }
    }

    @Test
    fun `DELETE collectors removes collector`() {
        val collector = collectorRepository.save(Collector(userId = userId, type = "chrome_extension"))

        mockMvc.delete("/api/collectors/${collector.id}") {
            cookie(sessionCookie)
        }.andExpect {
            status { isOk() }
        }

        mockMvc.get("/api/collectors") {
            cookie(sessionCookie)
        }.andExpect {
            jsonPath("$.length()") { value(0) }
        }
    }
}
```

Create `backend/src/test/kotlin/app/rippl/account/AccountControllerTest.kt`:

```kotlin
package app.rippl.account

import app.rippl.TestcontainersConfig
import app.rippl.auth.JwtService
import app.rippl.auth.User
import app.rippl.auth.UserRepository
import app.rippl.sessions.Session
import app.rippl.sessions.SessionRepository
import jakarta.servlet.http.Cookie
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import java.time.LocalDate

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfig::class)
class AccountControllerTest {

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var jwtService: JwtService
    @Autowired lateinit var userRepository: UserRepository
    @Autowired lateinit var sessionRepository: SessionRepository

    @Test
    fun `DELETE account removes user and all data`() {
        val user = userRepository.save(User(email = "delete-me@example.com"))
        sessionRepository.save(
            Session("del-1", user.id!!, "claude.ai", 1000, 2000, 600, LocalDate.now())
        )
        val cookie = Cookie("session", jwtService.generateSessionToken(user.id!!))

        mockMvc.delete("/api/account") {
            cookie(cookie)
        }.andExpect {
            status { isOk() }
        }

        assertNull(userRepository.findByEmail("delete-me@example.com"))
        assertEquals(0, sessionRepository.countByUserId(user.id!!))
    }
}
```

- [ ] **Step 3: Run tests to verify they fail**

```bash
./gradlew :backend:test --tests "app.rippl.collectors.CollectorsControllerTest" --tests "app.rippl.account.AccountControllerTest"
```

Expected: FAIL — controllers not yet created

- [ ] **Step 4: Implement CollectorsController and AccountController**

Create `backend/src/main/kotlin/app/rippl/collectors/CollectorsController.kt`:

```kotlin
package app.rippl.collectors

import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.UUID

data class CreateCollectorRequest(val type: String)

@RestController
@RequestMapping("/api/collectors")
class CollectorsController(private val collectorRepository: CollectorRepository) {

    @GetMapping
    fun list(@AuthenticationPrincipal userId: UUID): List<Map<String, Any?>> =
        collectorRepository.findByUserId(userId).map { it.toDto() }

    @PostMapping
    fun create(
        @AuthenticationPrincipal userId: UUID,
        @RequestBody request: CreateCollectorRequest
    ): Map<String, Any?> {
        val collector = collectorRepository.save(Collector(userId = userId, type = request.type))
        return collector.toDto()
    }

    @DeleteMapping("/{id}")
    fun delete(@AuthenticationPrincipal userId: UUID, @PathVariable id: UUID): ResponseEntity<Void> {
        val collector = collectorRepository.findByIdAndUserId(id, userId)
            ?: return ResponseEntity.notFound().build()
        collectorRepository.delete(collector)
        return ResponseEntity.ok().build()
    }

    private fun Collector.toDto() = mapOf(
        "id" to id,
        "type" to type,
        "enabled" to enabled,
        "linkedAt" to linkedAt.toString()
    )
}
```

Create `backend/src/main/kotlin/app/rippl/account/AccountController.kt`:

```kotlin
package app.rippl.account

import app.rippl.auth.UserRepository
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseCookie
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Duration
import java.util.UUID

@RestController
@RequestMapping("/api/account")
class AccountController(private val userRepository: UserRepository) {

    @DeleteMapping
    @Transactional
    fun deleteAccount(@AuthenticationPrincipal userId: UUID): ResponseEntity<Void> {
        userRepository.deleteById(userId)
        val cookie = ResponseCookie.from("session", "")
            .httpOnly(true).path("/").maxAge(Duration.ZERO).build()
        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, cookie.toString())
            .build()
    }
}
```

- [ ] **Step 5: Run tests to verify they pass**

```bash
./gradlew :backend:test --tests "app.rippl.collectors.CollectorsControllerTest" --tests "app.rippl.account.AccountControllerTest"
```

Expected: ALL PASS

- [ ] **Step 6: Run full backend test suite**

```bash
./gradlew :backend:test
```

Expected: ALL PASS — verify no regressions

- [ ] **Step 7: Commit**

```bash
git add backend/src/main/kotlin/app/rippl/collectors/ backend/src/main/kotlin/app/rippl/account/ backend/src/test/kotlin/app/rippl/collectors/ backend/src/test/kotlin/app/rippl/account/
git commit -m "feat: add collectors CRUD and account deletion endpoints"
```

---

## Task 9: Frontend Shell (Routing, Layout, Auth, Types, Data)

**Files:**
- Create: `frontend/src/types.ts`
- Create: `frontend/src/data/domains.ts`, `frontend/src/data/comparisons.ts`
- Create: `frontend/src/api/client.ts`, `frontend/src/api/auth.ts`
- Create: `frontend/src/context/AuthContext.tsx`, `frontend/src/hooks/useAuth.ts`
- Create: `frontend/src/components/Layout.tsx`
- Replace: `frontend/src/App.tsx`, `frontend/src/main.tsx`

- [ ] **Step 1: Create shared types**

Create `frontend/src/types.ts`:

```typescript
export interface User {
  id: string
  email: string
}

export interface WeeklyTrend {
  week: string
  domain: string
  totalSeconds: number
  totalSaved: number
}

export interface MonthlyTrend {
  month: string
  domain: string
  totalSeconds: number
  totalSaved: number
}

export interface TimeSaved {
  total: number
  byDomain: Record<string, number>
  byActivity: Record<string, number>
}

export interface MirrorMoment {
  type: string
  message: string
}

export interface CollectorInfo {
  id: string
  type: string
  enabled: boolean
  linkedAt: string
}
```

- [ ] **Step 2: Create domain metadata and comparisons data**

Create `frontend/src/data/domains.ts`:

```typescript
export interface DomainInfo {
  name: string
  icon: string
  color: string
}

export const DOMAINS: Record<string, DomainInfo> = {
  'claude.ai': { name: 'Claude', icon: '\u{1F916}', color: '#D4A574' },
  'chatgpt.com': { name: 'ChatGPT', icon: '\u{1F4AC}', color: '#10A37F' },
  'chat.openai.com': { name: 'ChatGPT', icon: '\u{1F4AC}', color: '#10A37F' },
  'gemini.google.com': { name: 'Gemini', icon: '✨', color: '#4285F4' },
  'copilot.microsoft.com': { name: 'Copilot', icon: '\u{1F535}', color: '#0078D4' },
  'perplexity.ai': { name: 'Perplexity', icon: '\u{1F50D}', color: '#20808D' },
  'you.com': { name: 'You.com', icon: '\u{1F7E3}', color: '#7B61FF' },
  'poe.com': { name: 'Poe', icon: '⚡', color: '#5856D6' },
  'phind.com': { name: 'Phind', icon: '\u{1F50E}', color: '#3B82F6' },
  'huggingface.co': { name: 'Hugging Face', icon: '\u{1F917}', color: '#FFD21E' },
  'pi.ai': { name: 'Pi', icon: '\u{1F967}', color: '#E85D04' },
  'meta.ai': { name: 'Meta AI', icon: '\u{1F537}', color: '#0668E1' },
  'mistral.ai': { name: 'Mistral', icon: '\u{1F32A}', color: '#F7D046' },
  'deepseek.com': { name: 'DeepSeek', icon: '\u{1F30A}', color: '#4F46E5' },
  'grok.x.ai': { name: 'Grok', icon: '\u{1F47E}', color: '#1DA1F2' },
}

export function getDomain(domain: string): DomainInfo {
  return DOMAINS[domain] ?? { name: domain, icon: '\u{1F310}', color: '#6B7280' }
}
```

Create `frontend/src/data/comparisons.ts`:

```typescript
interface Comparison {
  threshold: number
  text: string
}

const TIME_COMPARISONS: Comparison[] = [
  { threshold: 5, text: 'enough to brew and enjoy a cup of coffee' },
  { threshold: 15, text: 'enough to take a proper walk around the block' },
  { threshold: 30, text: 'enough to cook a meal from scratch' },
  { threshold: 60, text: 'enough to watch an episode of your favorite show' },
  { threshold: 120, text: 'enough to watch a full movie' },
  { threshold: 300, text: 'enough to run a half marathon' },
  { threshold: 480, text: 'enough to read a novel cover to cover' },
  { threshold: 1200, text: 'enough to binge an entire season of a TV show' },
  { threshold: 2400, text: 'enough to read War and Peace' },
  { threshold: 4800, text: 'enough to run a marathon... twice' },
]

export function getComparison(minutes: number): string {
  let best = TIME_COMPARISONS[0]
  for (const c of TIME_COMPARISONS) {
    if (minutes >= c.threshold) best = c
    else break
  }
  return best.text
}
```

- [ ] **Step 3: Create API client and auth API**

Create `frontend/src/api/client.ts`:

```typescript
const BASE = '/api'

async function request<T>(path: string, options?: RequestInit): Promise<T> {
  const res = await fetch(`${BASE}${path}`, {
    ...options,
    headers: { 'Content-Type': 'application/json', ...options?.headers },
    credentials: 'same-origin',
  })
  if (res.status === 401) {
    window.location.href = '/login'
    throw new Error('Unauthorized')
  }
  if (!res.ok) throw new Error(`API error: ${res.status}`)
  if (res.headers.get('content-length') === '0') return undefined as T
  return res.json()
}

export const api = {
  get: <T>(path: string) => request<T>(path),
  post: <T>(path: string, body?: unknown) =>
    request<T>(path, { method: 'POST', body: body ? JSON.stringify(body) : undefined }),
  delete: <T>(path: string) => request<T>(path, { method: 'DELETE' }),
}
```

Create `frontend/src/api/auth.ts`:

```typescript
import { api } from './client'
import type { User } from '../types'

export function sendMagicLink(email: string): Promise<{ sent: boolean }> {
  return api.post('/auth/magic-link', { email })
}

export function getMe(): Promise<User> {
  return api.get<User>('/auth/me')
}

export function logout(): Promise<void> {
  return api.post('/auth/logout')
}
```

- [ ] **Step 4: Create AuthContext and useAuth hook**

Create `frontend/src/context/AuthContext.tsx`:

```tsx
import { createContext, useEffect, useState, type ReactNode } from 'react'
import type { User } from '../types'
import { getMe, logout as apiLogout } from '../api/auth'

interface AuthState {
  user: User | null
  loading: boolean
  logout: () => Promise<void>
  refresh: () => Promise<void>
}

export const AuthContext = createContext<AuthState>({
  user: null,
  loading: true,
  logout: async () => {},
  refresh: async () => {},
})

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [loading, setLoading] = useState(true)

  const refresh = async () => {
    try {
      const u = await getMe()
      setUser(u)
    } catch {
      setUser(null)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { refresh() }, [])

  const logout = async () => {
    await apiLogout()
    setUser(null)
    window.location.href = '/login'
  }

  return (
    <AuthContext.Provider value={{ user, loading, logout, refresh }}>
      {children}
    </AuthContext.Provider>
  )
}
```

Create `frontend/src/hooks/useAuth.ts`:

```typescript
import { useContext } from 'react'
import { AuthContext } from '../context/AuthContext'

export function useAuth() {
  return useContext(AuthContext)
}
```

- [ ] **Step 5: Create Layout component**

Create `frontend/src/components/Layout.tsx`:

```tsx
import { NavLink, Outlet, Navigate } from 'react-router-dom'
import { useAuth } from '../hooks/useAuth'

export function Layout() {
  const { user, loading, logout } = useAuth()

  if (loading) return <div className="min-h-screen flex items-center justify-center">Loading...</div>
  if (!user) return <Navigate to="/login" replace />

  return (
    <div className="min-h-screen bg-gray-50">
      <nav className="bg-white shadow-sm">
        <div className="max-w-5xl mx-auto px-4 py-3 flex justify-between items-center">
          <NavLink to="/" className="text-xl font-bold text-gray-900">rippl</NavLink>
          <div className="flex gap-6 items-center">
            <NavLink to="/" end className={({ isActive }) =>
              isActive ? 'text-indigo-600 font-medium' : 'text-gray-600 hover:text-gray-900'
            }>Dashboard</NavLink>
            <NavLink to="/trends" className={({ isActive }) =>
              isActive ? 'text-indigo-600 font-medium' : 'text-gray-600 hover:text-gray-900'
            }>Trends</NavLink>
            <NavLink to="/mirror" className={({ isActive }) =>
              isActive ? 'text-indigo-600 font-medium' : 'text-gray-600 hover:text-gray-900'
            }>Mirror</NavLink>
            <NavLink to="/settings" className={({ isActive }) =>
              isActive ? 'text-indigo-600 font-medium' : 'text-gray-600 hover:text-gray-900'
            }>Settings</NavLink>
            <button onClick={logout} className="text-gray-400 hover:text-gray-600 text-sm">
              Sign out
            </button>
          </div>
        </div>
      </nav>
      <main className="max-w-5xl mx-auto px-4 py-8">
        <Outlet />
      </main>
    </div>
  )
}
```

- [ ] **Step 6: Wire up App.tsx and main.tsx**

Replace `frontend/src/App.tsx`:

```tsx
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { AuthProvider } from './context/AuthContext'
import { Layout } from './components/Layout'
import { Login } from './pages/Login'
import { Dashboard } from './pages/Dashboard'
import { Trends } from './pages/Trends'
import { Mirror } from './pages/Mirror'
import { Settings } from './pages/Settings'

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route element={<Layout />}>
            <Route path="/" element={<Dashboard />} />
            <Route path="/trends" element={<Trends />} />
            <Route path="/mirror" element={<Mirror />} />
            <Route path="/settings" element={<Settings />} />
          </Route>
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  )
}
```

Replace `frontend/src/main.tsx`:

```tsx
import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import App from './App'
import './index.css'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>
)
```

- [ ] **Step 7: Create placeholder page files**

Create stub files so the app compiles. Each will be replaced in later tasks:

`frontend/src/pages/Login.tsx`:
```tsx
export function Login() { return <div>Login page — TODO</div> }
```

`frontend/src/pages/Dashboard.tsx`:
```tsx
export function Dashboard() { return <div>Dashboard — TODO</div> }
```

`frontend/src/pages/Trends.tsx`:
```tsx
export function Trends() { return <div>Trends — TODO</div> }
```

`frontend/src/pages/Mirror.tsx`:
```tsx
export function Mirror() { return <div>Mirror — TODO</div> }
```

`frontend/src/pages/Settings.tsx`:
```tsx
export function Settings() { return <div>Settings — TODO</div> }
```

- [ ] **Step 8: Remove Vite boilerplate files**

Delete `frontend/src/App.css`, `frontend/src/assets/` (the Vite logo), and any other generated boilerplate that isn't needed.

- [ ] **Step 9: Verify frontend compiles and routing works**

```bash
cd frontend && npm run build
```

Expected: Build succeeds. Then start dev server and verify `/login` shows the stub and `/` redirects to login when not authenticated.

- [ ] **Step 10: Commit**

```bash
git add frontend/src/
git commit -m "feat: add frontend shell with routing, auth context, and layout"
```

---

## Task 10: Login Page

**Files:**
- Replace: `frontend/src/pages/Login.tsx`

- [ ] **Step 1: Implement Login page**

Replace `frontend/src/pages/Login.tsx`:

```tsx
import { useState } from 'react'
import { Navigate, useSearchParams } from 'react-router-dom'
import { sendMagicLink } from '../api/auth'
import { useAuth } from '../hooks/useAuth'

export function Login() {
  const { user, loading } = useAuth()
  const [searchParams] = useSearchParams()
  const [email, setEmail] = useState('')
  const [sent, setSent] = useState(false)
  const [error, setError] = useState(searchParams.get('error') || '')
  const [submitting, setSubmitting] = useState(false)

  if (loading) return <div className="min-h-screen flex items-center justify-center">Loading...</div>
  if (user) return <Navigate to="/" replace />

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    setSubmitting(true)
    try {
      await sendMagicLink(email)
      setSent(true)
    } catch {
      setError('Failed to send magic link. Please try again.')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50">
      <div className="max-w-sm w-full space-y-8 p-8">
        <div className="text-center">
          <h1 className="text-3xl font-bold text-gray-900">rippl</h1>
          <p className="mt-2 text-gray-600">Sign in to your dashboard</p>
        </div>

        {error && (
          <div className="bg-red-50 text-red-700 p-3 rounded text-sm">
            {error === 'invalid_token' ? 'That link has expired or already been used.' : error}
          </div>
        )}

        {sent ? (
          <div className="bg-green-50 text-green-700 p-4 rounded text-center">
            <p className="font-medium">Check your email</p>
            <p className="text-sm mt-1">We sent a sign-in link to {email}</p>
          </div>
        ) : (
          <form onSubmit={handleSubmit} className="space-y-4">
            <input
              type="email"
              value={email}
              onChange={e => setEmail(e.target.value)}
              placeholder="you@example.com"
              required
              className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-indigo-500"
            />
            <button
              type="submit"
              disabled={submitting}
              className="w-full py-2 bg-indigo-600 text-white rounded-md hover:bg-indigo-700 disabled:opacity-50"
            >
              {submitting ? 'Sending...' : 'Send magic link'}
            </button>
          </form>
        )}
      </div>
    </div>
  )
}
```

- [ ] **Step 2: Verify login page renders**

Start dev server, navigate to `/login`. Verify:
- Email input and button visible
- Submitting shows loading state
- After submit shows "check your email" message (API call will fail in dev without backend — that's OK for now)

- [ ] **Step 3: Commit**

```bash
git add frontend/src/pages/Login.tsx
git commit -m "feat: add login page with magic link form"
```

---

## Task 11: Dashboard + Onboarding Pages

**Files:**
- Create: `frontend/src/api/trends.ts`, `frontend/src/api/insights.ts`
- Create: `frontend/src/components/TimeSavedCard.tsx`
- Create: `frontend/src/components/TrendChart.tsx`
- Create: `frontend/src/components/MirrorMomentCard.tsx`
- Create: `frontend/src/components/OnboardingChecklist.tsx`
- Replace: `frontend/src/pages/Dashboard.tsx`

- [ ] **Step 1: Create API modules for trends and insights**

Create `frontend/src/api/trends.ts`:

```typescript
import { api } from './client'
import type { WeeklyTrend, MonthlyTrend, TimeSaved } from '../types'

export function getWeeklyTrends(from?: string, to?: string): Promise<WeeklyTrend[]> {
  const params = new URLSearchParams()
  if (from) params.set('from', from)
  if (to) params.set('to', to)
  const q = params.toString()
  return api.get(`/trends/weekly${q ? '?' + q : ''}`)
}

export function getMonthlyTrends(from?: string, to?: string): Promise<MonthlyTrend[]> {
  const params = new URLSearchParams()
  if (from) params.set('from', from)
  if (to) params.set('to', to)
  const q = params.toString()
  return api.get(`/trends/monthly${q ? '?' + q : ''}`)
}

export function getTimeSaved(): Promise<TimeSaved> {
  return api.get('/trends/time-saved')
}
```

Create `frontend/src/api/insights.ts`:

```typescript
import { api } from './client'
import type { MirrorMoment } from '../types'

export function getMirrorMoments(): Promise<MirrorMoment[]> {
  return api.get('/insights/mirror')
}
```

- [ ] **Step 2: Create TimeSavedCard component**

Create `frontend/src/components/TimeSavedCard.tsx`:

```tsx
import type { TimeSaved } from '../types'
import { getComparison } from '../data/comparisons'

export function TimeSavedCard({ data }: { data: TimeSaved }) {
  const hours = Math.floor(data.total / 60)
  const minutes = data.total % 60

  return (
    <div className="bg-white rounded-lg shadow p-6">
      <p className="text-sm text-gray-500 uppercase tracking-wide">Total time saved</p>
      <p className="text-4xl font-bold text-gray-900 mt-1">
        {hours > 0 && <>{hours}h </>}{minutes}m
      </p>
      <p className="text-gray-500 mt-2">That's {getComparison(data.total)}</p>
    </div>
  )
}
```

- [ ] **Step 3: Create TrendChart component**

Create `frontend/src/components/TrendChart.tsx`:

```tsx
import { AreaChart, Area, XAxis, YAxis, Tooltip, ResponsiveContainer } from 'recharts'
import type { WeeklyTrend } from '../types'
import { getDomain } from '../data/domains'

interface ChartData {
  week: string
  [domain: string]: number | string
}

export function TrendChart({ data }: { data: WeeklyTrend[] }) {
  const domains = [...new Set(data.map(d => d.domain))]

  const byWeek: Record<string, ChartData> = {}
  for (const d of data) {
    if (!byWeek[d.week]) byWeek[d.week] = { week: d.week }
    byWeek[d.week][d.domain] = Math.round(d.totalSeconds / 60)
  }
  const chartData = Object.values(byWeek).sort((a, b) => a.week.localeCompare(b.week))

  if (chartData.length === 0) return null

  return (
    <div className="bg-white rounded-lg shadow p-6">
      <h3 className="text-sm text-gray-500 uppercase tracking-wide mb-4">AI Usage (minutes/week)</h3>
      <ResponsiveContainer width="100%" height={300}>
        <AreaChart data={chartData}>
          <XAxis dataKey="week" tickFormatter={w => new Date(w).toLocaleDateString(undefined, { month: 'short', day: 'numeric' })} />
          <YAxis />
          <Tooltip />
          {domains.map(domain => (
            <Area
              key={domain}
              type="monotone"
              dataKey={domain}
              stackId="1"
              fill={getDomain(domain).color}
              stroke={getDomain(domain).color}
              name={getDomain(domain).name}
            />
          ))}
        </AreaChart>
      </ResponsiveContainer>
    </div>
  )
}
```

- [ ] **Step 4: Create MirrorMomentCard component**

Create `frontend/src/components/MirrorMomentCard.tsx`:

```tsx
import type { MirrorMoment } from '../types'

const TYPE_ICONS: Record<string, string> = {
  weekly_usage: '\u{1F4C8}',
  top_tool: '\u{1F3AF}',
  time_saving_activity: '⏱️',
  busiest_day: '\u{1F4C5}',
}

export function MirrorMomentCard({ moment }: { moment: MirrorMoment }) {
  return (
    <div className="bg-white rounded-lg shadow p-4">
      <span className="text-2xl">{TYPE_ICONS[moment.type] ?? '\u{1F4A1}'}</span>
      <p className="text-gray-700 mt-2 text-sm">{moment.message}</p>
    </div>
  )
}
```

- [ ] **Step 5: Create OnboardingChecklist component**

Create `frontend/src/components/OnboardingChecklist.tsx`:

```tsx
export function OnboardingChecklist() {
  return (
    <div className="max-w-md mx-auto mt-16 text-center">
      <h2 className="text-2xl font-bold text-gray-900">Welcome to rippl</h2>
      <p className="text-gray-500 mt-2">Let's get you set up</p>
      <div className="mt-8 space-y-4 text-left">
        <Step number={1} title="Connect a data source" description="Install the Chrome extension to start tracking your AI usage." />
        <Step number={2} title="Use AI for a day" description="Browse your favorite AI tools. We'll capture your sessions automatically." />
        <Step number={3} title="See your insights" description="Come back here to explore trends, time saved, and mirror moments." />
      </div>
      <a
        href="/settings"
        className="inline-block mt-8 px-6 py-2 bg-indigo-600 text-white rounded-md hover:bg-indigo-700"
      >
        Go to Settings to connect
      </a>
    </div>
  )
}

function Step({ number, title, description }: { number: number; title: string; description: string }) {
  return (
    <div className="flex gap-4 items-start">
      <div className="w-8 h-8 rounded-full bg-indigo-100 text-indigo-600 flex items-center justify-center font-bold text-sm flex-shrink-0">
        {number}
      </div>
      <div>
        <p className="font-medium text-gray-900">{title}</p>
        <p className="text-sm text-gray-500">{description}</p>
      </div>
    </div>
  )
}
```

- [ ] **Step 6: Implement Dashboard page**

Replace `frontend/src/pages/Dashboard.tsx`:

```tsx
import { useEffect, useState } from 'react'
import { getWeeklyTrends, getTimeSaved } from '../api/trends'
import { getMirrorMoments } from '../api/insights'
import type { WeeklyTrend, TimeSaved, MirrorMoment } from '../types'
import { TimeSavedCard } from '../components/TimeSavedCard'
import { TrendChart } from '../components/TrendChart'
import { MirrorMomentCard } from '../components/MirrorMomentCard'
import { OnboardingChecklist } from '../components/OnboardingChecklist'

export function Dashboard() {
  const [trends, setTrends] = useState<WeeklyTrend[]>([])
  const [timeSaved, setTimeSaved] = useState<TimeSaved | null>(null)
  const [insights, setInsights] = useState<MirrorMoment[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    Promise.all([
      getWeeklyTrends(),
      getTimeSaved(),
      getMirrorMoments(),
    ]).then(([t, ts, i]) => {
      setTrends(t)
      setTimeSaved(ts)
      setInsights(i)
    }).finally(() => setLoading(false))
  }, [])

  if (loading) return <div className="text-center py-16 text-gray-500">Loading dashboard...</div>
  if (!timeSaved || timeSaved.total === 0) return <OnboardingChecklist />

  return (
    <div className="space-y-8">
      <TimeSavedCard data={timeSaved} />
      <TrendChart data={trends} />
      {insights.length > 0 && (
        <div>
          <h3 className="text-sm text-gray-500 uppercase tracking-wide mb-4">Mirror Moments</h3>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {insights.map((m, i) => <MirrorMomentCard key={i} moment={m} />)}
          </div>
        </div>
      )}
    </div>
  )
}
```

- [ ] **Step 7: Verify build compiles**

```bash
cd frontend && npm run build
```

Expected: Build succeeds

- [ ] **Step 8: Commit**

```bash
git add frontend/src/api/trends.ts frontend/src/api/insights.ts frontend/src/components/ frontend/src/pages/Dashboard.tsx
git commit -m "feat: add dashboard page with time saved, trend chart, and onboarding"
```

---

## Task 12: Trends + Mirror Pages

**Files:**
- Replace: `frontend/src/pages/Trends.tsx`
- Replace: `frontend/src/pages/Mirror.tsx`

- [ ] **Step 1: Implement Trends page**

Replace `frontend/src/pages/Trends.tsx`:

```tsx
import { useEffect, useState } from 'react'
import { getWeeklyTrends, getMonthlyTrends, getTimeSaved } from '../api/trends'
import type { WeeklyTrend, MonthlyTrend, TimeSaved } from '../types'
import { TrendChart } from '../components/TrendChart'
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, PieChart, Pie, Cell } from 'recharts'
import { getDomain } from '../data/domains'

export function Trends() {
  const [weekly, setWeekly] = useState<WeeklyTrend[]>([])
  const [timeSaved, setTimeSaved] = useState<TimeSaved | null>(null)
  const [view, setView] = useState<'weekly' | 'monthly'>('weekly')
  const [monthly, setMonthly] = useState<MonthlyTrend[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    Promise.all([getWeeklyTrends(), getMonthlyTrends(), getTimeSaved()])
      .then(([w, m, ts]) => { setWeekly(w); setMonthly(m); setTimeSaved(ts) })
      .finally(() => setLoading(false))
  }, [])

  if (loading) return <div className="text-center py-16 text-gray-500">Loading trends...</div>

  const domainData = timeSaved ? Object.entries(timeSaved.byDomain).map(([domain, saved]) => ({
    name: getDomain(domain).name,
    value: saved,
    color: getDomain(domain).color,
  })) : []

  const activityData = timeSaved ? Object.entries(timeSaved.byActivity).map(([activity, saved]) => ({
    name: activity,
    value: saved,
  })) : []

  return (
    <div className="space-y-8">
      <div className="flex gap-2">
        <button onClick={() => setView('weekly')} className={`px-3 py-1 rounded text-sm ${view === 'weekly' ? 'bg-indigo-600 text-white' : 'bg-gray-200'}`}>Weekly</button>
        <button onClick={() => setView('monthly')} className={`px-3 py-1 rounded text-sm ${view === 'monthly' ? 'bg-indigo-600 text-white' : 'bg-gray-200'}`}>Monthly</button>
      </div>

      <TrendChart data={view === 'weekly' ? weekly : monthly.map(m => ({ week: m.month, domain: m.domain, totalSeconds: m.totalSeconds, totalSaved: m.totalSaved }))} />

      {domainData.length > 0 && (
        <div className="bg-white rounded-lg shadow p-6">
          <h3 className="text-sm text-gray-500 uppercase tracking-wide mb-4">Time saved by tool</h3>
          <ResponsiveContainer width="100%" height={250}>
            <BarChart data={domainData} layout="vertical">
              <XAxis type="number" unit=" min" />
              <YAxis type="category" dataKey="name" width={100} />
              <Tooltip />
              <Bar dataKey="value" name="Minutes saved">
                {domainData.map((d, i) => <Cell key={i} fill={d.color} />)}
              </Bar>
            </BarChart>
          </ResponsiveContainer>
        </div>
      )}

      {activityData.length > 0 && (
        <div className="bg-white rounded-lg shadow p-6">
          <h3 className="text-sm text-gray-500 uppercase tracking-wide mb-4">What you use AI for</h3>
          <ResponsiveContainer width="100%" height={250}>
            <PieChart>
              <Pie data={activityData} dataKey="value" nameKey="name" cx="50%" cy="50%" outerRadius={90} label={({ name, percent }) => `${name} ${(percent * 100).toFixed(0)}%`}>
                {activityData.map((_, i) => <Cell key={i} fill={['#6366F1', '#10B981', '#F59E0B', '#EF4444'][i % 4]} />)}
              </Pie>
              <Tooltip />
            </PieChart>
          </ResponsiveContainer>
        </div>
      )}
    </div>
  )
}
```

- [ ] **Step 2: Implement Mirror page**

Replace `frontend/src/pages/Mirror.tsx`:

```tsx
import { useEffect, useState } from 'react'
import { getMirrorMoments } from '../api/insights'
import type { MirrorMoment } from '../types'
import { MirrorMomentCard } from '../components/MirrorMomentCard'

export function Mirror() {
  const [moments, setMoments] = useState<MirrorMoment[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    getMirrorMoments()
      .then(setMoments)
      .finally(() => setLoading(false))
  }, [])

  if (loading) return <div className="text-center py-16 text-gray-500">Loading insights...</div>

  if (moments.length === 0) {
    return (
      <div className="text-center py-16">
        <p className="text-gray-500">Not enough data yet for mirror moments.</p>
        <p className="text-gray-400 text-sm mt-2">Keep using AI tools and check back soon.</p>
      </div>
    )
  }

  return (
    <div>
      <h2 className="text-lg font-semibold text-gray-900 mb-6">Mirror Moments</h2>
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {moments.map((m, i) => <MirrorMomentCard key={i} moment={m} />)}
      </div>
    </div>
  )
}
```

- [ ] **Step 3: Verify build compiles**

```bash
cd frontend && npm run build
```

Expected: Build succeeds

- [ ] **Step 4: Commit**

```bash
git add frontend/src/pages/Trends.tsx frontend/src/pages/Mirror.tsx
git commit -m "feat: add trends page with charts and mirror moments page"
```

---

## Task 13: Settings Page

**Files:**
- Create: `frontend/src/api/collectors.ts`, `frontend/src/api/account.ts`
- Create: `frontend/src/components/CollectorCard.tsx`
- Replace: `frontend/src/pages/Settings.tsx`

- [ ] **Step 1: Create API modules**

Create `frontend/src/api/collectors.ts`:

```typescript
import { api } from './client'
import type { CollectorInfo } from '../types'

export function getCollectors(): Promise<CollectorInfo[]> {
  return api.get('/collectors')
}

export function addCollector(type: string): Promise<CollectorInfo> {
  return api.post('/collectors', { type })
}

export function removeCollector(id: string): Promise<void> {
  return api.delete(`/collectors/${id}`)
}
```

Create `frontend/src/api/account.ts`:

```typescript
import { api } from './client'

export function deleteAccount(): Promise<void> {
  return api.delete('/account')
}
```

- [ ] **Step 2: Create CollectorCard component**

Create `frontend/src/components/CollectorCard.tsx`:

```tsx
import type { CollectorInfo } from '../types'

interface Props {
  collector: CollectorInfo
  onRemove: (id: string) => void
}

const TYPE_LABELS: Record<string, string> = {
  chrome_extension: 'Chrome Extension',
}

export function CollectorCard({ collector, onRemove }: Props) {
  return (
    <div className="flex items-center justify-between bg-white rounded-lg shadow p-4">
      <div>
        <p className="font-medium text-gray-900">{TYPE_LABELS[collector.type] ?? collector.type}</p>
        <p className="text-sm text-gray-500">
          Connected {new Date(collector.linkedAt).toLocaleDateString()}
        </p>
      </div>
      <button
        onClick={() => onRemove(collector.id)}
        className="text-red-600 hover:text-red-800 text-sm"
      >
        Remove
      </button>
    </div>
  )
}
```

- [ ] **Step 3: Implement Settings page**

Replace `frontend/src/pages/Settings.tsx`:

```tsx
import { useEffect, useState } from 'react'
import { getCollectors, addCollector, removeCollector } from '../api/collectors'
import { deleteAccount } from '../api/account'
import type { CollectorInfo } from '../types'
import { CollectorCard } from '../components/CollectorCard'
import { useAuth } from '../hooks/useAuth'

export function Settings() {
  const { user } = useAuth()
  const [collectors, setCollectors] = useState<CollectorInfo[]>([])
  const [loading, setLoading] = useState(true)
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false)

  useEffect(() => {
    getCollectors().then(setCollectors).finally(() => setLoading(false))
  }, [])

  const handleAddExtension = async () => {
    const c = await addCollector('chrome_extension')
    setCollectors(prev => [...prev, c])
  }

  const handleRemove = async (id: string) => {
    await removeCollector(id)
    setCollectors(prev => prev.filter(c => c.id !== id))
  }

  const handleDelete = async () => {
    await deleteAccount()
    window.location.href = '/login'
  }

  return (
    <div className="space-y-10">
      <section>
        <h2 className="text-lg font-semibold text-gray-900 mb-4">Data Sources</h2>
        {loading ? (
          <p className="text-gray-500">Loading...</p>
        ) : (
          <div className="space-y-3">
            {collectors.map(c => (
              <CollectorCard key={c.id} collector={c} onRemove={handleRemove} />
            ))}
            {!collectors.some(c => c.type === 'chrome_extension') && (
              <button
                onClick={handleAddExtension}
                className="w-full py-3 border-2 border-dashed border-gray-300 rounded-lg text-gray-500 hover:border-indigo-400 hover:text-indigo-600"
              >
                + Connect Chrome Extension
              </button>
            )}
          </div>
        )}
      </section>

      <section>
        <h2 className="text-lg font-semibold text-gray-900 mb-2">Account</h2>
        <p className="text-sm text-gray-500 mb-2">Signed in as {user?.email}</p>
        {showDeleteConfirm ? (
          <div className="bg-red-50 p-4 rounded space-y-3">
            <p className="text-red-700 text-sm font-medium">
              This will permanently delete your account and all data. This cannot be undone.
            </p>
            <div className="flex gap-2">
              <button onClick={handleDelete} className="px-4 py-2 bg-red-600 text-white rounded text-sm hover:bg-red-700">
                Yes, delete everything
              </button>
              <button onClick={() => setShowDeleteConfirm(false)} className="px-4 py-2 bg-gray-200 rounded text-sm">
                Cancel
              </button>
            </div>
          </div>
        ) : (
          <button
            onClick={() => setShowDeleteConfirm(true)}
            className="text-red-600 hover:text-red-800 text-sm"
          >
            Delete my account
          </button>
        )}
      </section>
    </div>
  )
}
```

- [ ] **Step 4: Verify build compiles**

```bash
cd frontend && npm run build
```

Expected: Build succeeds

- [ ] **Step 5: Commit**

```bash
git add frontend/src/api/collectors.ts frontend/src/api/account.ts frontend/src/components/CollectorCard.tsx frontend/src/pages/Settings.tsx
git commit -m "feat: add settings page with collectors and account deletion"
```

---

## Task 14: SPA Routing + CORS + Dockerfile

**Files:**
- Create: `backend/src/main/kotlin/app/rippl/SpaForwardController.kt`
- Create: `backend/src/main/kotlin/app/rippl/CorsConfig.kt`
- Create: `Dockerfile`
- Create: `.dockerignore`

- [ ] **Step 1: Implement SPA catch-all controller**

Create `backend/src/main/kotlin/app/rippl/SpaForwardController.kt`:

```kotlin
package app.rippl

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod

@Controller
class SpaForwardController {

    @RequestMapping(
        value = ["/", "/login", "/trends", "/mirror", "/settings"],
        method = [RequestMethod.GET]
    )
    fun forward() = "forward:/index.html"
}
```

- [ ] **Step 2: Add profile-based CORS config**

Create `backend/src/main/kotlin/app/rippl/CorsConfig.kt`:

```kotlin
package app.rippl

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
@Profile("dev")
class CorsConfig {

    @Bean
    fun corsConfigurer() = object : WebMvcConfigurer {
        override fun addCorsMappings(registry: CorsRegistry) {
            registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:5173")
                .allowedMethods("GET", "POST", "DELETE")
                .allowCredentials(true)
        }
    }
}
```

- [ ] **Step 3: Create Dockerfile**

Create `Dockerfile`:

```dockerfile
# Stage 1: Build frontend
FROM node:22-alpine AS frontend
WORKDIR /app/frontend
COPY frontend/package*.json ./
RUN npm ci
COPY frontend/ .
RUN npm run build

# Stage 2: Build backend
FROM gradle:8.10-jdk21 AS backend
WORKDIR /app
COPY settings.gradle.kts .
COPY backend/ backend/
COPY --from=frontend /app/frontend/dist backend/src/main/resources/static/
WORKDIR /app/backend
RUN gradle bootJar --no-daemon -x test

# Stage 3: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=backend /app/backend/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

Create `.dockerignore`:

```
.git
.idea
.gradle
build
node_modules
frontend/node_modules
frontend/dist
*.md
.claude
docs
```

- [ ] **Step 4: Verify Docker build works**

```bash
docker build -t rippl-dashboard .
```

Expected: Build succeeds. (No need to run — no DB yet in Docker.)

- [ ] **Step 5: Run full backend test suite one final time**

```bash
./gradlew :backend:test
```

Expected: ALL PASS

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/kotlin/app/rippl/SpaForwardController.kt backend/src/main/kotlin/app/rippl/CorsConfig.kt Dockerfile .dockerignore
git commit -m "feat: add SPA routing, CORS config, and Dockerfile"
```
