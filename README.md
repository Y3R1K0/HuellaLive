# HuellaLive

HuellaLive es una plataforma movil para conectar animales rescatados con familias adoptantes mediante un feed de videos estilo social, perfiles de animales, perfiles de albergues, solicitudes de adopcion, chats, mapa de albergues y donaciones directas con Mercado Pago Marketplace.

## Integrantes

- Yeriko Ali
- Diego Calle

## Arquitectura

```text
HuellaLive
+-- app/                 App Kotlin Multiplatform / Compose Multiplatform
|   +-- androidApp/      Aplicacion Android
|   +-- iosApp/          Entrada iOS preparada
|   +-- shared/          UI, modelos, repositorios y logica compartida
+-- backend/             API NestJS + Prisma + PostgreSQL
+-- admin-dashboard/     Dashboard web estatico para administracion
+-- deploy/              Carpeta lista para publicar admin en VPS
+-- entregables/         APK debug y archivos de entrega
+-- presentacion/        Material de presentacion
```

## Funcionalidades principales

- Login y registro para humanos y albergues.
- Login con Google mediante Firebase.
- Registro de albergues con aprobacion administrativa.
- Feed de videos de animales e historias de albergue.
- Likes en videos, reportes y control de videos vistos.
- Perfil de animal con foto, videos, estado, cartilla y datos editables.
- Perfil de albergue con portada, foto, descripcion, ubicacion y animales.
- Historias de albergue independientes de los animales.
- Subida de fotos/videos a Cloudinary.
- Busqueda por especie, ciudad y filtros predeterminados del admin.
- Explorar con mapa OpenStreetMap/MapLibre.
- Solicitudes de adopcion, aceptacion/rechazo y transferencia del animal.
- Chats entre humano y albergue tras solicitud de adopcion.
- Donaciones a albergues con Mercado Pago Marketplace.
- Dashboard admin para albergues, especies, ciudades, reportes y metricas.

## Tecnologias

- Kotlin Multiplatform
- Compose Multiplatform / Material 3
- Android
- NestJS
- Prisma
- PostgreSQL
- Cloudinary
- Firebase Auth / Firebase Admin
- Mercado Pago Marketplace
- Render
- VPS con Nginx para el admin web

## URLs de produccion

```text
API:   https://api.huellalive.lat
Admin: https://admin.huellalive.lat
```

## Backend local

```powershell
cd C:\back\s\HuellaLive\backend
npm install
npx prisma migrate deploy
npm run start:dev
```

## Variables de entorno backend

Crear `backend/.env` localmente. No subir este archivo a GitHub.

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

## App Android

```powershell
cd C:\back\s\HuellaLive\app
.\gradlew.bat :androidApp:assembleDebug
```

APK debug:

```text
C:\back\s\HuellaLive\app\androidApp\build\outputs\apk\debug\androidApp-debug.apk
```

APK de entrega:

```text
C:\back\s\HuellaLive\entregables\HuellaLive-debug.apk
```

## Admin dashboard local

```powershell
cd C:\back\s\HuellaLive\admin-dashboard
python -m http.server 5174
```

Abrir:

```text
http://localhost:5174
```

## Deploy backend en Render

```powershell
cd C:\back\s\HuellaLive\backend
docker build -t k4ry/huellalive-backend:latest .
docker push k4ry/huellalive-backend:latest
```

En Render:

- Crear PostgreSQL.
- Crear Web Service desde imagen Docker.
- Image URL: `docker.io/k4ry/huellalive-backend:latest`.
- Configurar variables de entorno.
- Asociar dominio `api.huellalive.lat`.

## Deploy admin en VPS

La carpeta preparada esta en:

```text
C:\back\s\HuellaLive\deploy\huellalive-admin-vps
```

Copiar a la VPS y servir con Nginx apuntando a `admin.huellalive.lat`.

## Seguridad

No subir:

- `backend/.env`
- JSON de Firebase Admin SDK
- dumps `.dump`, `.sql`
- `node_modules`
- carpetas `build`, `dist`, `.gradle`
- claves de Mercado Pago, Cloudinary o Firebase

## Estado actual

Proyecto funcional para demo avanzada:

- App Android operativa.
- Backend en Render.
- Base PostgreSQL en Render.
- Cloudinary activo.
- Firebase Auth activo.
- Mercado Pago Marketplace integrado.
- Admin web listo para VPS.

Pendientes posibles:

- Pulido final visual.
- Notificaciones push productivas.
- Build firmado AAB para Play Store.
- Validaciones legales/tributarias del flujo de donaciones.
