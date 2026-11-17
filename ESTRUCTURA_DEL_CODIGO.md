# Cómo está organizado el código

La app mantiene Java para comportamiento y XML para presentación. No se añadió un framework de interfaz ni una base de datos en esta entrega.

## Paquetes

Todos están dentro de `app/src/main/java/pe/flora/andina/`:

| Carpeta / paquete | Clases principales | Responsabilidad |
| --- | --- | --- |
| Raíz | MainActivity | Inicialización, navegación, captura de fotos y coordinación |
| `ui` | CatalogScreen, HistoryScreen, SettingsScreen | Conectar pantallas XML con datos y eventos |
| `ui` | RecognitionResults, PlantCharts | Mostrar candidatos, puntajes y Top 5 |
| `ui` | HistoryPhotos, ScreenUi | Visor/miniaturas e inflado de layouts |
| `data` | PlantRepository | Cargar y consultar el catálogo JSON, nombres y alias |
| `data` | HistoryStore | Guardar, confirmar, borrar y filtrar el historial actual |
| `data` | PhotoStore | Guardar y eliminar archivos de fotografías |
| `recognition` | Recognizer | Ejecutar Pl@ntNet o TensorFlow Lite |
| `recognition` | RecognitionController | Coordinar análisis asíncrono, foto y callbacks |
| `model` | PlantMatch, RecognitionResult, AnalysisResult | Datos que se intercambian entre análisis y pantalla |
| `model` | HistorySnapshot, ConfirmationResult | Historial filtrado y resultado de confirmar una especie |
| `util` | TextNormalizer, AppConstants | Búsqueda sin tildes, claves de preferencias, umbral, límite y zona horaria |

El repositorio de datos ya no necesita una pantalla para consultar plantas. RecognitionController ya no dibuja resultados. HistoryStore ya no muestra Toast: devuelve un resultado y MainActivity decide qué mensaje mostrar.

## Flujo de una consulta

1. MainActivity abre cámara/galería y muestra la foto en screen_scan.xml.
2. RecognitionController pide confirmación y ejecuta Recognizer en su worker.
3. Recognizer devuelve RecognitionResult con la lista de PlantMatch.
4. PhotoStore guarda la fotografía y el controlador devuelve AnalysisResult.
5. MainActivity pide a HistoryStore guardar la consulta y muestra RecognitionResults.
6. Si el usuario cambia la especie, HistoryStore actualiza la misma consulta; la Activity presenta el resultado.
7. HistoryScreen solicita un HistorySnapshot filtrado; PlantCharts dibuja los conteos de ese mismo conjunto.

## Qué cambió respecto a la entrega XML anterior

- Las clases se movieron a paquetes con responsabilidades explícitas.
- PlantCatalog se separó en **PlantRepository** (datos) y **CatalogScreen** (interfaz/voz).
- Los resultados visuales salieron de RecognitionController y pasaron a **RecognitionResults**.
- Los filtros y conteos salieron de HistoryScreen y pasaron a **HistoryStore.query**.
- Los registros de datos dejaron de estar anidados dentro del reconocedor y ahora están en **model**.
- MainActivity inicializa módulos en initializeModules y maneja la confirmación en confirmPlant.
- Se nombraron límites, tiempos de espera y parámetros del modelo.
- Java usa imports explícitos, formato consistente de cuatro espacios y comentarios de propósito.
- XML tiene indentación legible y mantiene los 16 layouts.

## Qué archivo modificar

| Necesidad | Archivo |
| --- | --- |
| Cambiar aspecto de una pantalla | XML correspondiente en res/layout |
| Cambiar colores o tamaño de letra | colors.xml y styles.xml |
| Cambiar API o formato de respuesta | recognition/Recognizer.java |
| Cambiar el proceso de análisis | recognition/RecognitionController.java |
| Cambiar qué se muestra en candidatos | ui/RecognitionResults.java |
| Agregar plantas | assets/catalogo.json |
| Cambiar búsqueda del catálogo | data/PlantRepository.java |
| Cambiar filtros/ranking de consultas | data/HistoryStore.java |
| Cambiar almacenamiento a Room | data/HistoryStore.java y futuros data/local/* |
| Cambiar voz o ficha | ui/CatalogScreen.java |

## Reglas para trabajar en equipo

- La capa data no debe crear Views, diálogos ni Toast.
- La capa recognition no debe consultar campos EditText ni construir tarjetas.
- Los XML contienen estructuras visuales; Java cambia datos, visibilidad y eventos.
- No cambiar claves `flora`, `key` o `history` sin una migración: se utilizan para conservar datos instalados.
- Mantener los mismos filtros para lista y gráfico.
- No mezclar implementación de Room con la reorganización en el mismo commit: primero validar esta base, después crear una rama para la BD.

## Actualizar el repositorio existente

1. Confirmar o guardar los cambios pendientes y crear la rama `refactor-estructura-java`.
2. **Sustituir completamente** `app/src/main/java/pe/flora/andina/` por esta versión. No combinar dejando las antiguas clases en la raíz: sus paquetes e imports cambiaron.
3. Copiar `app/src/main/res/` y conservar el catálogo incluido en `app/src/main/assets/`.
4. Usar la configuración Gradle incluida. Si existe la antigua `gradle/gradle-daemon-jvm.properties` que exige JDK 25, retirarla conforme a ACTUALIZAR_A_XML.md.
5. Revisar el diff en GitKraken. Si hay mejoras propias posteriores a la versión enviada, integrarlas antes de reemplazar.
6. Sincronizar, compilar y probar los flujos principales en el teléfono.
7. Commit sugerido: `refactor: separar interfaz datos y reconocimiento`. Push y revisión del equipo.

No tocar la carpeta .git ni reinstalar la aplicación borrando datos para actualizar el código. Una actualización instalada necesita la misma firma para conservar la app y su información.

La siguiente etapa está explicada en **docs/database/GUIA_BASE_DATOS.md**. Esa BD es una propuesta, no una funcionalidad ya instalada.
