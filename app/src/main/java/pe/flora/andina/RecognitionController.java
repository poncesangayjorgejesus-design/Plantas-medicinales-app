
package pe.flora.andina;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.widget.LinearLayout;
import org.json.JSONObject;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.UUID;
import java.io.IOException;
import static pe.flora.andina.ScreenUi.*;

/** Flujo de analisis y resultados. Responsable: integrante 2. */
final class RecognitionController {

    private final Activity activity;
    private final SharedPreferences prefs;
    private final ExecutorService worker;
    private final ScreenUi ui;
    private final PhotoStore photos;

    record Analysis(
            Recognizer.Result result,
            String id,
            String error
    ) {}

    RecognitionController(
            Activity activity,
            SharedPreferences prefs,
            ExecutorService worker,
            ScreenUi ui,
            PhotoStore photos
    ) {
        this.activity = activity;
        this.prefs = prefs;
        this.worker = worker;
        this.ui = ui;
        this.photos = photos;
    }

    void request(
            Bitmap photo,
            Runnable onStart,
            Consumer<Analysis> onResult,
            Consumer<String> onError
    ) {
        if (photo == null) return;

        String key = prefs.getString("key", "");

        Runnable start = () ->
                analyze(photo, key, onStart, onResult, onError);

        if (!key.isBlank()) {
            new AlertDialog.Builder(activity)
                    .setTitle("Analizar con Pl@ntNet")
                    .setMessage(
                            "Se enviará una copia de la foto " +
                                    "a Pl@ntNet para identificar la planta."
                    )
                    .setNegativeButton("Cancelar", null)
                    .setPositiveButton(
                            "Enviar foto",
                            (dialog, which) -> start.run()
                    )
                    .show();
        } else {
            start.run();
        }
    }

    private void analyze(
            Bitmap photo,
            String key,
            Runnable onStart,
            Consumer<Analysis> onResult,
            Consumer<String> onError
    ) {
        if (activity.isDestroyed() || activity.isFinishing())
            return;

        onStart.run();

        worker.execute(() -> {
            try {
                Recognizer.Result answer =
                        Recognizer.identify(activity, photo, key);

                String id = UUID.randomUUID().toString();

                try {
                    photos.save(id, photo);
                } catch (IOException e) {
                    deliver(() ->
                            onResult.accept(
                                    new Analysis(
                                            answer,
                                            "",
                                            "No se pudo guardar la foto en el historial. " +
                                                    "Libera espacio y vuelve a analizarla."
                                    )
                            )
                    );
                    return;
                }

                activity.runOnUiThread(() -> {
                    if (activity.isDestroyed()
                            || activity.isFinishing()) {
                        photos.delete(id);
                        return;
                    }

                    onResult.accept(
                            new Analysis(answer, id, "")
                    );
                });

            } catch (Exception e) {
                String msg = e.getMessage();

                String message =
                        msg != null &&
                                (msg.startsWith("Pl@ntNet")
                                        || msg.startsWith("Revisa")
                                        || msg.startsWith("Se alcanzó")
                                        || msg.startsWith("Agrega"))
                                ? msg
                                : "No se pudo analizar la foto. " +
                                  "Revisa la conexión o la configuración del modelo.";

                deliver(() -> onError.accept(message));
            }
        });
    }

    private void deliver(Runnable callback) {
        activity.runOnUiThread(() -> {
            if (!activity.isDestroyed()
                    && !activity.isFinishing()) {
                callback.run();
            }
        });
    }

    void showResults(
            LinearLayout page,
            Recognizer.Result result,
            PlantCatalog catalog,
            String resultId,
            String confirmedPlant,
            Consumer<Recognizer.Match> onConfirm
    ) {
        if (result != null) {

            ui.label(
                    page,
                    "Coincidencias posibles · " + result.engine(),
                    20,
                    INK,
                    true
            );

            if (confirmedPlant.isEmpty()
                    && (result.matches().isEmpty()
                    || result.matches().get(0).score() < 0.55f)) {

                ui.label(
                        page,
                        "Resultado incierto. Prueba con una foto " +
                                "más cercana de una hoja o flor.",
                        16,
                        INK,
                        true
                );
            }

            for (Recognizer.Match match : result.matches()) {

                JSONObject plant =
                        catalog.find(match.scientific());

                LinearLayout item = ui.card(page, -1);

                String title = plant == null
                        ? match.common()
                        : plant.optString("nombre");

                ui.label(
                        item,
                        title.isBlank()
                                ? match.scientific()
                                : title,
                        20,
                        INK,
                        true
                );

                // Nombre cientifico de la planta.
                ui.label(
                        item,
                        match.scientific(),
                        15,
                        GREEN,
                        false
                );

                // NUEVO: GRAFICO DE COINCIDENCIA.
                PlantCharts.showMatchScore(
                        activity,
                        ui,
                        item,
                        match.score()
                );

                // Mostrar ficha de la planta.
                if (plant != null) {
                    ui.action(
                            item,
                            "Ver ficha",
                            () -> catalog.detail(plant)
                    );
                } else {
                    ui.label(
                            item,
                            "Especie sin ficha en el catálogo local.",
                            14,
                            INK,
                            false
                    );
                }

                // Guardar planta identificada.
                if (!resultId.isEmpty()) {

                    boolean chosen =
                            confirmedPlant.equals(
                                    catalog.speciesKey(
                                            match.scientific()
                                    )
                            );

                    ui.action(
                            item,
                            chosen
                                    ? "Guardada en el historial"
                                    : "Guardar esta planta",
                            () -> onConfirm.accept(match)
                    ).setEnabled(!chosen);
                }
            }

            ui.label(
                    page,
                    PlantCatalog.CAUTION,
                    14,
                    INK,
                    false
            );
        }
    }
}
