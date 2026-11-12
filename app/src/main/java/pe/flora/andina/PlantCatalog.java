package pe.flora.andina;

import android.app.AlertDialog;
import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.text.*;
import android.widget.*;
import org.json.*;
import java.text.Normalizer;
import java.util.Locale;
import static pe.flora.andina.ScreenUi.*;

final class PlantCatalog {
    static final String CAUTION = "Identificación orientativa. No ingieras ni apliques una planta identificada solo por fotografía.";
    private final Context context;
    private final ScreenUi ui;
    private final JSONArray catalog;
    private TextToSpeech voice;
    private boolean voiceReady;

    PlantCatalog(Context context, ScreenUi ui) {
        this.context = context;
        this.ui = ui;
        try { catalog = new JSONArray(Recognizer.read(context.getAssets().open("catalogo.json"))); }
        catch (Exception e) { throw new IllegalStateException("Catálogo inválido", e); }
        voice = new TextToSpeech(context, status -> {
            if (status == TextToSpeech.SUCCESS && voice != null)
                voiceReady = voice.setLanguage(new Locale("es", "PE")) >= 0;
        });
    }

    void close() { if (voice != null) voice.shutdown(); }

    static String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT).trim();
    }

    JSONObject find(String scientific) {
        for (int i = 0; i < catalog.length(); i++) {
            JSONObject item = catalog.optJSONObject(i);
            if (item.optString("cientifico").equalsIgnoreCase(scientific)) return item;
            JSONArray aliases = item.optJSONArray("alias");
            if (aliases != null) {
                for (int j = 0; j < aliases.length(); j++) {
                    if (aliases.optString(j).equalsIgnoreCase(scientific)) return item;
                }
            }
        }
        return null;
    }

    void explore(LinearLayout page) {
        ui.label(page, "Catálogo medicinal", 30, INK, true);
        EditText query = new EditText(context);
        query.setSingleLine(true);
        query.setHint("Nombre común o científico");
        page.addView(query);
        LinearLayout rows = ui.column();
        page.addView(rows);
        Runnable refresh = () -> {
            rows.removeAllViews();
            String search = normalize(query.getText().toString());
            for (int i = 0; i < catalog.length(); i++) {
                JSONObject plant = catalog.optJSONObject(i);
                if (!normalize(plant.optString("nombre") + " " + plant.optString("cientifico")).contains(search)) continue;
                LinearLayout item = ui.card(rows, -1);
                ui.label(item, plant.optString("nombre"), 21, INK, true);
                ui.label(item, plant.optString("cientifico"), 15, GREEN, false);
                ui.action(item, "Abrir ficha", () -> detail(plant));
            }
            if (rows.getChildCount() == 0) ui.label(rows, "Sin coincidencias", 16, INK, false);
        };
        query.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) { refresh.run(); }
            public void afterTextChanged(Editable value) {}
        });
        refresh.run();
    }

    void detail(JSONObject plant) {
        String message = plant.optString("cientifico") + "\n" + plant.optString("familia") + "\n\n"
                + "Distribución\n" + plant.optString("region") + "\n\n"
                + "Uso tradicional documentado\n" + plant.optString("tradicion") + "\n\n"
                + CAUTION + "\n\nFuente\n" + plant.optString("fuente");
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle(plant.optString("nombre")).setMessage(message)
                .setPositiveButton("Cerrar", null).setNeutralButton("Escuchar", null)
                .setNegativeButton("Detener audio", null).create();
        dialog.setOnDismissListener(ignored -> { if (voice != null) voice.stop(); });
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(view -> {
            if (voiceReady) voice.speak(plant.optString("nombre") + ". " + message,
                    TextToSpeech.QUEUE_FLUSH, null, "ficha");
            else Toast.makeText(context, "Configura una voz en español en Android.", Toast.LENGTH_SHORT).show();
        });
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(view -> {
            if (voice != null) voice.stop();
        });
    }

    String speciesKey(String name) {
        JSONObject plant = find(name);
        return plant == null ? name.trim() : plant.optString("cientifico");
    }

    String displayName(JSONObject row) {
        JSONObject plant = find(row.optString("plant"));
        if (plant != null) return plant.optString("nombre");
        String common = row.optString("common");
        return common.isBlank() ? row.optString("plant") : common;
    }

}
