# Validación de la versión ordenada

## Realizado

- `:app:assembleDebug`: compilación Java, recursos XML, DEX y APK correctos.
- `:app:lintDebug`: completado sin errores; quedan advertencias no bloqueantes de presentación/configuración.
- Prueba en JVM de TextNormalizer: tildes, mayúsculas, espacios y cadena vacía correctos.
- SQL propuesto ejecutado en SQLite en memoria: PK única, actualización sin duplicar, fecha con final exclusivo, búsqueda literal, Top 5, marcador único y eliminación correctos.
- La BD propuesta no está conectada a la app; la prueba SQL no equivale a probar Room.

Entorno: JDK 21, Gradle 8.9, plugin Android 8.7.3, SDK 35 y Build Tools 34.0.0. Se mantienen applicationId, versionCode y claves de preferencias para no cambiar la identidad ni el almacenamiento existente.

## Pendiente en teléfono

No se ejecutó la app con una clave real en dispositivo/emulador. Probar:

1. Cámara, galería, cancelación y nueva foto después de un resultado.
2. Análisis con clave válida, inválida y sin conexión.
3. Confirmar una especie y corregirla sin duplicar la consulta.
4. Catálogo, búsqueda sin tildes, ficha y escuchar/detener audio.
5. Historial, fecha de Perú, texto y gráfico con los mismos filtros.
6. Fotos, eliminación individual, borrado total y cancelaciones.
7. Cerrar y reabrir para comprobar persistencia y registros antiguos.
8. Tamaño de letra aumentado y pantalla pequeña.

La guía de Room contiene pruebas adicionales para ejecutar cuando se implemente la BD y la importación de datos.
