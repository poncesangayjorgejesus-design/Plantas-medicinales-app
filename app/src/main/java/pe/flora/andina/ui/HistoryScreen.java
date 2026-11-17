package pe.flora.andina.ui;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import pe.flora.andina.R;
import pe.flora.andina.data.HistoryStore;
import pe.flora.andina.data.PlantRepository;
import pe.flora.andina.model.HistorySnapshot;
import pe.flora.andina.util.AppConstants;

import java.text.DateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

/** Presenta filtros, consultas y acciones; solicita los datos al repositorio. */
public final class HistoryScreen {
    private final Context context;
    private final ScreenUi ui;
    private final HistoryStore store;
    private final CatalogScreen catalogScreen;
    private final PlantRepository repository;
    private final HistoryPhotos photos;
    private final Runnable refresh;
    private LocalDate historyDate;
    private String historyQuery = "";

    public HistoryScreen(
            Context context,
            ScreenUi ui,
            HistoryStore store,
            CatalogScreen catalogScreen,
            PlantRepository repository,
            HistoryPhotos photos,
            Runnable refresh) {
        this.context = context;
        this.ui = ui;
        this.store = store;
        this.catalogScreen = catalogScreen;
        this.repository = repository;
        this.photos = photos;
        this.refresh = refresh;
    }

    public void addClearButton(LinearLayout page, Runnable afterClear) {
        page.findViewById(R.id.clear_history)
                .setOnClickListener(
                        v ->
                                new AlertDialog.Builder(context)
                                        .setTitle("Borrar todas las consultas")
                                        .setMessage(
                                                "Se eliminará el historial de este dispositivo.")
                                        .setNegativeButton("Cancelar", null)
                                        .setPositiveButton(
                                                "Borrar",
                                                (d, which) -> {
                                                    store.clear();
                                                    afterClear.run();
                                                })
                                        .show());
    }

    public void show(LinearLayout page) {
        View screen = ui.inflate(R.layout.screen_history, page);
        EditText search = screen.findViewById(R.id.history_query);
        search.setText(historyQuery);
        Button dateButton = screen.findViewById(R.id.date_filter);
        dateButton.setText(
                historyDate == null
                        ? context.getString(R.string.date_filter)
                        : context.getString(
                                R.string.date_selected,
                                historyDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))));
        dateButton.setOnClickListener(v -> chooseHistoryDate());
        screen.findViewById(R.id.clear_date)
                .setVisibility(historyDate == null ? View.GONE : View.VISIBLE);
        screen.findViewById(R.id.clear_date)
                .setOnClickListener(
                        v -> {
                            historyDate = null;
                            refresh.run();
                        });
        LinearLayout rows = screen.findViewById(R.id.history_rows);
        search.addTextChangedListener(
                new TextWatcher() {
                    public void beforeTextChanged(
                            CharSequence value, int start, int count, int after) {}

                    public void onTextChanged(
                            CharSequence value, int start, int before, int count) {
                        historyQuery = value.toString();
                        renderHistory(rows);
                    }

                    public void afterTextChanged(Editable value) {}
                });
        renderHistory(rows);
    }

    private void chooseHistoryDate() {
        LocalDate selected =
                historyDate == null ? LocalDate.now(AppConstants.HISTORY_ZONE) : historyDate;

        new DatePickerDialog(
                        context,
                        (picker, year, month, day) -> {
                            historyDate = LocalDate.of(year, month + 1, day);
                            refresh.run();
                        },
                        selected.getYear(),
                        selected.getMonthValue() - 1,
                        selected.getDayOfMonth())
                .show();
    }

    private void renderHistory(LinearLayout rowsView) {
        rowsView.removeAllViews();

        HistorySnapshot snapshot = store.query(historyDate, historyQuery);
        List<JSONObject> filtered = snapshot.rows();
        Map<String, Integer> totals = snapshot.totals();
        Map<String, String> names = snapshot.names();

        // El gráfico recibe exactamente los mismos registros que el listado.
        PlantCharts.showTopFive(context, ui, rowsView, totals, names);

        ui.message(rowsView, context.getString(R.string.count_consultations, filtered.size()));

        DateFormat dates =
                DateFormat.getDateTimeInstance(
                        DateFormat.SHORT, DateFormat.SHORT, new Locale("es", "PE"));

        dates.setTimeZone(TimeZone.getTimeZone("America/Lima"));

        for (JSONObject row : filtered) {
            addConsultationRow(rowsView, dates, row);
        }

        if (filtered.isEmpty())
            ui.message(
                    rowsView,
                    context.getString(
                            store.logs().length() == 0
                                    ? R.string.history_empty
                                    : R.string.history_no_filter_matches));
    }

    /** Enlaza una tarjeta XML y sus acciones sin modificar otras consultas. */
    private void addConsultationRow(LinearLayout rowsView, DateFormat dates, JSONObject row) {
        String plant = row.optString("plant");
        View entry = ui.inflate(R.layout.item_history, rowsView);
        ((TextView) entry.findViewById(R.id.history_name))
                .setText(plant.isEmpty() ? "Resultado incierto" : repository.displayName(row));
        TextView scientific = entry.findViewById(R.id.history_scientific);
        scientific.setText(plant);
        scientific.setVisibility(
                !plant.isEmpty() && !plant.equals(repository.displayName(row))
                        ? View.VISIBLE
                        : View.GONE);
        ((TextView) entry.findViewById(R.id.history_metadata))
                .setText(
                        dates.format(new Date(row.optLong("date")))
                                + " · "
                                + row.optString("engine"));
        photos.show(entry.findViewById(R.id.history_photo_container), row);
        JSONObject match = repository.find(plant);
        entry.findViewById(R.id.history_detail)
                .setVisibility(match == null ? View.GONE : View.VISIBLE);
        entry.findViewById(R.id.history_detail)
                .setOnClickListener(v -> catalogScreen.detail(match));
        String consultationId = row.optString("id");
        entry.findViewById(R.id.delete_consultation)
                .setVisibility(consultationId.isEmpty() ? View.GONE : View.VISIBLE);
        entry.findViewById(R.id.delete_consultation)
                .setOnClickListener(
                        v ->
                                new AlertDialog.Builder(context)
                                        .setTitle("Eliminar consulta")
                                        .setMessage(
                                                "¿Eliminar esta consulta y su fotografía? Las"
                                                        + " demás consultas se conservarán.")
                                        .setNegativeButton("Cancelar", null)
                                        .setPositiveButton(
                                                "Eliminar",
                                                (dialog, which) -> {
                                                    if (store.deleteOne(consultationId)) {
                                                        Toast.makeText(
                                                                        context,
                                                                        R.string
                                                                                .consultation_deleted,
                                                                        Toast.LENGTH_SHORT)
                                                                .show();
                                                        renderHistory(rowsView);
                                                    } else {
                                                        Toast.makeText(
                                                                        context,
                                                                        R.string.delete_failed,
                                                                        Toast.LENGTH_LONG)
                                                                .show();
                                                    }
                                                })
                                        .show());
    }
}
