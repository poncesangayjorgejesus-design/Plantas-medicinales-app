# Flora Andina — código organizado, Java + XML

Proyecto Android con interfaz XML y clases Java separadas por responsabilidad. Incluye identificación por API, catálogo, voz, historial con fotos, filtros, borrado y gráficos.

## Leer primero

- [ESTRUCTURA_DEL_CODIGO.md](ESTRUCTURA_DEL_CODIGO.md): paquetes, flujo, responsables y cómo actualizar el repositorio.
- [docs/database/GUIA_BASE_DATOS.md](docs/database/GUIA_BASE_DATOS.md): pasos para crear una BD local con Room 2.x y migrar el historial.
- [docs/database/schema_v1.sql](docs/database/schema_v1.sql): esquema propuesto, todavía no integrado.
- [VALIDACION.md](VALIDACION.md): comprobaciones realizadas y pruebas pendientes.

**Estado actual:** el código está reorganizado y compila. El historial aún usa SharedPreferences; el catálogo usa JSON y las fotos son archivos privados. Room es la siguiente etapa explicada en la guía, no una dependencia ya agregada.

## Ejecutar

Abrir esta carpeta en Android Studio, utilizar JDK 17 o 21, instalar SDK Platform 35, sincronizar Gradle Wrapper 8.9 y ejecutar app. Ingresar la clave Pl@ntNet desde Ajustes.

Los 16 layouts están en `app/src/main/res/layout/`. El código está en `app/src/main/java/pe/flora/andina/`, distribuido en ui, data, recognition, model y util.

Al actualizar, sustituir la carpeta Java completa como indica ESTRUCTURA_DEL_CODIGO.md para que no queden clases antiguas duplicadas. Conservar la carpeta .git del repositorio.

El script e integración TensorFlow Lite se conservan, sin dataset ni modelo entrenado incluido.
