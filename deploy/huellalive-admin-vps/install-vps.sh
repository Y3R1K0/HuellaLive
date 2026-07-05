#!/usr/bin/env bash
set -euo pipefail

APP_DIR="/var/www/huellalive-admin"
NGINX_SITE="/etc/nginx/sites-available/huellalive-admin"
NGINX_ENABLED="/etc/nginx/sites-enabled/huellalive-admin"

echo "Instalando dependencias..."
sudo apt update
sudo apt install -y nginx certbot python3-certbot-nginx unzip

echo "Preparando carpeta publica..."
sudo mkdir -p "$APP_DIR"
sudo cp -R ./index.html ./styles.css ./app.js ./README.md ./VPS_DEPLOY.md "$APP_DIR"/
sudo chown -R www-data:www-data "$APP_DIR"
sudo find "$APP_DIR" -type d -exec chmod 755 {} \;
sudo find "$APP_DIR" -type f -exec chmod 644 {} \;

echo "Configurando Nginx..."
sudo cp ./nginx-huellalive-admin.conf "$NGINX_SITE"
sudo ln -sf "$NGINX_SITE" "$NGINX_ENABLED"
sudo nginx -t
sudo systemctl reload nginx

echo "Listo en HTTP: http://admin.huellalive.lat"
echo "Cuando el DNS ya apunte a esta VPS, ejecuta:"
echo "sudo certbot --nginx -d admin.huellalive.lat"
