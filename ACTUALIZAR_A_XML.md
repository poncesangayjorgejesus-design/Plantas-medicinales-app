# Flora Andina — interfaz Java + XML

> Esta guía describe la migración visual. Para actualizar esta versión ordenada, seguir primero ESTRUCTURA_DEL_CODIGO.md: las clases ahora están en subpaquetes y debe sustituirse la carpeta Java completa.

Las pantallas, tarjetas, fotografías y gráficos ahora se definen en `app/src/main/res/layout/`. Java carga los diseños con `setContentView`, `LayoutInflater` y `findViewById`, y conserva la lógica, datos, eventos y consultas a la API.

## Qué versión se utilizó

El ZIP recibido contiene una carpeta de trabajo de `main` que no incluye PlantCatalog, HistoryStore, HistoryScreen, PlantCharts ni el catálogo JSON. Sus avances sí están dentro del historial Git incluido. La base utilizada es el commit `f15e4ea`, rama `origin/mejoras-estilos-flora`, que incluye el merge de gráficos e historial y los estilos posteriores. No se consultó ni modificó el repositorio remoto.

Se mantuvieron los datos del catálogo de esa rama, la API, el almacenamiento privado, fotos, confirmación de especie, búsqueda, fecha, borrado individual y total, voz y gráficos. En esta versión del equipo, **la búsqueda de texto y la fecha filtran también el gráfico Top 5**; se conserva ese comportamiento.

## Dónde editar cada pantalla

| XML (dentro de res/layout) | Java que conecta el diseño |
| --- | --- |
| `activity_main.xml` | MainActivity: estructura y navegación inferior |
| `screen_scan.xml` | MainActivity: cámara, galería, vista previa y análisis |
| `screen_catalog.xml` | CatalogScreen: buscador y lista del catálogo |
| `item_plant.xml` | CatalogScreen: tarjeta de una planta |
| `dialog_plant.xml` | CatalogScreen: ficha y botones Escuchar/Detener/Cerrar |
| `screen_history.xml` | HistoryScreen: buscador y filtro por fecha |
| `item_history.xml` | HistoryScreen: consulta y botón Eliminar |
| `item_history_photo.xml` | HistoryPhotos: miniatura y botón Ver foto |
| `dialog_photo.xml` | HistoryPhotos: fotografía ampliada |
| `screen_settings.xml` | SettingsScreen e HistoryScreen: clave y borrado global |
| `section_results.xml` | RecognitionController: encabezado, aviso y candidatos |
| `item_match.xml` | RecognitionController: candidato, ficha y confirmación |
| `card_top_five.xml` | PlantCharts: encabezado y contenedor del gráfico |
| `item_chart_bar.xml` | PlantCharts: nombre, cantidad y barra |
| `view_match_score.xml` | PlantCharts: puntaje y barra de coincidencia |
| `item_message.xml` | ScreenUi: mensajes de listas vacías y contadores |

Colores: `res/values/colors.xml`. Textos de interfaz: `res/values/strings.xml`. Tipografías, tamaños y estilos: `res/values/styles.xml`. Fondos y botones: `res/drawable/`. Color de navegación seleccionada: `res/color/nav_text.xml`.

En Android Studio: Project > app > src > main > res > layout. Abrir el XML y elegir **Code**, **Split** o **Design**. Los layouts de pantallas y tarjetas pueden previsualizarse por separado; las listas se llenan en tiempo de ejecución con datos reales.

## Qué sigue en Java

Java mantiene las acciones de botones, captura de fotos, petición HTTP, respuesta JSON, cálculo de filtros/ranking/puntajes y persistencia. Las tarjetas repetidas se inflan desde XML dentro de los bucles. Los gráficos usan ProgressBar horizontal declarado en XML; Java cambia su valor y el color según el puntaje.

Los diálogos estándar de confirmación y el selector de fecha siguen usando AlertDialog y DatePickerDialog; no generan una pantalla principal a mano. La ficha y el visor de fotografías sí tienen layouts XML propios. Ya no hay construcción manual de Button, TextView, LinearLayout, EditText, ImageView, ScrollView o ProgressBar en las clases de la app.

No se añaden una base de datos ni servicios externos. Se conservan `pe.flora.andina`, versionCode 2, versionName 1.1 y SharedPreferences `flora`, con las claves `key` e `history`. No desinstalar la app si quieren conservar sus consultas y fotos. Para instalar encima se necesita la misma firma utilizada anteriormente.

## Abrir el proyecto completo

1. Extraer este ZIP en una carpeta nueva.
2. Abrir la carpeta `Plantas-medicinales-app` con Android Studio.
3. Usar JDK 17 o 21 como Gradle JDK. Instalar SDK Platform 35 cuando lo solicite.
4. Sincronizar y ejecutar el módulo `app`.
5. Ingresar la clave en Ajustes; no incluirla en Java ni Git.

Se alineó el Wrapper con **Gradle 8.9**, la versión indicada para el plugin Android **8.7.3** que ya tenía el proyecto. Se retiró `gradle/gradle-daemon-jvm.properties`, que imponía JDK 25. `gradle.properties` contiene `android.useAndroidX=true` y salto de línea. Las dependencias de la app se conservan.

La integración TensorFlow Lite sigue en el código original, pero el ZIP no aporta dataset, modelo entrenado ni etiquetas. Para identificar mediante API, basta la clave válida y conexión.

## Aplicar en su repositorio con GitKraken

1. Guardar o confirmar el trabajo pendiente. Crear la rama `interfaz-xml` desde la rama que contiene sus últimos avances.
2. Copiar el contenido de `app/src/main/` de esta entrega al mismo lugar del clon. Esto incluye Java, assets y todos los recursos XML.
3. Copiar `gradle/wrapper/gradle-wrapper.properties` y `gradle.properties` de esta entrega. Mantener app/build.gradle si coincide con el incluido.
4. **Si existe en su clon, eliminar `gradle/gradle-daemon-jvm.properties`** para quitar la imposición anterior de JDK 25. La ausencia de este archivo en un ZIP no lo elimina al copiar encima.
5. Copiar esta guía y VALIDACION.md si quieren dejar la explicación para el profesor.
6. Revisar los cambios antes de confirmar. Si han hecho más cambios después del ZIP enviado, integrarlos mediante el diff, sin reemplazarlos a ciegas.
7. Sincronizar, compilar y realizar las pruebas de VALIDACION.md.
8. Commit sugerido: `refactor: migrar interfaz de Flora Andina a XML`. Push y revisión del equipo antes de fusionar.

El entregable no lleva `.git`, `.idea`, archivos build ni rutas de SDK de esta máquina. No copiar ni sustituir la carpeta `.git` de su repositorio; así conservan su historial y ramas.

## Referencias de Android

- [Layouts XML y lógica Java](https://developer.android.com/develop/ui/views/layout/declaring-layout)
- [Compatibilidad del plugin Android 8.7](https://developer.android.com/build/releases/agp-8-7-0-release-notes)
