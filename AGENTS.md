# AGENTS.md

## Layout

- `frontend/` — Angular 20 + Tailwind 3 + Karma/Jasmine. Dev server :4200.
- `backend/backend/` — Spring Boot 4.1.2 (Java 17 target, Maven wrapper). **Maven root is the doubly-nested `backend/backend/`, not `backend/`.** Port 8080.
- `restaurant.db` (repo root) — SQLite DDL reference dump; **not used at runtime** (backend uses PostgreSQL). Safe to ignore.
- Root `package-lock.json` is a stray empty lockfile; there is no root package.json. Do npm work only inside `frontend/`.
- No CI workflows, no ESLint, no lint script anywhere.

## Commands (Windows host)

- Backend: run **from `backend/backend`** with `.\mvnw.cmd` (e.g. `.\mvnw.cmd spring-boot:run`, `.\mvnw.cmd test`).
- Frontend: use `npm.cmd`, not `npm` — PowerShell execution policy blocks `npm.ps1` in this environment.
- Single backend test: `.\mvnw.cmd test "-Dtest=PlatoCategoriaCrudTest"` (from `backend/backend`).
- Frontend `npm test` is Karma **watch mode** (hangs). One-shot: `npx ng test --watch=false --browsers=ChromeHeadless`.
- Verification order: backend `.\mvnw.cmd test` → frontend `npm.cmd run build` (Angular strict templates act as typecheck) → `npm.cmd test` if specs changed.

## Backend gotchas

- **Integration tests need a real local PostgreSQL.** `BasePruebaIntegracion` (`src/test/.../BasePruebaIntegracion.java`) hardcodes `postgres` credentials (in `@BeforeAll` and `src/test/resources/application-test.properties`) that are **duplicated from the dev password** in `src/main/resources/application.properties` — same value in two test-only places, keep them in sync if the local DB password rotates. The hardcoded value is used only to create/verify the isolated DB; the test datasource itself is always `bd_restaurant_test`. Tests auto-create DB `bd_restaurant_test` and assert the datasource URL contains it; they refuse to run against `bd_Restaurant`. All suites extend `BasePruebaIntegracion` — extend it for new tests (profile `test`, MockMvc with security filters off, data cleaned after each test). The `@AfterEach` cleanup must use `deleteAllInBatch()` for `usuario`/`persona`: plain `deleteAll()` loads both entities into one session and Hibernate's `CHECK_ON_FLUSH` throws `TransientPropertyValueException` (the inverse `Persona.usuario` points at the removed entity).
- Security is configured in `config/SecurityConfig.java`: `SecurityFilterChain` with stateless JWT (`JwtFiltroAutenticacion`), BCrypt encoder, roles `ADMINISTRADOR`/`MOZO`/`CAJERO`. **There is no public registration**: only `POST /api/auth/login` is public; user creation lives at `/api/usuarios` (ADMINISTRADOR only, see the `requestMatchers` matrix) and the first-admin bootstrap is documented in `docs/primer-administrador.md`. Tests that need the real security chain use `@AutoConfigureMockMvc(addFilters = true)` (see `LoginSeguridadTest` and `UsuarioGestionTest`); the base class keeps `addFilters = false`. JWT settings (`jwt.secret`, `jwt.expiracion-minutos`) are in `application.properties`. `config/DevDataSeeder.java` seeds fixed dev users but only under profile `dev` (never active by default; keep it off in production).
- No Flyway/Liquibase: schema changes go through JPA entities + `spring.jpa.hibernate.ddl-auto=update`.
- `application.properties` carries plaintext local DB credentials and is currently modified in the working tree (uncommitted password change).
- Package layout under `com.elhornero.backend`: `controller` → `service` → `repository`, plus `dto`, `entity`, `exception` (`GlobalExceptionHandler`). REST paths are Spanish plurals: `/api/platos`, `/api/insumos`, `/api/pedidos`, …

## Frontend gotchas

- **The frontend makes zero HTTP calls today** — no `HttpClient`, no environment files, no proxy config. Views are standalone components using signals + local mock data. Wiring to the backend will also require adding CORS (backend has none) or a dev-server proxy.
- Strict TS (`strict`, `strictTemplates`, `noPropertyAccessFromIndexSignature`) in `tsconfig.json`.
- Prettier settings live in `frontend/package.json` (printWidth 100, single quotes, angular parser for HTML) but **prettier is not installed** — no format command exists.
- Tailwind uses **v3 directives** (`@tailwind base;…`) and a custom theme (colors `cream`/`ink`/`terra`, `side` breakpoint) in `tailwind.config.js` — reuse those tokens instead of raw hex.
- Routes are declared centrally in `src/app/app.routes.ts`; the sidebar in `src/app/shared/sidebar/sidebar.component.ts` lists items that have no route yet (e.g. Reportes).
- Component file naming is inconsistent: `salon.component.ts` / `sidebar.component.ts` vs `pedidos.ts` / `inventario.ts`. Match the pattern of the feature you edit.

## Conventions

- Codebase language is **Spanish**: comments, exception messages, test names, UI copy, and domain terms (plato, insumo, merma, comprobante). Keep new code/tests/commits in Spanish to match.
- Commit style: conventional commits with Spanish subjects (`feat(frontend): módulo de inventario`). Default branch is `master`; changes arrive via PRs from forks.
