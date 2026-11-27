# Mis Claves

Aplicación Android independiente para organizar usuarios y contraseñas.

## Estado actual

Esta es una **versión inicial de desarrollo**. Incluye una interfaz en español, tema claro/oscuro, alta/edición/eliminación y búsqueda de registros, copia al portapapeles y almacenamiento local cifrado con AES-GCM mediante una clave de Android Keystore.

## Compilar desde GitHub usando un teléfono

Cada cambio en la rama `main` inicia el flujo **Construir APK de Mis Claves**. También se puede iniciar manualmente desde la pestaña **Actions**.

Cuando termine correctamente:
1. Abre la ejecución más reciente en **Actions**.
2. Busca **Artifacts**.
3. Descarga `MisClaves-debug`; contiene `app-debug.apk`.

## Seguridad: importante

- El repositorio es público: nunca agregues aquí contraseñas, tokens, claves privadas, archivos de firma ni datos personales.
- La APK de depuración todavía no es una versión final ni auditada.
- Aunque el archivo local se cifra con AES-GCM y la clave se guarda en Android Keystore, aún faltan bloqueo de bóveda con PIN/biometría, borrado automático del portapapeles, protección contra capturas, pruebas de recuperación y una auditoría de seguridad.
- **No guardes contraseñas reales ni importantes en esta versión inicial.**
- La compilación de depuración no es una publicación firmada para distribución. Para actualizaciones instalables se deberá configurar una firma de lanzamiento y conservarla de forma privada y segura.

## Requisitos del proyecto

- Android 8.0 (API 26) o superior.
- Kotlin, Jetpack Compose y Android Gradle Plugin.
