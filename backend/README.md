# HuellaLive Backend

API de HuellaLive construida con NestJS, Prisma y PostgreSQL.

## Funcionalidades

- Autenticacion JWT.
- Login con Firebase/Google.
- Registro de humanos y albergues.
- Gestion de animales y cartillas.
- Feed de videos e historias de albergue.
- Likes, reportes e historial de videos vistos.
- Solicitudes de adopcion y transferencia de animales.
- Chats y mensajes con imagenes.
- Subida de medios a Cloudinary.
- Donaciones con Mercado Pago Marketplace.
- Modulo admin para albergues, especies, ciudades y reportes.

## Requisitos

- Node.js 22 o compatible
- npm
- PostgreSQL
- Docker Desktop para deploy por imagen

## Instalacion local

```powershell
cd C:\back\s\HuellaLive\backend
npm install
```

## Variables de entorno

Crear `backend/.env`. No subirlo a GitHub.

```env
DATABASE_URL=
JWT_SECRET=
JWT_REFRESH_SECRET=
JWT_EXPIRES_IN=7d
JWT_REFRESH_EXPIRES_IN=30d
CLOUDINARY_CLOUD_NAME=
CLOUDINARY_API_KEY=
CLOUDINARY_API_SECRET=
FIREBASE_PROJECT_ID=
FIREBASE_CLIENT_EMAIL=
FIREBASE_PRIVATE_KEY=
MERCADOPAGO_ACCESS_TOKEN=
MERCADOPAGO_CLIENT_ID=
MERCADOPAGO_CLIENT_SECRET=
MERCADOPAGO_REDIRECT_URI=https://api.huellalive.lat/payments/mercadopago/oauth/callback
MERCADOPAGO_WEBHOOK_SECRET=
PUBLIC_BASE_URL=https://api.huellalive.lat
DONATION_DEVELOPER_CUT_RATE=0.25
APP_DEEP_LINK_SCHEME=huellalive
```

## Base de datos

```powershell
npx prisma migrate deploy
```

## Ejecutar en desarrollo

```powershell
npm run start:dev
```

API local:

```text
http://localhost:3000
```

## Compilar

```powershell
npm run build
```

## Produccion local

```powershell
npm run start:prod
```

## Deploy en Render con Docker

```powershell
cd C:\back\s\HuellaLive\backend
docker build -t k4ry/huellalive-backend:latest .
docker push k4ry/huellalive-backend:latest
```

En Render:

- Web Service desde imagen Docker.
- Imagen: `docker.io/k4ry/huellalive-backend:latest`.
- Variables de entorno configuradas.
- Dominio: `api.huellalive.lat`.

El contenedor ejecuta migraciones antes de levantar la API.

## Mercado Pago

Rutas importantes:

```text
OAuth callback:
https://api.huellalive.lat/payments/mercadopago/oauth/callback

Webhook:
https://api.huellalive.lat/payments/mercadopago/webhook
```

El flujo marketplace conecta la cuenta Mercado Pago de cada albergue y aplica la comision de HuellaLive con `DONATION_DEVELOPER_CUT_RATE`.

## Seguridad

No subir:

- `.env`
- JSON de Firebase Admin
- claves reales de Mercado Pago
- claves reales de Cloudinary
- dumps de base de datos
- `node_modules`
- `dist`
