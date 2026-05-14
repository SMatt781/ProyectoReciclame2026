# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

### Run the application
```bash
./mvnw spring-boot:run
# Windows:
mvnw.cmd spring-boot:run
```
App runs at `http://localhost:8080`. Root `/` redirects to `/login`.

### Build JAR
```bash
./mvnw package
java -jar target/ProyectoReciclame-0.0.1-SNAPSHOT.jar
```

### Run tests
```bash
./mvnw test
```

### Tailwind CSS
```bash
npm run dev          # watch mode (development)
npm run build        # minified build (before commit)
```
Input: `input.css` → Output: `src/main/resources/static/css/output.css`. Run the build when changing Tailwind classes; the compiled output is committed to the repo.

### Database setup
Before first run, execute the SQL schema manually in MySQL:
```sql
source reciclame_20-04.sql;
```
This drops and recreates database `reciclame`. JPA DDL mode is `none` — Hibernate never modifies the schema.

## Prerequisites

- JDK 17
- MySQL 8.0 on `localhost:3306`, credentials `root` / `12345678` (see `application.properties`)
- Node.js (for Tailwind CSS builds)

## Architecture

**Spring Boot 4.x MVC app** — server-side rendered with Thymeleaf. Not a REST API; controllers return view names.

### Layer structure (`com.example.proyectoreciclame`)

| Package | Purpose |
|---|---|
| `Controller/` | `@Controller` classes; return Thymeleaf view names |
| `Service/` | Business logic |
| `Repository/` | Spring Data JPA interfaces (`JpaRepository`) |
| `Entity/` | JPA `@Entity` classes (Jakarta Persistence) |
| `Dto/` | Form objects and view projections |
| `Security/` | Spring Security config and custom handlers |

### Global model injection
`GlobalUserModelAdvice` is a `@ControllerAdvice` that injects three attributes into **every** Thymeleaf model automatically: `sessionUser` (a `SessionUserDto`), `notificacionesNoLeidas` (unread count), and `usuarioRol`. Every template can use these without the controller adding them explicitly.

### Security
- Form login with email field named `correo` (not `username`)
- `CustomUserDetailsService` rejects users where `estadoAprobacion != APROBADO` or `estadoCuenta != ACTIVO`
- Role-based routing: `/admin/**` → ADMIN/SUPERADMIN, `/socio/**` → SOCIO, `/visualizador/**` → VISUALIZADOR
- Post-login redirect is handled by `CustomAuthenticationSuccessHandler`
- Every login/logout is audited in `intento_login` and `registro_sesiones` tables; the session DB ID is kept in HTTP session attribute `ID_SESION_BD`
- Password hashing: BCrypt strength 10

### Database naming
JPA uses `PhysicalNamingStrategyStandardImpl` — **no** automatic camelCase-to-snake_case conversion. Every `@Entity` field that maps to a column must have an explicit `@Column(name="...")` annotation matching the SQL column name exactly.

### Entities with enums
- `Usuario`: `EstadoCuenta` enum (`ACTIVO`, `BLOQUEADO`); `estadoAprobacion` stored as string (`PENDIENTE`/`APROBADO`/`RECHAZADO`)
- `Estudio`: `FormatoEstudio`, `EstadoEstudio`, `TipoAcceso` enums
- `Normativa`: several classification enums

### Templates
Located in `src/main/resources/templates/`, organized by role:
- `auth/` — login, registration, password recovery flows
- `admin/` — dashboard, user management, studies, normativas, solicitudes, notifications, activity logs
- `socio/` — home panel, study and normativa viewers
- `fragments/` — `navbar.html`, `sidebar.html`, `icons.html`, `study-card.html` (included via Thymeleaf fragments)

Tailwind CSS v3 with `@tailwindcss/forms` plugin. Thymeleaf cache is disabled in development (`spring.thymeleaf.cache=false`).

### Email
`CorreoService` sends via Spring Mail (SMTP relay `reciclame.soporte@gmail.com`). Used for password recovery codes managed by `RecuperacionPasswordService`.
