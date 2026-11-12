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
    
}
