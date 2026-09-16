# Stock Dashboard

Dashboard full-stack de sugerencias de acciones. React (Vite) + Spring Boot + PostgreSQL.

## Fase 1 (completa): Autenticación

- Registro con nombre/apellido/email/contraseña + verificación de email por código.
- Login con Google (OAuth2) — opcional, ver abajo.
- JWT (access token 15 min + refresh token 30 días, con rotación en cada uso).

## Requisitos

- Java 21+ (el proyecto se generó con Spring Boot 4, requiere 21 como mínimo).
- Node 20+.
- Docker (para Postgres local).

## Cómo correrlo

**1. Base de datos:**
```bash
cd backend
docker compose up -d
```

**2. Backend** (puerto 8080):
```bash
cd backend
JAVA_HOME=/opt/homebrew/opt/openjdk@21 ./mvnw spring-boot:run
```

**3. Frontend** (puerto 5183):
```bash
cd frontend
npm install
npm run dev
```

Abrí http://localhost:5183 — te lleva a `/login`.

## Configuración opcional

Por defecto la app corre sin credenciales externas:
- **Verificación de email**: en modo `console` por defecto — el código de verificación se loguea en la consola del backend en vez de mandarse por mail de verdad. Para mandar emails reales, seteá `EMAIL_MODE=smtp` + `SMTP_HOST` / `SMTP_USER` / `SMTP_PASSWORD` (ver `backend/src/main/resources/application.yml`).
- **Login con Google**: no aparece habilitado hasta que configures `GOOGLE_CLIENT_ID` y `GOOGLE_CLIENT_SECRET` (sacalos de [Google Cloud Console](https://console.cloud.google.com/apis/credentials), con `http://localhost:8080/login/oauth2/code/google` como redirect URI autorizado).
- **JWT secret**: hay un valor de desarrollo por defecto — cambialo con `APP_JWT_SECRET` antes de cualquier ambiente compartido.

## Tests

```bash
cd backend
JAVA_HOME=/opt/homebrew/opt/openjdk@21 ./mvnw test
```

16 tests (lógica de registro, verificación, login, refresh/rotación de tokens, y alta/link de cuentas por Google).

## Estructura

Ver el mensaje de planificación original — resumen rápido:
- `backend/src/main/java/com/stockdashboard/{auth,user,security,config,email,common}` — por feature.
- `frontend/src/features/{auth,dashboard}` — por feature.

## Próximas fases (no arrancadas)

2. Dashboard + favoritos.
3. Sugerencia: más activas del día (Twelve Data).
4. Sugerencia: earnings calendar (Financial Modeling Prep).
5. Sugerencia: estrategia intradiaria (SMA/EMA/MACD calculados en el backend).
6. Detalle de acción + widget de TradingView.
