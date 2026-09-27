# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

Personal finance management system (记账 / 理财 / 统计 / 备忘 / AI 顾问). The root is a monorepo of three independent projects that share one backend API:

- `FinanceManagement/` — Spring Boot 3 backend (Java 17, Maven). The single source of truth for all business logic and data.
- `FinanceManagementVue/` — Vue 3 + TypeScript + Vite web SPA (user + admin console).
- `FinanceManagementUniApp/` — uni-app mobile client (compiled to WeChat mini-program, `mp-weixin`), user-side features only.

Root docs: `个人财务管理项目文档.md` (project overview), `从零部署完整手册.md` (CentOS + Nginx deployment). Backend also has `docs/API接口详细文档.md` (full API reference) and `docs/SpringSecurity + JWT + Redis 整套框架 从登录到鉴权全流程.md`.

## Commands

### Backend (`FinanceManagement/`)

Requires JDK 17, MySQL 8 (`finance_management` database), and Redis. Run `sql/init.sql` first to create the schema and seed data.

```bash
mvn spring-boot:run                          # dev (profile "dev" is the default)
mvn clean package -DskipTests                # build jar
java -jar target/FinanceManagement-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
mvn test                                     # run tests (only a context-load test exists)
mvn test -Dtest=FinanceManagementApplicationTests   # single test
./mvnw.cmd                                   # Maven wrapper (Windows); ./mvnw on Unix
```

Default port `8080`, base path `/api/v1`.

### Web frontend (`FinanceManagementVue/`)

Requires Node `^20.19.0 || >=22.12.0`.

```bash
npm install
npm run dev            # dev server on http://localhost:5173
npm run type-check     # vue-tsc --build
npm run build          # runs type-check + vite build
npm run preview
```

### UniApp (`FinanceManagementUniApp/`)

`package.json` has no build scripts (only the `pinia` dependency). This project is run from HBuilderX (run → 微信小程序), which produces `unpackage/dist/dev/mp-weixin/`. There is no CLI build command checked in.

## Architecture

### Backend layering

Conventional three-tier, split by feature module under `src/main/java/com/finance/`:

- `modules/<feature>/` — `auth`, `bill`, `plan`, `statistics`, `memo`, `ai`, `admin`. Each contains `controller` → `service` (interface + `impl/`) → `mapper` (MyBatis-Plus, no XML for most queries) plus `entity` and `dto` (request/VO classes).
- `common/` — `config/` (CORS, MyBatis-Plus, Redis, WebMvc), `exception/` (`BusinessException` + `GlobalExceptionHandler`), `result/` (`Result<T>`, `PageResult`).
- `security/` — `SecurityConfig`, `JwtAuthenticationFilter`, `JwtTokenProvider`, `UserDetailsServiceImpl`, `CustomAccessDeniedHandler`.
- `util/` — `SecurityUtil` (current user id), `RedisUtil`, `CacheClient`, `RedisData`.

### Response contract

Every endpoint returns `Result<T>` = `{ code, message, data }` where `code == 200` means success. The Vue Axios interceptor ([request.ts](FinanceManagementVue/src/api/request.ts)) unwraps `data` on `200` and maps HTTP `401`/`403`/`500`/`503` to logout/error toasts. When adding an endpoint, return `Result` from the controller — do not throw HTTP errors for business failures, throw `BusinessException`.

### Auth & data isolation (critical to preserve)

- **JWT + Redis dual check.** Login writes the JWT into Redis (`token:user:{userId}`, 7-day TTL). `JwtAuthenticationFilter` validates both the JWT signature *and* that the token still exists in Redis, so tokens can be forcibly invalidated (password change, ban). Logging in on a second device kicks the first.
- **Horizontal isolation.** User endpoints must obtain the caller via `SecurityUtil.getCurrentUserId()` and filter by `eq(userId, ...)`. Never accept a userId from the request body/path for user-scoped resources.
- **RBAC.** Two roles (`USER`, `ADMIN`); admin endpoints under `/admin/**` require `hasRole("ADMIN")`. The frontend mirrors this with `meta.role` + `router.beforeEach` guards.

### Configuration

- `src/main/resources/application.yml` sets `spring.profiles.active` (`dev` by default) and imports a `.env` file via `spring.config.import`. `application-dev.yml` / `application-prod.yml` reference secrets as `${DB_PASSWORD}`, `${REDIS_PASSWORD}`, `${JWT_SECRET}`, `${AI_API_KEY}` — copy `.env.example` → `.env` (gitignored) to supply them. Missing values fail startup.
- Notable dev/prod differences: dev enables SQL logging and uses Redis `database: 1`; prod uses `database: 0`, `com.finance: info`, no SQL logging.
- MyBatis-Plus logical delete uses field `deleted` (`1` = deleted). Entities with a `deleted` column are soft-deleted.
- AI chat calls SiliconFlow (DeepSeek R1) with an OpenAI-compatible client; disable via `ai.enabled: false`.

### Frontend structure (`FinanceManagementVue/`)

- `src/api/request.ts` — single Axios instance; base URL comes from `VITE_API_BASE_URL` in `.env` (defaults to `http://localhost:8080/api/v1`; no Vite dev proxy is configured).
- `src/api/modules/*.ts` — one file per backend module (`auth`, `bill`, `financePlan`, `statistics`, `memo`, `ai`, `admin`).
- `src/router/index.ts` — routes + guards; `src/stores/` — Pinia `auth` + `app`; `src/types/index.ts` — shared TypeScript types.
- Views map to routes in `views/<feature>/`. Charts use ECharts via `vue-echarts`.

### UniApp (`FinanceManagementUniApp/`)

- `utils/request.js` — `uni.request` wrapper; base URL from `VITE_API_BASE_URL` in `.env` (defaults to `http://localhost:8080/api/v1`). `api/index.js` defines user-end endpoints only (no admin).
- `store/auth.js` — Pinia auth store; `pages.json` declares routes and the tab bar. Pages live in `pages/<feature>/`.

## Gotchas

- Secrets and per-environment values live in gitignored `.env` files (backend `FinanceManagement/.env`, web `FinanceManagementVue/.env`, uni-app `.env`); committed `.env.example` files are the templates. Copy `.env.example` → `.env` before running.
- The AI `api-key` and prod DB password were previously committed to the backend repo's git history — rotate them (the history still contains them).
- For a device/remote backend, the clients must point to a reachable host (`.env` `VITE_API_BASE_URL`) or use a reverse proxy.
- `sql/init.sql` inserts a placeholder BCrypt hash for the admin account (`admin` / `admin123`) — generate a real hash with `BCryptPasswordEncoder.encode(...)` before use.
- Backend tests are minimal (a single `@SpringBootTest` context load); there is no test suite to rely on for regression.
