# Deploy a Railway

Esta app tiene 3 piezas, cada una un servicio separado en Railway:

1. **Postgres** — plugin gestionado de Railway, no necesita Dockerfile.
2. **backend** — `backend/Dockerfile`, Spring Boot.
3. **frontend** — `frontend/Dockerfile`, build estático servido con nginx.

## 1. Crear el proyecto

En [railway.app](https://railway.app), creá un proyecto nuevo y conectá el repo de GitHub (o subí el código si todavía no está en GitHub).

## 2. Agregar Postgres

Dentro del proyecto: **+ New → Database → PostgreSQL**. Railway lo levanta solo y expone variables (`PGHOST`, `PGPORT`, `PGDATABASE`, `PGUSER`, `PGPASSWORD`) que los otros servicios pueden referenciar.

## 3. Servicio backend

**+ New → GitHub Repo** (mismo repo) → en **Settings**:
- **Root Directory**: `backend`
- Railway va a detectar el `Dockerfile` solo.

**Variables** (Settings → Variables). Las que referencian el servicio de Postgres usan la sintaxis `${{Postgres.NOMBRE}}` (Railway autocompleta esto si escribís `${{` y elegís el servicio):

```
DB_HOST=${{Postgres.PGHOST}}
DB_PORT=${{Postgres.PGPORT}}
DB_NAME=${{Postgres.PGDATABASE}}
DB_USER=${{Postgres.PGUSER}}
DB_PASSWORD=${{Postgres.PGPASSWORD}}

APP_JWT_SECRET=<generar uno nuevo, ver abajo — NUNCA el de desarrollo>

FRONTEND_URL=<URL pública del servicio frontend, la sabés después del paso 4>

EMAIL_MODE=smtp
SMTP_HOST=<tu proveedor SMTP>
SMTP_PORT=587
SMTP_USER=<...>
SMTP_PASSWORD=<...>

GOOGLE_CLIENT_ID=<el mismo que ya creaste en Google Cloud>
GOOGLE_CLIENT_SECRET=<el mismo>

MARKETDATA_PROVIDER=twelvedata
TWELVE_DATA_API_KEY=<tu key>
MARKETDATA_MAX_STALE_FETCH=8
STRATEGY_PROVIDER=twelvedata
STRATEGY_MAX_STALE_FETCH=8

EARNINGS_PROVIDER=fmp
FMP_API_KEY=<tu key>
```

**JWT secret nuevo** (no reutilices el de dev, que está harcodeado como default en el repo):

```bash
openssl rand -base64 32
```

Railway asigna la URL pública del backend automáticamente (Settings → Networking → Generate Domain) — algo como `https://stockdash-backend-production.up.railway.app`. La vas a necesitar para el paso 4 y para el redirect de Google.

## 4. Servicio frontend

**+ New → GitHub Repo** (mismo repo) → **Settings**:
- **Root Directory**: `frontend`

**Build Variables** (no runtime — el Dockerfile las usa como `ARG` en el build, tienen que estar en la sección de variables ANTES de que Railway construya la imagen):

```
VITE_API_URL=<la URL del backend del paso 3>
```

Generá también su dominio público (Settings → Networking → Generate Domain). Esa URL es la que ponés como `FRONTEND_URL` en el backend (paso 3) — puede que tengas que volver atrás y completarla ahí una vez que la tengas.

## 5. Actualizar Google OAuth para producción

En Google Cloud Console → tu cliente OAuth (Stock-Dash) → agregá una **URI de redirección autorizada** nueva, sin borrar la de localhost:

```
https://<tu-backend-de-railway>/login/oauth2/code/google
```

## 6. Cosas a tener en cuenta

- **Los free tiers de Twelve Data (8 req/min, 800/día) y FMP siguen siendo los mismos** — production no les da más cuota. Si esto va a tener uso real, en algún momento vas a necesitar un plan pago de esos proveedores.
- **Google OAuth va a seguir en modo "Testing"** hasta que publiques la app — solo van a poder loguearse los emails que agregaste como test users.
- **`EMAIL_MODE=smtp` es obligatorio en producción** — con `console` (el default de desarrollo) el código de verificación se loguea en un log que nadie ve, y nadie puede verificar su cuenta.
