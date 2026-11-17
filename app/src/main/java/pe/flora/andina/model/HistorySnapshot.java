package pe.flora.andina.model;

import org.json.JSONObject;

import java.util.List;
import java.util.Map;

/** Vista filtrada del historial y conteos que alimentan el gráfico. */
public record HistorySnapshot(
        List<JSONObject> rows, Map<String, Integer> totals, Map<String, String> names) {}
