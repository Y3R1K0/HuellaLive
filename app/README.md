# HuellaLive App

Aplicacion movil de HuellaLive construida con Kotlin Multiplatform y Compose Multiplatform.

## Estructura

```text
app/
+-- androidApp/      App Android
+-- iosApp/          Entrada iOS preparada
+-- shared/          Codigo compartido
|   +-- commonMain/  UI, modelos, repositorios y navegacion
|   +-- androidMain/ Implementaciones Android
|   +-- iosMain/     Implementaciones iOS preparadas
+-- gradle/          Configuracion Gradle
```

## Funcionalidades

- Feed de videos de animales e historias de albergue.
- Busqueda por especie, ciudad y estado.
- Explorar con mapa.
- Perfiles de animal, humano y albergue.
- Solicitudes de adopcion.
- Chats y envio de imagenes.
- Subida de fotos/videos.
- Login por correo y Google.
- Donaciones a albergues mediante Mercado Pago.

## Ejecutar en Android Studio

1. Abrir `C:\back\s\HuellaLive\app` en Android Studio.
2. Esperar sincronizacion de Gradle.
3. Seleccionar `androidApp`.
4. Ejecutar en emulador o celular.

## Compilar APK debug

```powershell
cd C:\back\s\HuellaLive\app
.\gradlew.bat :androidApp:assembleDebug
```

Salida:

```text
C:\back\s\HuellaLive\app\androidApp\build\outputs\apk\debug\androidApp-debug.apk
```

## Copiar APK a entregables

```powershell
Copy-Item "C:\back\s\HuellaLive\app\androidApp\build\outputs\apk\debug\androidApp-debug.apk" "C:\back\s\HuellaLive\entregables\HuellaLive-debug.apk" -Force
```

## iOS

El proyecto tiene entrada iOS preparada en `iosApp`, pero el desarrollo y pruebas actuales estan enfocados en Android. Para compilar iOS se requiere macOS y Xcode.
