package pe.flora.andina;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.FileProvider;

import pe.flora.andina.data.HistoryStore;
import pe.flora.andina.data.PhotoStore;
import pe.flora.andina.data.PlantRepository;
import pe.flora.andina.model.ConfirmationResult;
import pe.flora.andina.model.PlantMatch;
import pe.flora.andina.model.RecognitionResult;
import pe.flora.andina.recognition.RecognitionController;
import pe.flora.andina.ui.CatalogScreen;
import pe.flora.andina.ui.HistoryPhotos;
import pe.flora.andina.ui.HistoryScreen;
import pe.flora.andina.ui.RecognitionResults;
import pe.flora.andina.ui.ScreenUi;
import pe.flora.andina.ui.SettingsScreen;
import pe.flora.andina.util.AppConstants;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Punto de entrada: navega entre pantallas y coordina captura, análisis y persistencia. */
public class MainActivity extends ComponentActivity {
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private LinearLayout root, page;
    private SharedPreferences prefs;
    private ScreenUi ui;
    private CatalogScreen catalog;
    private PlantRepository plantRepository;
    private RecognitionResults recognitionResults;
    private HistoryStore historyStore;
    private HistoryScreen historyScreen;
    private SettingsScreen settingsScreen;
    private RecognitionController recognition;
    private PhotoStore photos;
    private Uri cameraUri, photoUri;
    private Bitmap photo;
    private boolean busy;
    private RecognitionResult result;
    private String error = "";
    private int tab;
    private String resultId = "", confirmedPlant = "";

    private final ActivityResultLauncher<String> gallery =
            registerForActivityResult(
                    new ActivityResultContracts.GetContent(),
                    uri -> {
                        if (uri != null) load(uri);
                    });
    private final ActivityResultLauncher<Uri> camera =
            registerForActivityResult(
                    new ActivityResultContracts.TakePicture(),
                    ok -> {
                        if (ok && cameraUri != null) load(cameraUri);
                    });

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        initializeModules();
        if (state != null) cameraUri = state.getParcelable("capture");
        show(0);
    }

    /** Crea una instancia de cada repositorio y conecta las pantallas que lo utilizan. */
    private void initializeModules() {
        prefs = getSharedPreferences(AppConstants.PREFERENCES_NAME, MODE_PRIVATE);
        ui = new ScreenUi(this);
        plantRepository = new PlantRepository(this);
        catalog = new CatalogScreen(this, ui, plantRepository);
        recognitionResults = new RecognitionResults(this, ui);
        photos = new PhotoStore(this);
        historyStore = new HistoryStore(prefs, plantRepository, photos);
        historyScreen =
                new HistoryScreen(
                        this,
                        ui,
                        historyStore,
                        catalog,
                        plantRepository,
                        new HistoryPhotos(this, ui, photos),
                        () -> show(2));
        settingsScreen = new SettingsScreen(this, ui, prefs);
        recognition = new RecognitionController(this, prefs, worker, photos);
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        state.putParcelable("capture", cameraUri);
    }

    @Override
    protected void onDestroy() {
        worker.shutdownNow();
        if (catalog != null) catalog.close();
        super.onDestroy();
    }

    private void show(int next) {
        tab = next;
        setContentView(R.layout.activity_main);
        root = findViewById(R.id.root);
        page = findViewById(R.id.page_content);
        root.setOnApplyWindowInsetsListener(
                (view, insets) -> {
                    view.setPadding(
                            insets.getSystemWindowInsetLeft(),
                            insets.getSystemWindowInsetTop(),
                            insets.getSystemWindowInsetRight(),
                            insets.getSystemWindowInsetBottom());
                    return insets.consumeSystemWindowInsets();
                });
        root.requestApplyInsets();
        switch (next) {
            case 0 -> scan();
            case 1 -> catalog.explore(page);
            case 2 -> historyScreen.show(page);
            case 3 -> {
                settingsScreen.show(page);
                historyScreen.addClearButton(
                        page,
                        () -> {
                            resultId = "";
                            confirmedPlant = "";
                            show(2);
                        });
            }
        }
        int[] ids = {R.id.nav_scan, R.id.nav_catalog, R.id.nav_history, R.id.nav_settings};
        for (int i = 0; i < ids.length; i++) {
            int destination = i;
            Button button = findViewById(ids[i]);
            button.setSelected(i == next);
            button.setTypeface(null, i == next ? Typeface.BOLD : Typeface.NORMAL);
            button.setOnClickListener(v -> show(destination));
        }
    }

    private void scan() {
        View screen = ui.inflate(R.layout.screen_scan, page);
        ImageView preview = screen.findViewById(R.id.photo_preview);
        preview.setVisibility(photo == null ? View.GONE : View.VISIBLE);
        preview.setImageBitmap(photo);
        Button cameraButton = screen.findViewById(R.id.camera_button);
        cameraButton.setEnabled(!busy);
        cameraButton.setOnClickListener(v -> capture());
        Button galleryButton = screen.findViewById(R.id.gallery_button);
        galleryButton.setEnabled(!busy);
        galleryButton.setOnClickListener(v -> gallery.launch("image/*"));
        Button analyzeButton = screen.findViewById(R.id.analyze_button);
        analyzeButton.setVisibility(photo == null ? View.GONE : View.VISIBLE);
        analyzeButton.setEnabled(!busy);
        analyzeButton.setText(busy ? R.string.analyzing : R.string.analyze);
        analyzeButton.setOnClickListener(v -> identify());
        screen.findViewById(R.id.loading_message)
                .setVisibility(busy && photo == null ? View.VISIBLE : View.GONE);
        TextView errorView = screen.findViewById(R.id.error_message);
        errorView.setText(error);
        errorView.setVisibility(error.isEmpty() ? View.GONE : View.VISIBLE);
        recognitionResults.show(
                screen.findViewById(R.id.results_container),
                result,
                catalog,
                plantRepository,
                resultId,
                confirmedPlant,
                this::confirmPlant);
    }

    /** La persistencia informa un estado; los mensajes y la navegación pertenecen a la Activity. */
    private void confirmPlant(PlantMatch match) {
        ConfirmationResult confirmation = historyStore.confirmMatch(resultId, match);
        int message =
                switch (confirmation.status()) {
                    case SAVED -> R.string.plant_saved;
                    case NOT_FOUND -> R.string.consultation_missing;
                    case FAILED -> R.string.plant_save_failed;
                };
        if (confirmation.status() == ConfirmationResult.Status.SAVED) {
            confirmedPlant = confirmation.species();
            show(0);
        }
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
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
        worker.execute(
                () -> {
                    try {
                        Bitmap bitmap =
                                ImageDecoder.decodeBitmap(
                                        ImageDecoder.createSource(getContentResolver(), uri),
                                        (decoder, info, source) -> {
                                            decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
                                            int largest =
                                                    Math.max(
                                                            info.getSize().getWidth(),
                                                            info.getSize().getHeight());
                                            decoder.setTargetSampleSize(
                                                    Math.max(1, (int) Math.ceil(largest / 1280.0)));
                                        });
                        finishWork(
                                () -> {
                                    photo = bitmap;
                                    photoUri = uri;
                                });
                    } catch (Exception e) {
                        finishWork(
                                () -> error = "No se pudo abrir la foto. Prueba con otra imagen.");
                    }
                });
    }

    private void finishWork(Runnable update) {
        runOnUiThread(
                () -> {
                    if (isDestroyed() || isFinishing()) return;
                    busy = false;
                    update.run();
                    if (tab == 0) show(0);
                });
    }

    private void identify() {
        recognition.request(
                photo,
                this::startAnalysis,
                analysis -> {
                    finishWork(
                            () -> {
                                result = analysis.result();
                                error = analysis.error();
                                if (!analysis.id().isEmpty()) {
                                    if (historyStore.record(analysis.id(), result))
                                        resultId = analysis.id();
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
