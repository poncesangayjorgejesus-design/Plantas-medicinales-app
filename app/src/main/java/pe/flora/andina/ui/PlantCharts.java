package pe.flora.andina.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import pe.flora.andina.R;
import pe.flora.andina.util.AppConstants;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Enlaza conteos y puntajes reales con las barras declaradas en XML. */
public final class PlantCharts {
    private PlantCharts() {}

    public static void showTopFive(
            Context context,
            ScreenUi ui,
            LinearLayout parent,
            Map<String, Integer> totals,
            Map<String, String> names) {
        View card = ui.inflate(R.layout.card_top_five, parent);
        card.findViewById(R.id.chart_empty)
                .setVisibility(totals.isEmpty() ? View.VISIBLE : View.GONE);
        TextView countView = card.findViewById(R.id.chart_count);
        countView.setVisibility(totals.isEmpty() ? View.GONE : View.VISIBLE);
        if (totals.isEmpty()) return;
        int count = 0;
        for (int total : totals.values()) count += total;
        countView.setText(context.getString(R.string.chart_count, count));
        List<Map.Entry<String, Integer>> ordered = new ArrayList<>(totals.entrySet());
        ordered.sort(
                (a, b) -> {
                    int comparison = Integer.compare(b.getValue(), a.getValue());
                    return comparison != 0
                            ? comparison
                            : a.getKey().compareToIgnoreCase(b.getKey());
                });
        int max = Math.max(1, ordered.get(0).getValue());
        LinearLayout rows = card.findViewById(R.id.chart_rows);
        for (int i = 0; i < Math.min(5, ordered.size()); i++) {
            Map.Entry<String, Integer> item = ordered.get(i);
            String name = names.getOrDefault(item.getKey(), item.getKey());
            int value = item.getValue();
            View row = ui.inflate(R.layout.item_chart_bar, rows);
            ((TextView) row.findViewById(R.id.chart_label)).setText((i + 1) + ". " + name);
            ((TextView) row.findViewById(R.id.chart_value)).setText(String.valueOf(value));
            ProgressBar bar = row.findViewById(R.id.chart_progress);
            bar.setProgress(Math.round((float) value / max * 1000));
            bar.setContentDescription(context.getString(R.string.chart_description, name, value));
        }
    }

    public static void showMatchScore(
            Context context, ScreenUi ui, LinearLayout parent, float rawScore) {
        int score = Math.round(Math.max(0f, Math.min(1f, rawScore)) * 100f);
        View row = ui.inflate(R.layout.view_match_score, parent);
        ((TextView) row.findViewById(R.id.score_text))
                .setText(context.getString(R.string.score, score));
        ProgressBar bar = row.findViewById(R.id.score_progress);
        bar.setMax(100);
        bar.setProgress(score);
        bar.setProgressTintList(
                ColorStateList.valueOf(
                        context.getColor(
                                score >= Math.round(AppConstants.ACCEPTANCE_SCORE * 100)
                                        ? R.color.green
                                        : R.color.caution)));
        bar.setContentDescription(context.getString(R.string.score_description, score));
    }
}
