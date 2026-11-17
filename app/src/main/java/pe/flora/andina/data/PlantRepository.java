package pe.flora.andina.data;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import pe.flora.andina.util.TextNormalizer;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/** Lee y consulta el catálogo local; no conoce pantallas, voz ni botones. */
public final class PlantRepository {
    private final JSONArray plants;

    public PlantRepository(Context context) {
        try (InputStream input = context.getAssets().open("catalogo.json");
                ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            plants = new JSONArray(output.toString("UTF-8"));
        } catch (Exception error) {
            throw new IllegalStateException("Catálogo inválido", error);
        }
    }

    public List<JSONObject> search(String query) {
        String normalized = TextNormalizer.normalize(query);
        List<JSONObject> matches = new ArrayList<>();
        for (int i = 0; i < plants.length(); i++) {
            JSONObject plant = plants.optJSONObject(i);
            String searchable = plant.optString("nombre") + " " + plant.optString("cientifico");
            if (TextNormalizer.normalize(searchable).contains(normalized)) matches.add(plant);
        }
        return matches;
    }

    /** Encuentra una ficha por nombre científico o por uno de sus sinónimos. */
    public JSONObject find(String scientific) {
        for (int i = 0; i < plants.length(); i++) {
            JSONObject plant = plants.optJSONObject(i);
            if (plant.optString("cientifico").equalsIgnoreCase(scientific)) return plant;
            JSONArray aliases = plant.optJSONArray("alias");
            if (aliases == null) continue;
            for (int alias = 0; alias < aliases.length(); alias++) {
                if (aliases.optString(alias).equalsIgnoreCase(scientific)) return plant;
            }
        }
        return null;
    }

    public String speciesKey(String scientific) {
        JSONObject plant = find(scientific);
        return plant == null ? scientific.trim() : plant.optString("cientifico");
    }

    public String displayName(JSONObject consultation) {
        JSONObject plant = find(consultation.optString("plant"));
        if (plant != null) return plant.optString("nombre");
        String common = consultation.optString("common");
        return common.isBlank() ? consultation.optString("plant") : common;
    }
}
