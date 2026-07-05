# HuellaLive Admin - VPS

Esta carpeta esta lista para subir a la VPS y servir el dashboard admin como sitio estatico.

## Datos

VPS:

```text
38.250.116.219
```

Dominio recomendado para el panel:

```text
admin.huellalive.lat
```

API usada por defecto:

```text
https://api.huellalive.lat
```

## 1. DNS

En Namecheap agrega este registro:

```text
Type: A Record
Host: admin
Value: 38.250.116.219
TTL: Automatic
```

Espera a que propague. Puedes probar:

```bash
nslookup admin.huellalive.lat
```

Debe devolver `38.250.116.219`.

## 2. Subir carpeta a la VPS

Desde Windows puedes subir el ZIP o la carpeta por SCP.

Ejemplo con ZIP:

```powershell
scp "C:\back\s\HuellaLive\deploy\huellalive-admin-vps.zip" root@38.250.116.219:/root/
```

En la VPS:

```bash
cd /root
unzip -o huellalive-admin-vps.zip -d huellalive-admin-vps
cd huellalive-admin-vps
chmod +x install-vps.sh
./install-vps.sh
```

## 3. Activar HTTPS

Cuando `http://admin.huellalive.lat` abra bien, ejecuta:

```bash
sudo certbot --nginx -d admin.huellalive.lat
```

Certbot modificara Nginx automaticamente para usar SSL.

## 4. Rutas importantes

Sitio publicado:

```text
/var/www/huellalive-admin
```

Config Nginx:

```text
/etc/nginx/sites-available/huellalive-admin
```

## 5. Actualizar despues

Sube nuevamente los archivos y desde la VPS ejecuta:

```bash
cd /root/huellalive-admin-vps
./install-vps.sh
```

No necesita Node, npm ni base de datos en la VPS. El dashboard consume el backend ya desplegado en Render.
