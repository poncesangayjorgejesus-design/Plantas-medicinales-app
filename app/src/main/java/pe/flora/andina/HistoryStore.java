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
}
