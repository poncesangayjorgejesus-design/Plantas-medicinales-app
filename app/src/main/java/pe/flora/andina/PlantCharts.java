
package pe.flora.andina;

import android.content.Context;
import android.graphics.Typeface;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static pe.flora.andina.ScreenUi.GREEN;
import static pe.flora.andina.ScreenUi.INK;

/** Graficos nativos con datos reales de la aplicacion. */
final class PlantCharts {

    private static final int TRACK = 0xffE0EAE2;
    private static final int CAUTION = 0xffB56B32;

    private PlantCharts() { }

    // Grafico de las cinco plantas mas identificadas.
    static void showTopFive(
            Context context,
            ScreenUi ui,
            LinearLayout parent,
            Map<String, Integer> totals,
            Map<String, String> names) {

        LinearLayout card = ui.card(parent, -1);
        ui.label(card, "Top 5 · plantas identificadas", 21, INK, true);

        if (totals.isEmpty()) {
            ui.label(card,
                    "Todavia no hay plantas identificadas. "
                            + "Analiza y guarda una planta para ver el grafico.",
                    15, INK, false);
            return;
        }

        int count = 0;
        for (int total : totals.values()) {
            count += total;
        }

        ui.label(card,
                count + " identificaciones registradas",
                14, GREEN, false);

        List<Map.Entry<String, Integer>> ordered =
                new ArrayList<>(totals.entrySet());

        ordered.sort((a, b) -> {
            int comparison = Integer.compare(
                    b.getValue(), a.getValue());

            return comparison != 0
                    ? comparison
                    : a.getKey().compareToIgnoreCase(b.getKey());
        });

        int max = Math.max(1, ordered.get(0).getValue());

        for (int i = 0; i < Math.min(5, ordered.size()); i++) {
            Map.Entry<String, Integer> item = ordered.get(i);

            String plantName = names.getOrDefault(
                    item.getKey(), item.getKey());

            int value = item.getValue();

            LinearLayout header = new LinearLayout(context);
            header.setOrientation(LinearLayout.HORIZONTAL);
            card.addView(header,
                    new LinearLayout.LayoutParams(-1, -2));

            TextView name = ui.label(
                    header,
                    (i + 1) + ". " + plantName,
                    16, INK, true);

            name.setMaxLines(2);
            name.setLayoutParams(
                    new LinearLayout.LayoutParams(0, -2, 1f));

            TextView number = ui.label(
                    header, String.valueOf(value),
                    16, GREEN, true);

            number.setTypeface(null, Typeface.BOLD);

            number.setPadding(
                    ui.dp(8), ui.dp(4), 0, ui.dp(7));

            addBar(context, ui, card,
                    value, max, GREEN,
                    plantName + ": " + value + " identificaciones");
        }
    }

    // Grafico del puntaje devuelto por el reconocimiento.
    static void showMatchScore(
            Context context,
            ScreenUi ui,
            LinearLayout parent,
            float rawScore) {

        int score = Math.round(
                Math.max(0f, Math.min(1f, rawScore)) * 100f);

        ui.label(parent,
                "Puntaje de coincidencia: " + score + "/100",
                14, INK, false);

        addBar(context, ui, parent,
                score, 100,
                score >= 55 ? GREEN : CAUTION,
                "Puntaje " + score + " de 100");
    }

    // Dibuja una barra horizontal.
    private static void addBar(
            Context context,
            ScreenUi ui,
            LinearLayout parent,
            int value,
            int max,
            int color,
            String description) {

        int total = Math.max(1, max);

        float ratio = Math.min(1f,
                Math.max(0f, (float) value / total));

        LinearLayout track = new LinearLayout(context);
        track.setOrientation(LinearLayout.HORIZONTAL);
        track.setBackground(ui.surface(TRACK, 10));
        track.setClipToOutline(true);
        track.setContentDescription(description);

        LinearLayout.LayoutParams space =
                new LinearLayout.LayoutParams(-1, ui.dp(14));

        space.bottomMargin = ui.dp(12);
        parent.addView(track, space);

        View fill = new View(context);
        fill.setBackground(ui.surface(color, 10));

        track.addView(fill,
                new LinearLayout.LayoutParams(0, -1, ratio));

        track.addView(new View(context),
                new LinearLayout.LayoutParams(
                        0, -1, 1f - ratio));
    }
}
