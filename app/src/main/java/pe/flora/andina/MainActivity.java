package pe.flora.andina;

import android.content.SharedPreferences;
import android.graphics.*;
import android.net.Uri;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.FileProvider;
import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

import static pe.flora.andina.ScreenUi.*;

public class MainActivity extends ComponentActivity {
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private LinearLayout root, page;
    private SharedPreferences prefs;
    private ScreenUi ui;
    private PlantCatalog catalog;
    private HistoryStore historyStore;
    private HistoryScreen historyScreen;
    private SettingsScreen settingsScreen;
    private RecognitionController recognition;
    private PhotoStore photos;
    private Uri cameraUri, photoUri;
    private Bitmap photo;
    private boolean busy;
    private Recognizer.Result result;
    private String error = "";
    private int tab;
    private String resultId = "", confirmedPlant = "";

    private final ActivityResultLauncher<String> gallery = registerForActivityResult(
            new ActivityResultContracts.GetContent(), uri -> { if (uri != null) load(uri); });
    private final ActivityResultLauncher<Uri> camera = registerForActivityResult(
            new ActivityResultContracts.TakePicture(), ok -> { if (ok && cameraUri != null) load(cameraUri); });

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("flora", MODE_PRIVATE);
        if (state != null) cameraUri = state.getParcelable("capture");
        ui = new ScreenUi(this);
        catalog = new PlantCatalog(this, ui);
        photos = new PhotoStore(this);
        historyStore = new HistoryStore(this, prefs, catalog, photos);
        historyScreen = new HistoryScreen(this, ui, historyStore, catalog,
                new HistoryPhotos(this, ui, photos), () -> show(2));
        settingsScreen = new SettingsScreen(this, ui, prefs);
        recognition = new RecognitionController(this, prefs, worker, ui, photos);
        show(0);
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        state.putParcelable("capture", cameraUri);
    }

    @Override protected void onDestroy() {
        worker.shutdownNow();
        if (catalog != null) catalog.close();
        super.onDestroy();
    }

    private void show(int next) {
        tab = next;
        root = ui.column();
        root.setBackgroundColor(PAPER);
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            view.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(),
                    insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            return insets.consumeSystemWindowInsets();
        });
        setContentView(root);
        root.requestApplyInsets();

        ScrollView scroll = new ScrollView(this);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        page = ui.column();
        page.setPadding(ui.dp(20), ui.dp(16), ui.dp(20), ui.dp(16));
        scroll.addView(page);
        ui.label(page, "FLORA ANDINA", 14, GREEN, true);
        switch (next) {
            case 0 -> scan();
            case 1 -> catalog.explore(page);
            case 2 -> historyScreen.show(page);
            case 3 -> {
                settingsScreen.show(page);
                historyScreen.addClearButton(page, () -> {
                    resultId = "";
                    confirmedPlant = "";
                    show(2);
                });
            }
        }

        LinearLayout nav = new LinearLayout(this);
        nav.setBackgroundColor(0xffE1ECDD);
        root.addView(nav);
        String[] names = {"Escanear", "Explorar", "Historial", "Ajustes"};
        for (int i = 0; i < names.length; i++) {
            int destination = i;
            Button button = new Button(this, null, android.R.attr.borderlessButtonStyle);
            button.setText(names[i]);
            button.setTextSize(12);
            button.setTextColor(i == next ? GREEN : INK);
            button.setSelected(i == next);
            button.setAllCaps(false);
            button.setPadding(0, 0, 0, 0);
            if (i == next) button.setTypeface(null, Typeface.BOLD);
            button.setOnClickListener(v -> show(destination));
            nav.addView(button, new LinearLayout.LayoutParams(0, ui.dp(56), 1));
        }
    }

    private void scan() {
        ui.label(page, "Identifica una planta", 30, INK, true);
        LinearLayout hero = ui.card(page, GREEN);
        ui.label(hero, "Fotografía hojas o flores", 23, -1, true);
        ui.label(hero, "Enfoca una sola planta. Acércate y evita sombras fuertes.", 16, 0xffE1ECDD, false);
        if (photo != null) {
            ImageView preview = new ImageView(this);
            preview.setImageBitmap(photo);
            preview.setScaleType(ImageView.ScaleType.FIT_CENTER);
            preview.setContentDescription("Foto que se analizará");
            page.addView(preview, new LinearLayout.LayoutParams(-1, ui.dp(210)));
        }
        ui.action(page, "Abrir cámara", this::capture).setEnabled(!busy);
        ui.action(page, "Elegir de la galería", () -> gallery.launch("image/*")).setEnabled(!busy);
        if (photo != null) ui.action(page, busy ? "Analizando…" : "Analizar foto", this::identify).setEnabled(!busy);
        if (busy && photo == null) ui.label(page, "Cargando imagen…", 16, GREEN, true);
        if (!error.isEmpty()) ui.label(page, error, 16, 0xff8B382B, true);
        recognition.showResults(page, result, catalog, resultId, confirmedPlant,
                match -> historyStore.confirmMatch(resultId, match, species -> {
                    confirmedPlant = species;
                    show(0);
                }));
    }

    private void capture() {
        try {
            File dir = new File(getCacheDir(), "capturas");
            if (!dir.exists() && !dir.mkdirs()) throw new IllegalStateException();
            File file = new File(dir, "planta.jpg");
            if (file.exists() && !file.delete()) throw new IllegalStateException();
            cameraUri = FileProvider.getUriForFile(this, getPackageName() + ".photos", file);
            camera.launch(cameraUri);
        } catch (RuntimeException e) {
            error = "Cámara no disponible. Puedes elegir una imagen de la galería.";
            show(0);
        }
    }

    private void load(Uri uri) {
        busy = true;
        photo = null;
        photoUri = null;
        result = null;
        resultId = "";
        confirmedPlant = "";
        error = "";
        show(0);
        worker.execute(() -> {
            try {
                Bitmap bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(getContentResolver(), uri),
                        (decoder, info, source) -> {
                            decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
                            int largest = Math.max(info.getSize().getWidth(), info.getSize().getHeight());
                            decoder.setTargetSampleSize(Math.max(1, (int) Math.ceil(largest / 1280.0)));
                        });
                finishWork(() -> { photo = bitmap; photoUri = uri; });
            } catch (Exception e) {
                finishWork(() -> error = "No se pudo abrir la foto. Prueba con otra imagen.");
            }
        });
    }

    private void finishWork(Runnable update) {
        runOnUiThread(() -> {
            if (isDestroyed() || isFinishing()) return;
            busy = false;
            update.run();
            if (tab == 0) show(0);
        });
    }

    private void identify() {
        recognition.request(photo, this::startAnalysis,
                analysis -> {
                    finishWork(() -> {
                        result = analysis.result();
                        error = analysis.error();
                        if (!analysis.id().isEmpty()) {
                            if (historyStore.record(analysis.id(), result)) resultId = analysis.id();
                            else {
                                error = "No se pudo guardar la consulta.";
                                photos.delete(analysis.id());
                            }
                        }
                    });
                    if (tab == 2 && !analysis.id().isEmpty()) show(2);
                },
                message -> finishWork(() -> error = message));
    }

    private void startAnalysis() {
        resultId = "";
        confirmedPlant = "";
        busy = true;
        error = "";
        result = null;
        if (photoUri != null && photoUri.equals(cameraUri)) {
            new File(getCacheDir(), "capturas/planta.jpg").delete();
            photoUri = null;
        }
        show(0);
    }
}
