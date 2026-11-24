# Neon Player 🎧

Reproductor de música para Android con diseño **morado neón y azul**.
Hecho con Kotlin + Jetpack Compose + Media3 (ExoPlayer). Se compila en la nube con GitHub Actions, sin Android Studio.

## Qué hace

- Lee las canciones guardadas en tu teléfono
- Reproducir / pausar, siguiente, anterior
- Barra de progreso (puedes arrastrarla)
- Modo aleatorio y repetir (todo / una)
- Lista de canciones con la actual resaltada

## Cómo obtener la app (solo con GitHub)

1. Crea un repositorio nuevo en github.com (puede ser privado).
2. Sube **todo el contenido** de esta carpeta, incluida la carpeta oculta `.github`.
   - Desde un computador: en el repo pulsa **Add file → Upload files** y arrastra todo.
   - Desde el celular: abre el repo en **Codespaces**, sube el .zip, descomprímelo con `unzip` y haz commit.
3. Ve a la pestaña **Actions**. El workflow "Compilar APK" arranca solo al subir los archivos (tarda unos 5 a 8 minutos).
4. Cuando termine, ve a **Releases** en la página principal del repo y descarga `app-debug.apk`.
5. Ábrelo en tu Android y acepta "instalar apps de orígenes desconocidos".
6. Al abrir la app, da permiso para leer la música.

## Cambiar los colores

Edita `app/src/main/java/com/neon/musica/ui/theme/Theme.kt`:

```kotlin
val NeonPurple = Color(0xFFB026FF)
val NeonBlue   = Color(0xFF00D4FF)
```

## Si falla la compilación

Abre la pestaña **Actions**, entra al intento fallido y copia el error. Con eso se corrige rápido.
