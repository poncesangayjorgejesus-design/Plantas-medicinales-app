
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
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Borrar", (d, which) -> {
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

        ui.action(page, historyDate == null
                        ? "Filtrar por fecha"
                        : "Fecha · " + historyDate.format(
                        java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                this::chooseHistoryDate);

        if (historyDate != null) {
            ui.action(page, "Ver todas las fechas", () -> {
                historyDate = null;
                refresh.run();
            });
        }

        LinearLayout rows = ui.column();
        page.addView(rows);

        search.addTextChangedListener(new TextWatcher() {
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
        LocalDate selected = historyDate == null
                ? LocalDate.now(ZoneId.of("America/Lima"))
                : historyDate;

        new DatePickerDialog(
                context,
                (picker, year, month, day) -> {
                    historyDate = LocalDate.of(year, month + 1, day);
                    refresh.run();
                },
                selected.getYear(),
                selected.getMonthValue() - 1,
                selected.getDayOfMonth()
        ).show();
    }

    private void renderHistory(LinearLayout rowsView) {
        rowsView.removeAllViews();

        JSONArray rows = store.logs();

        List<JSONObject> filtered = new ArrayList<>();
        Map<String, Integer> totals = new TreeMap<>();
        Map<String, String> names = new HashMap<>();

        ZoneId lima = ZoneId.of("America/Lima");
        String query = PlantCatalog.normalize(historyQuery);

        for (int i = 0; i < rows.length(); i++) {
            JSONObject row = rows.optJSONObject(i);
            if (row == null) continue;

            String plant = row.optString("plant");

            LocalDate date = Instant.ofEpochMilli(
                            row.optLong("date"))
                    .atZone(lima)
                    .toLocalDate();

            if (historyDate != null
                    && !historyDate.equals(date)) {
                continue;
            }

            String name = catalog.displayName(row);

            // Aplica los filtros al historial y al grafico.
            if (!PlantCatalog.normalize(name + " " + plant)
                    .contains(query)) {
                continue;
            }

            filtered.add(row);

            // Cuenta solo identificaciones confirmadas.
            if (!plant.isEmpty()) {
                String species = catalog.speciesKey(plant);

                totals.merge(species, 1, Integer::sum);
                names.putIfAbsent(species, name);
            }
        }

        // NUEVO: grafico de barras Top 5.
        PlantCharts.showTopFive(
                context,
                ui,
                rowsView,
                totals,
                names
        );

        ui.label(
                rowsView,
                filtered.size() + " consultas",
                16, GREEN, true
        );

        DateFormat dates = DateFormat.getDateTimeInstance(
                DateFormat.SHORT,
                DateFormat.SHORT,
                new Locale("es", "PE")
        );

        dates.setTimeZone(
                TimeZone.getTimeZone("America/Lima")
        );

        for (JSONObject row : filtered) {
            String plant = row.optString("plant");

            LinearLayout entry = ui.card(rowsView, -1);

            ui.label(
                    entry,
                    plant.isEmpty()
                            ? "Resultado incierto"
                            : catalog.displayName(row),
                    18, INK, true
            );

            if (!plant.isEmpty()
                    && !plant.equals(catalog.displayName(row))) {
                ui.label(entry, plant, 15, GREEN, false);
            }

            ui.label(
                    entry,
                    dates.format(new Date(row.optLong("date")))
                            + " · " + row.optString("engine"),
                    14, GREEN, false
            );

            // Fotografia de la consulta.
            photos.show(entry, row);

            // Acceso a la ficha de la planta.
            JSONObject match = catalog.find(plant);

            if (match != null) {
                ui.action(
                        entry,
                        "Abrir ficha",
                        () -> catalog.detail(match)
                );
            }

            // Eliminar solamente esta consulta.
            String consultationId = row.optString("id");

            if (!consultationId.isEmpty()) {
                ui.action(entry, "Eliminar consulta", () ->
                        new AlertDialog.Builder(context)
                                .setTitle("Eliminar consulta")
                                .setMessage(
                                        "¿Eliminar esta consulta y su fotografía? "
                                                + "Las demás consultas se conservarán."
                                )
                                .setNegativeButton("Cancelar", null)
                                .setPositiveButton(
                                        "Eliminar",
                                        (dialog, which) -> {
                                            if (store.deleteOne(consultationId)) {
                                                Toast.makeText(
                                                        context,
                                                        "Consulta eliminada",
                                                        Toast.LENGTH_SHORT
                                                ).show();

                                                // Actualizar historial y grafico.
                                                renderHistory(rowsView);
                                            } else {
                                                Toast.makeText(
                                                        context,
                                                        "No se pudo eliminar la consulta",
                                                        Toast.LENGTH_LONG
                                                ).show();
                                            }
                                        }
                                )
                                .show()
                );
            }
        }

        if (filtered.isEmpty()) {
            ui.label(
                    rowsView,
                    rows.length() == 0
                            ? "Aquí verás tus análisis de fotos."
                            : "Sin consultas para esos filtros.",
                    16,
                    INK,
                    false
            );
        }
    }
}
