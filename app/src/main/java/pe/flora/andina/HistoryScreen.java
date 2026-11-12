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

    void show(LinearLayout page) {
        ui.label(page, "Historial de consultas", 30, INK, true);
        EditText search = new EditText(context);
        search.setSingleLine(true);
        search.setHint("Buscar por nombre común o científico");
        search.setText(historyQuery);
        page.addView(search);
        ui.action(page, historyDate == null ? "Filtrar por fecha" : "Fecha · " + historyDate.format(
                java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")), this::chooseHistoryDate);
        if (historyDate != null) ui.action(page, "Ver todas las fechas", () -> {
            historyDate = null;
            refresh.run();
        });
        LinearLayout rows = ui.column();
        page.addView(rows);
        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence value, int start, int count, int after) {}
            public void onTextChanged(CharSequence value, int start, int before, int count) {
                historyQuery = value.toString();
                renderHistory(rows);
            }
            public void afterTextChanged(Editable value) {}
        });
        renderHistory(rows);
    }

    private void chooseHistoryDate() {
        LocalDate selected = historyDate == null ? LocalDate.now(ZoneId.of("America/Lima")) : historyDate;
        new DatePickerDialog(context, (picker, year, month, day) -> {
            historyDate = LocalDate.of(year, month + 1, day);
            refresh.run();
        }, selected.getYear(), selected.getMonthValue() - 1, selected.getDayOfMonth()).show();
    }
}
