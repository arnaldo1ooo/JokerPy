# JokerPy

App Android (celular y Android TV) para guardar tus sitios favoritos en un menú.

## Qué hace
- Menú en grilla con búsqueda, favoritos y navegación con control remoto.
- Agregar, editar y eliminar sitios a mano (pulsación larga sobre un sitio = opciones).
- Canales de TV abierta precargados la primera vez (archivo `app/src/main/assets/canales.json`).
- Icono de cada sitio: por defecto el de la propia página; se puede cambiar por uno propio.
- Bloqueador de anuncios, con interruptor en Ajustes (lista en `app/src/main/assets/anuncios.txt`).
- Video a pantalla completa dentro del navegador interno.
- Exportar e importar la lista en un archivo JSON (Ajustes > Respaldo). Al importar no se repiten los sitios que ya tienes. Algunas TV no traen selector de archivos; en ese caso la app lo avisa.

## Cómo levantarlo (Android Studio)
1. Instala Android Studio (la versión actual, gratis).
2. Abre Android Studio > Open > elige la carpeta `JokerPy`.
3. Espera a que termine "Gradle sync" (la primera vez descarga cosas, puede tardar unos minutos).
4. Arriba, en el selector de dispositivos, elige uno:
   - Tu celular con "Depuración USB" activada, o
   - Un emulador: Device Manager > Create Device > "Phone" o "Television (1080p)".
5. Presiona el botón verde de Play (Run).

## Probar en tu TV real
1. En la TV: Ajustes > Acerca de > pulsa 7 veces en "Compilación" para activar opciones de desarrollador.
2. Activa "Depuración por USB" o "Depuración por red".
3. En Android Studio: Device Manager > Pair devices over Wi-Fi, o `adb connect IP_DE_LA_TV:5555`.
4. Elige la TV en el selector y presiona Run.

## Generar el APK para instalar
Build > Build Bundle(s) / APK(s) > Build APK(s). Queda en `app/build/outputs/apk/debug/`.

## Antes de publicar
- Revisa que cada URL de `canales.json` sea la página oficial del canal.
- Cambia `applicationId` en `app/build.gradle` si quieres otro identificador.
