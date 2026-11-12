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
}
