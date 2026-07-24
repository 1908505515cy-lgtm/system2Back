# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

System2 is an AI-driven general management platform. It uses metadata-driven architecture with annotations (`@ModuleMeta`, `@FieldMeta`) to auto-register modules and generate CRUD UIs. An AI Agent enables natural language operations over the data.

## Repository Structure

Three independent services communicating over HTTP:

- **`springboot/`** - Java 21 backend (Spring Boot 3.2.5, MyBatis-Plus, MySQL). Port 9096.
- **`vue/`** - Vue 3 frontend (Vite 7, Element Plus, ECharts). Port 5174.
- **`pythonAI/`** - Python AI microservice (FastAPI, Ollama gemma4:latest). Port 8081.
- **`sql/`** - Database schema scripts (`phase3_tables.sql`).

```
Vue SPA (5174) ──HTTP──→ Spring Boot (9096) ──→ MySQL (3306)
     │
     └──SSE──→ Python AI (8081) ──→ Ollama (11434)
                    └──callback──→ Spring Boot (9096)
```

## Common Commands

### Frontend (from `vue/`)
```bash
npm install          # Install dependencies
npm run dev          # Start Vite dev server (port 5174)
npm run build        # Production build
npm run preview      # Preview production build
npm run lint         # ESLint auto-fix
npm run format       # Prettier format
```

### Backend (from `springboot/`)
```bash
# Note: Maven may not be in PATH. Use full path if needed: D:\develop\apache-maven-3.6.3\apache-maven-3.6.3\apache-maven-3.6.3\bin\mvn.cmd
mvn spring-boot:run  # Start backend server (port 9096)
mvn compile          # Compile only
mvn test             # Run tests
mvn package          # Build JAR
```

### Python AI (from `pythonAI/`)
```bash
pip install -r requirements.txt   # fastapi, uvicorn, ollama, httpx, pydantic, python-dotenv
python main.py                    # Start FastAPI with hot reload (port 8081)
# Windows: start.bat              # Kill port 8081 and restart
```

### Database
```bash
mysql -u root -p < sql/phase3_tables.sql   # Initialize schema
```
Database name: `system2`

### Docker (from root)
```bash
docker-compose up -d           # Start all services (MySQL, Ollama, Spring Boot, Python AI, Vue)
docker-compose down            # Stop all services
docker-compose logs -f         # Follow logs
docker-compose up -d --build   # Rebuild and start
```
Vue is served on port 80 in Docker mode. In Docker, frontend uses relative URLs (empty `VITE_BASE_URL`) and nginx proxies API calls to backend services.

## Backend Architecture: Generic CRUD Pattern

The core design pattern is **metadata-driven generic CRUD**. New modules are created by defining an entity with annotations and extending generic base classes -- no per-module controller/service boilerplate needed.

### Annotations

**`@ModuleMeta`** on entity class:
- `name()` -- module identifier (English, used for routing/API path)
- `title()` -- display name (Chinese)
- `icon()` -- Element Plus icon name (default `"User"`)
- `features()` -- comma-separated feature flags (default `"statusToggle,batchDelete"`). Known values: `statusToggle`, `batchDelete`, `resetPassword`

**`@FieldMeta`** on entity fields:
- `label()` -- display name
- `searchable()` -- participates in keyword search (default `false`)
- `showInTable()` / `showInForm()` -- visibility flags (default `true`)
- `type()` -- input type: `text | number | select | date | switch | textarea | password | tag`
- `required()` -- frontend validation (default `false`)
- `options()` -- inline options: `"label1:value1,label2:value2"`
- `dictCode()` -- loads options from `sys_dict_data` table (takes priority over `options`)
- `disabledOnEdit()` -- read-only in edit mode (default `false`)
- `width()` -- table column width in pixels (0 = auto)

### Key Classes (`com.example.common`)

- `ModuleRegistry` - Scans `com.example.entity` at startup, builds `ConcurrentHashMap<String, ModuleInfo>`. Recursively scans parent classes for `@FieldMeta` (important for entity inheritance like `Account` → `Admin`). Calls `resolveDictOptions()` to hydrate dict options from `sys_dict_data`.
- `GenericController<E,D,V>` - Abstract base with standard CRUD endpoints (add, update, delete, getById, page, updateStatus, updateField, batchDelete) plus recycle bin endpoints (trash, restore, permanentDelete, batchRestore, batchPermanentDelete). `MAX_PAGE_SIZE` = 100, `MAX_BATCH_SIZE` = 100.
- `GenericServiceImpl<E,D,V>` - Service impl with template hooks
- `GenericService<E,D,V>` - Interface including recycle bin methods

**Template method hooks** (subclasses override for custom behavior):
- `beforeAdd(D dto)` -- pre-insert validation (e.g., uniqueness checks)
- `afterAdd(E entity)` -- post-insert callback
- `beforeUpdate(D dto)` -- pre-update validation
- `toEntity(D dto)` / `toVo(E entity)` -- DTO conversion (default: `BeanUtils.copyProperties`)
- `buildKeywordCondition(QueryWrapper, keyword)` -- custom search fields
- `getOrderColumn()` -- sort column (default `"create_time"`)
- `buildTrashKeywordCondition(whereClause, params, keyword)` -- custom recycle bin search fields

**AI-specific methods** (called by `AiDispatchService`):
- `createFromMap(Map)` -- insert from arbitrary field map (whitelist-validated)
- `queryByFields(Map)` -- exact-match query (whitelist-validated, prevents SQL injection)

**Security:** `SENSITIVE_FIELDS` set blocks `updateField` from modifying `password`, `deleted`, `roleIds`, `role`, `accountCode`. Rate limiter on `/login` (10 req/min per IP).

**Interceptor chain** (registered in `WebMvcConfig`):
1. `RateLimitInterceptor` → `/login` only (10 req/60s per IP)
2. `LoginInterceptor` → `/**` (excludes `/login`, `/refresh`, `/test/**`, `/admin/ai-update-name`, `/module/**`, `/swagger-ui/**`, `/v3/api-docs/**`)
3. `ModulePermInterceptor` → `/**` with `WHITELIST_PREFIXES`: `/login`, `/module`, `/ai`, `/test`, `/home`, `/AIchat`, `/front`, `/chat`, `/dashboard`, `/export`, `/refresh`, `/logout`. Extracts module name from first URL segment, checks `RoleService.getUserModulePerms(roleIds)`. Empty perms = full access.

**Audit logging:** `AuditAspect` (AOP) intercepts all mutating controller methods (`add`, `update`, `delete`, `batchDelete`, `updateStatus`, `updateField`, `resetPassword`, `confirmAiAction`). Logs operator, action, module, recordId, detail (passwords redacted), and IP to `sys_audit_log`.

**Auth:**
- BCrypt password hashing (spring-security-crypto, not full Spring Security)
- JWT with refresh token: `/login` returns `{token, refreshToken, user}`, `/refresh` accepts `{refreshToken}` to get a new token
- Token blacklisting on `/logout` via Caffeine cache (24h TTL, max 10000 entries)
- SpringDoc OpenAPI at `/swagger-ui.html`

### To Add a New Module

1. Create entity in `com.example.entity` with `@ModuleMeta` + `@FieldMeta` + `@TableName`
2. Create mapper extending `BaseMapper<Entity>`
3. Create service extending `GenericServiceImpl<Entity, Dto, Vo>`
4. Create controller extending `GenericController<Entity, Dto, Vo>` with `@RequestMapping`
5. Module auto-registers and gets a full CRUD UI -- no other wiring needed

## Frontend Architecture

- **Dynamic module rendering:** Route `/manager/:module` maps to `GenericCrud.vue`, which fetches module config from backend `/module/list` and renders search, table, pagination, and CRUD dialogs dynamically. The component is entirely driven by `ModuleConfig` -- no hardcoded field names.
- **Module config conversion:** `utils/moduleConverter.ts` transforms backend `ModuleInfoDto` into frontend `ModuleConfig` (columns, searchFields, formFields, features).
- **Local overrides:** `config/modules.ts` allows per-module frontend customization via `localOverrides` record. Module configs are cached with 5-minute TTL. Add an entry to customize any module's UI without touching the backend.
- **API layer:** `utils/crudApi.ts` is a factory (`createCrudApi(base)`) that generates all CRUD methods (including recycle bin: trash, restore, permanentDelete, batchRestore, batchPermanentDelete) from an `apiBase` URL. Domain-specific typed APIs live in `api/manager/`.
- **Auth:** JWT + refresh token stored in localStorage, injected via axios interceptor. On 401, the interceptor attempts token refresh via `/refresh` and queues failed requests during the refresh. On persistent failure, clears storage and redirects to `/login`. Route guards enforce module-level permissions: `user.modulePerms` from localStorage is checked against the `:module` route param; non-empty perms array blocks unauthorized modules.
- **Path alias:** `@/*` maps to `./src/*`.
- **Store:** Single Pinia store (`stores/user.ts`) for token/refreshToken/userInfo. No global module state.
- **Theme:** SCSS customization via `unplugin-element-plus`, primary color `#5f56e7` in `assets/css/index.scss`.
- **Lint/Format:** ESLint (`npm run lint`) and Prettier (`npm run format`) configured. Node engine: `^20.19.0 || >=22.12.0`.

## AI Integration Flow

1. User sends natural language → Vue `AIchat.vue` page
2. Frontend calls Python FastAPI `/api/v1/algo/chat/stream` directly via `fetch()` SSE (not through Spring Boot). The SSE composable is `composables/useSSE.ts`.
3. Python service dispatch: regex fast-path (rename commands) → obvious-chat fast-path → full LLM tool selection via Ollama
4. **7 tools** in `pythonAI/app/core/tool_registry.py`: `create_entity`, `query_entity`, `update_entity_field`, `update_entity_status`, `delete_entity`, `analyze_data`, `chat`
5. Tools with `requires_confirm: true` (all write ops) return `CONFIRM_REQUIRED` JSON → frontend renders confirmation card → user confirms → frontend POSTs to Spring Boot `/ai/execute`
6. `AiDispatchService` resolves the target `GenericService` by trying bean name `entityType + "ServiceImpl"` first, then `entityType + "Service"`, then case-insensitive prefix match
7. Python AI loads module metadata from Spring Boot `GET /module/list` at startup for LLM system prompt context

**Adding a new AI tool:** Append to `TOOLS` list in `pythonAI/app/core/tool_registry.py`. The LLM prompt and dispatch logic read from this list dynamically. Set `requires_confirm: true` for any write operation.

**SSE event types:** `start` (with `intent` field), `token` (incremental text), `json` (structured response), `error`, `[DONE]`. Heartbeat comments (`: heartbeat\n\n`) sent every 15 seconds during streaming.

**Error resilience:** LLM failures silently degrade to `chat` tool. All SSE streams terminate with `[DONE]` even on error.

**Python→Java callback:** Write operations use `java_client.py` to POST to Spring Boot `/ai/execute` with `{entityType, action, params}`.

**Non-streaming endpoint:** `POST /analyze` is a non-streaming alternative used by Spring Boot's `AiDispatchService.chat()` proxy method.

**Chat history:** `ChatController` (`/chat`) provides session/message CRUD, persisted in Spring Boot. Frontend `AIchat.vue` manages sessions (create, switch, delete) and passes last 10 messages as history to the SSE stream.

## Key Configuration Files

| File | Notes |
|---|---|
| `.env.example` | **Primary env template** -- copy to `.env` and fill in values for all services |
| `springboot/src/main/resources/application.yml` | Port 9096, MySQL, JWT secret, AI callback secret |
| `vue/.env.development` | `VITE_BASE_URL='http://localhost:9096'` (Spring Boot), `VITE_AI_BASE_URL='http://localhost:8081'` (Python AI) |
| `vue/vite.config.js` | Element Plus auto-import, SCSS theming, `@/` alias |
| `pythonAI/app/core/config.py` | Ollama model, URLs, callback secret (all env-overridable) |
| `docker-compose.yml` | Full-stack orchestration with health checks |

## Codebase Notes

- **Mixed JS/TS:** Frontend is migrating toward TypeScript. API layers, types, and utils use `.ts`; core files (main.js, router) still `.js`.
- **Inter-service auth:** Spring Boot and Python AI share a callback secret (`System2AiCallbackSecret2026` in application.yml and config.py).
- **Logical delete:** All entities use `deleted` field (1/0), configured via MyBatis-Plus `@TableLogic`. Recycle bin endpoints (`/trash`, `/restore/{id}`, `/permanent-delete/{id}`) operate on logically deleted records.
- **Entity inheritance:** `Account` is a base entity (no `@TableName` or `@ModuleMeta`) with shared fields (id, username, password, realName, avatar, gender, mobile, email, status, role, remark, timestamps, deleted, accountCode, lastLoginTime, lastLoginIp, loginCount). `Admin` extends it with `@SuperBuilder` and adds `adminCode`, `deptId`, `roleIds`. Use `@SuperBuilder` on both parent and child when extending entities. `ModuleRegistry` recursively scans parent classes for `@FieldMeta`.
- **Module metadata endpoint:** `/module/list` and `/module/info/{name}` are excluded from login interceptor (publicly accessible for AI service startup and frontend module loading).
- **Key libraries:** Hutool (backend utils), EasyExcel (import/export), Caffeine (token blacklist cache), ECharts (frontend charts).
- **Python AI dispatch fast-paths:** Regex patterns intercept rename commands before LLM. Short messages without action keywords are routed directly to `chat` tool. LLM uses temperature 0.0 for tool selection, 0.7 for chat generation.
