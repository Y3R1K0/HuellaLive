# Deploy HuellaLive Backend en Render

Dominio objetivo:

- API: `https://api.huellalive.lat`
- Health check: `https://api.huellalive.lat/health`
- Mercado Pago webhook: `https://api.huellalive.lat/payments/mercadopago/webhook`

## 1. Crear PostgreSQL en Render

En Render:

1. New + -> PostgreSQL.
2. Nombre sugerido: `huellalive-db`.
3. Region: la misma que el backend.
4. Plan recomendado para produccion inicial: plan pago basico.
5. Copia el `Internal Database URL`.

Ese valor ira como `DATABASE_URL` en el backend.

## 2. Desplegar backend sin GitHub

Como no vamos a subir el proyecto a GitHub, usa una imagen Docker.

Desde `C:\back\s\HuellaLive\backend`:

```powershell
docker build -t TU_USUARIO_DOCKER/huellalive-backend:latest .
docker push TU_USUARIO_DOCKER/huellalive-backend:latest
```

En Render:

1. New + -> Web Service.
2. Selecciona `Deploy an existing image from a registry`.
3. Image URL: `docker.io/TU_USUARIO_DOCKER/huellalive-backend:latest`.
4. Nombre: `huellalive-api`.
5. Region: la misma que PostgreSQL.
6. Instance type: Starter o superior.

El Dockerfile ya corre:

```bash
npx prisma migrate deploy && node dist/main.js
```

## 3. Variables de entorno

Configura estas variables en Render:

```env
DATABASE_URL=
JWT_SECRET=
JWT_REFRESH_SECRET=
JWT_EXPIRES_IN=7d
JWT_REFRESH_EXPIRES_IN=30d
CLOUDINARY_CLOUD_NAME=huellalive
CLOUDINARY_API_KEY=
CLOUDINARY_API_SECRET=
FIREBASE_PROJECT_ID=huellalive
FIREBASE_CLIENT_EMAIL=
FIREBASE_PRIVATE_KEY=
MERCADOPAGO_ACCESS_TOKEN=
MERCADOPAGO_CLIENT_ID=4883957871607037
MERCADOPAGO_CLIENT_SECRET=
MERCADOPAGO_REDIRECT_URI=https://api.huellalive.lat/payments/mercadopago/oauth/callback
PUBLIC_BASE_URL=https://api.huellalive.lat
DONATION_DEVELOPER_CUT_RATE=0.25
APP_DEEP_LINK_SCHEME=huellalive
```

Notas:

- `FIREBASE_PRIVATE_KEY` debe conservar los saltos como `\n` si Render lo guarda en una sola linea.
- No subas `.env` ni JSON de Firebase Admin.
- Usa credenciales productivas de Mercado Pago cuando ya se publique la app.

## 4. Dominio en Render

En el Web Service:

1. Settings -> Custom Domains.
2. Add Custom Domain: `api.huellalive.lat`.
3. Render mostrara el registro DNS que debes crear.

En el panel donde compraste `huellalive.lat`, crea el DNS que Render indique.

Normalmente sera algo como:

```text
Tipo: CNAME
Nombre: api
Valor: tu-servicio.onrender.com
```

Render emitira SSL automaticamente cuando el DNS propague.

## 5. Mercado Pago

En Mercado Pago configura OAuth y webhook.

OAuth redirect/callback:

```text
https://api.huellalive.lat/payments/mercadopago/oauth/callback
```

Webhook:

```text
https://api.huellalive.lat/payments/mercadopago/webhook
```

Eventos recomendados:

- `payment`

Marketplace:

- Cada albergue debe entrar a `Donaciones y retiros`.
- Tocar `Conectar Mercado Pago marketplace`.
- Autorizar la cuenta Mercado Pago del albergue.
- Las donaciones se crean con el token del albergue y `marketplace_fee` para la comision de HuellaLive.
- Comision actual de HuellaLive: 25% del monto total de la donacion.

## 6. App Android

Cuando `api.huellalive.lat` este activo, cambia la URL base de la app a:

```text
https://api.huellalive.lat
```

Luego recompila el APK/AAB.
