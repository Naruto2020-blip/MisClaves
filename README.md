# Mis Claves

Aplicación Android independiente para organizar usuarios y contraseñas.

## Descargar la APK

**[Descargar la APK firmada más reciente de Mis Claves](https://github.com/Naruto2020-blip/MisClaves/releases/latest/download/app-release.apk)**

El enlace apunta automáticamente a la última versión release firmada publicada.

## Estado actual

Esta es una versión inicial de desarrollo. Incluye una interfaz en español, tema claro/oscuro, alta/edición/eliminación y búsqueda de registros, copia al portapapeles y almacenamiento local cifrado con AES-GCM mediante una clave de Android Keystore.

## Compilar desde GitHub usando un teléfono

Cada cambio en la rama `main` inicia el flujo **Construir APK de Mis Claves**. También se puede iniciar manualmente desde la pestaña **Actions**.

Cuando termine correctamente, el flujo guarda la APK como artefacto y publica una release firmada. El enlace de descarga de arriba siempre apunta a la publicación más reciente.

## Seguridad: importante

- El repositorio es público: nunca agregues aquí contraseñas, tokens, claves privadas, archivos de firma ni datos personales.
- Antes de guardar contraseñas importantes, revisa las funciones de seguridad y las limitaciones de esta versión.
- La APK release se firma con la clave privada de lanzamiento, que debe conservarse de forma privada y segura para todas las actualizaciones futuras.

## Requisitos del proyecto

- Android 8.0 (API 26) o superior.
- Kotlin, Jetpack Compose y Android Gradle Plugin.
