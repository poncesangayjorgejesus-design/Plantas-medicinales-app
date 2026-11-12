package pe.flora.andina;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.widget.*;
import android.text.*;
import org.json.*;
import java.time.*;
import java.text.DateFormat;
import java.util.*;
import static pe.flora.andina.ScreenUi.*;

final class HistoryScreen {
    private final Context context;
    private final ScreenUi ui;
    private final HistoryStore store;
    private final PlantCatalog catalog;
    private final HistoryPhotos photos;
    private final Runnable refresh;
    private LocalDate historyDate;
    private String historyQuery = "";

    HistoryScreen(Context context, ScreenUi ui, HistoryStore store, PlantCatalog catalog,
                  HistoryPhotos photos, Runnable refresh) {
        this.context = context;
        this.ui = ui;
        this.store = store;
        this.catalog = catalog;
        this.photos = photos;
        this.refresh = refresh;
    }

    void addClearButton(LinearLayout page, Runnable afterClear) {
        ui.action(page, "Borrar historial", () -> new AlertDialog.Builder(context)
                .setTitle("Borrar todas las consultas")
                .setMessage("Se eliminará el historial de este dispositivo.")
                .setNegativeButton("Cancelar", null).setPositiveButton("Borrar", (d, which) -> {
                    store.clear();
                    afterClear.run();
                }).show());
    }
}
