package pe.flora.andina.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import pe.flora.andina.R;
import pe.flora.andina.data.PlantRepository;

import java.util.Locale;

/** Presenta catálogo y fichas, y controla la lectura por voz. */
public final class CatalogScreen {
    static final String CAUTION =
            "Identificación orientativa. No ingieras ni apliques una planta identificada solo por"
                    + " fotografía.";
    private final Context context;
    private final ScreenUi ui;
    private final PlantRepository repository;
    private TextToSpeech voice;
    private boolean voiceReady;

    public CatalogScreen(Context context, ScreenUi ui, PlantRepository repository) {
        this.context = context;
        this.ui = ui;
        this.repository = repository;
        voice =
                new TextToSpeech(
                        context,
                        status -> {
                            if (status == TextToSpeech.SUCCESS && voice != null)
                                voiceReady = voice.setLanguage(new Locale("es", "PE")) >= 0;
                        });
    }

    public void close() {
        if (voice != null) voice.shutdown();
    }

    public void explore(LinearLayout page) {
        View screen = ui.inflate(R.layout.screen_catalog, page);
        EditText query = screen.findViewById(R.id.catalog_query);
        LinearLayout rows = screen.findViewById(R.id.catalog_rows);
        Runnable refresh =
                () -> {
                    rows.removeAllViews();
                    for (JSONObject plant : repository.search(query.getText().toString())) {
                        View item = ui.inflate(R.layout.item_plant, rows);
                        ((TextView) item.findViewById(R.id.plant_name))
                                .setText(plant.optString("nombre"));
                        ((TextView) item.findViewById(R.id.plant_scientific))
                                .setText(plant.optString("cientifico"));
                        item.findViewById(R.id.plant_detail).setOnClickListener(v -> detail(plant));
                    }
                    if (rows.getChildCount() == 0)
                        ui.message(rows, context.getString(R.string.no_matches));
                };
        query.addTextChangedListener(
                new TextWatcher() {
                    public void beforeTextChanged(
                            CharSequence s, int start, int count, int after) {}

                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        refresh.run();
                    }

                    public void afterTextChanged(Editable value) {}
                });
        refresh.run();
    }

    public void detail(JSONObject plant) {
        String message =
                plant.optString("cientifico")
                        + "\n"
                        + plant.optString("familia")
                        + "\n\n"
                        + "Distribución\n"
                        + plant.optString("region")
                        + "\n\n"
                        + "Uso tradicional documentado\n"
                        + plant.optString("tradicion")
                        + "\n\n"
                        + CAUTION;
        View content = ui.dialog(R.layout.dialog_plant);
        ((TextView) content.findViewById(R.id.detail_name)).setText(plant.optString("nombre"));
        ((TextView) content.findViewById(R.id.detail_content)).setText(message);
        AlertDialog dialog = new AlertDialog.Builder(context).setView(content).create();
        dialog.setOnDismissListener(
                ignored -> {
                    if (voice != null) voice.stop();
                });
        content.findViewById(R.id.speak_button)
                .setOnClickListener(
                        v -> {
                            if (voiceReady)
                                voice.speak(
                                        plant.optString("nombre") + ". " + message,
                                        TextToSpeech.QUEUE_FLUSH,
                                        null,
                                        "ficha");
                            else
                                Toast.makeText(context, R.string.voice_missing, Toast.LENGTH_SHORT)
                                        .show();
                        });
        content.findViewById(R.id.stop_speech)
                .setOnClickListener(
                        v -> {
                            if (voice != null) voice.stop();
                        });
        content.findViewById(R.id.close_detail).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }
}
