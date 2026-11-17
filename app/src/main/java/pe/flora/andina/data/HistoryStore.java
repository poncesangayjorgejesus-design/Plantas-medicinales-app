package pe.flora.andina.data;

import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import pe.flora.andina.model.ConfirmationResult;
import pe.flora.andina.model.HistorySnapshot;
import pe.flora.andina.model.PlantMatch;
import pe.flora.andina.model.RecognitionResult;
import pe.flora.andina.util.AppConstants;
import pe.flora.andina.util.TextNormalizer;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Repositorio actual del historial en SharedPreferences. Aquí se reemplazará la persistencia por
 * Room.
 */
public final class HistoryStore {
    private final SharedPreferences prefs;
    private final PlantRepository repository;
    private final PhotoStore photos;

    public HistoryStore(SharedPreferences prefs, PlantRepository repository, PhotoStore photos) {
        this.prefs = prefs;
        this.repository = repository;
        this.photos = photos;
    }

    public void clear() {
        JSONArray rows = logs();
        prefs.edit().remove(AppConstants.HISTORY_KEY).apply();
        for (int i = 0; i < rows.length(); i++) photos.delete(rows.optJSONObject(i));
    }

    public JSONArray logs() {
        try {
            return new JSONArray(prefs.getString(AppConstants.HISTORY_KEY, "[]"));
        } catch (JSONException e) {
            return new JSONArray();
        }
    }

    public boolean record(String id, RecognitionResult answer) {
        JSONArray previous = logs(), updated = new JSONArray();
        PlantMatch top = answer.matches().isEmpty() ? null : answer.matches().get(0);
        boolean accepted = top != null && top.score() >= AppConstants.ACCEPTANCE_SCORE;

        try {
            updated.put(
                    new JSONObject()
                            .put("id", id)
                            .put("date", System.currentTimeMillis())
                            .put("plant", accepted ? repository.speciesKey(top.scientific()) : "")
                            .put("common", accepted ? top.common() : "")
                            .put("engine", answer.engine()));

            for (int i = 0; i < Math.min(previous.length(), AppConstants.HISTORY_LIMIT - 1); i++)
                updated.put(previous.getJSONObject(i));

            prefs.edit().putString(AppConstants.HISTORY_KEY, updated.toString()).apply();

            for (int i = AppConstants.HISTORY_LIMIT - 1; i < previous.length(); i++)
                photos.delete(previous.optJSONObject(i));

            return true;
        } catch (JSONException e) {
            return false;
        }
    }

    /** Elimina una consulta y borra su foto solo después de persistir el cambio. */
    public boolean deleteOne(String id) {
        if (id == null || id.isEmpty()) return false;

        JSONArray previous = logs();
        JSONArray remaining = new JSONArray();
        JSONObject removed = null;

        for (int i = 0; i < previous.length(); i++) {
            JSONObject row = previous.optJSONObject(i);

            if (removed == null && row != null && id.equals(row.optString("id"))) {
                removed = row;
            } else {
                remaining.put(previous.opt(i));
            }
        }

        if (removed == null) return false;

        boolean saved =
                prefs.edit().putString(AppConstants.HISTORY_KEY, remaining.toString()).commit();

        if (saved) photos.delete(removed);

        return saved;
    }

    /** Actualiza la misma fila; la pantalla decide cómo presentar el resultado. */
    public ConfirmationResult confirmMatch(String id, PlantMatch match) {
        JSONArray rows = logs();
        for (int i = 0; i < rows.length(); i++) {
            JSONObject row = rows.optJSONObject(i);
            if (!id.equals(row.optString("id"))) continue;
            try {
                String species = repository.speciesKey(match.scientific());
                row.put("plant", species).put("common", match.common());
                prefs.edit().putString(AppConstants.HISTORY_KEY, rows.toString()).apply();
                return new ConfirmationResult(ConfirmationResult.Status.SAVED, species);
            } catch (JSONException error) {
                return new ConfirmationResult(ConfirmationResult.Status.FAILED, "");
            }
        }
        return new ConfirmationResult(ConfirmationResult.Status.NOT_FOUND, "");
    }

    /** Usa los mismos filtros para el listado y el Top 5. */
    public HistorySnapshot query(LocalDate selectedDate, String searchText) {
        JSONArray rows = logs();

        List<JSONObject> filtered = new ArrayList<>();
        Map<String, Integer> totals = new TreeMap<>();
        Map<String, String> names = new HashMap<>();

        ZoneId lima = AppConstants.HISTORY_ZONE;
        String query = TextNormalizer.normalize(searchText);

        for (int i = 0; i < rows.length(); i++) {
            JSONObject row = rows.optJSONObject(i);
            if (row == null) continue;

            String plant = row.optString("plant");

            LocalDate date = Instant.ofEpochMilli(row.optLong("date")).atZone(lima).toLocalDate();

            if (selectedDate != null && !selectedDate.equals(date)) {
                continue;
            }

            String name = repository.displayName(row);

            // Aplica los filtros al historial y al grafico.
            if (!TextNormalizer.normalize(name + " " + plant).contains(query)) {
                continue;
            }

            filtered.add(row);

            // Cuenta registros con una especie aceptada, automática o manualmente.
            if (!plant.isEmpty()) {
                String species = repository.speciesKey(plant);

                totals.merge(species, 1, Integer::sum);
                names.putIfAbsent(species, name);
            }
        }

        return new HistorySnapshot(filtered, totals, names);
    }
}
