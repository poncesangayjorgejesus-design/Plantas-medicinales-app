package pe.flora.andina;

import android.content.Context;
import android.content.SharedPreferences;
import android.widget.Toast;
import org.json.*;
import java.util.function.Consumer;

final class HistoryStore {
    private final Context context;
    private final SharedPreferences prefs;
    private final PlantCatalog catalog;
    private final PhotoStore photos;
    HistoryStore(Context context, SharedPreferences prefs, PlantCatalog catalog, PhotoStore photos) {
        this.context = context;
        this.prefs = prefs;
        this.catalog = catalog;
        this.photos = photos;
    }

    void clear() {
        JSONArray rows = logs();
        prefs.edit().remove("history").apply();
        for (int i = 0; i < rows.length(); i++) photos.delete(rows.optJSONObject(i));
    }

    JSONArray logs() {
        try { return new JSONArray(prefs.getString("history", "[]")); }
        catch (JSONException e) { return new JSONArray(); }
    }

    boolean record(String id, Recognizer.Result answer) {
        JSONArray previous = logs(), updated = new JSONArray();
        Recognizer.Match top = answer.matches().isEmpty() ? null : answer.matches().get(0);
        boolean accepted = top != null && top.score() >= 0.55f;
        try {
            updated.put(new JSONObject().put("id", id).put("date", System.currentTimeMillis())
                    .put("plant", accepted ? catalog.speciesKey(top.scientific()) : "")
                    .put("common", accepted ? top.common() : "").put("engine", answer.engine()));
            for (int i = 0; i < Math.min(previous.length(), 199); i++) updated.put(previous.getJSONObject(i));
            prefs.edit().putString("history", updated.toString()).apply();
            for (int i = 199; i < previous.length(); i++) photos.delete(previous.optJSONObject(i));
            return true;
        } catch (JSONException e) {
            return false;
        }
    }
}
