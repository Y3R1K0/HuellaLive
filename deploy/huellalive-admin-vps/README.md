# HuellaLive Admin Dashboard

Dashboard web estatico para administrar HuellaLive desde una VPS.

## Ejecutar local

Abre `index.html` directamente o sirve esta carpeta con cualquier servidor estatico.

Ejemplo:

```bash
python -m http.server 5174
```

Luego entra a:

```text
http://localhost:5174
```

## Configuracion

Por defecto usa:

```text
https://api.huellalive.lat
```

Tambien puedes cambiar la API desde la pantalla de login.

## Subir a VPS

Copia todo el contenido de esta carpeta a tu carpeta publica, por ejemplo:

```text
/var/www/huellalive-admin
```

Es una app estatica, no necesita Node en produccion.

## Funciones incluidas

- Login admin con JWT del backend.
- Metricas generales.
- Aprobar o rechazar albergues pendientes.
- Gestionar ciudades predeterminadas.
- Gestionar especies predeterminadas.
- Aprobar o rechazar solicitudes de nuevas especies.
- Revisar reportes y cambiar su estado.

