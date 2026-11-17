package pe.flora.andina.ui;

import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONObject;

import pe.flora.andina.R;
import pe.flora.andina.data.PlantRepository;
import pe.flora.andina.model.PlantMatch;
import pe.flora.andina.model.RecognitionResult;
import pe.flora.andina.util.AppConstants;

import java.util.function.Consumer;

/** Presenta candidatos y botones de confirmación; no realiza llamadas de red. */
public final class RecognitionResults {
    private final Context context;
    private final ScreenUi ui;

    public RecognitionResults(Context context, ScreenUi ui) {
        this.context = context;
        this.ui = ui;
    }

    public void show(
            LinearLayout page,
            RecognitionResult result,
            CatalogScreen catalogScreen,
            PlantRepository repository,
            String resultId,
            String confirmedPlant,
            Consumer<PlantMatch> onConfirm) {
        if (result == null) return;
        View section = ui.inflate(R.layout.section_results, page);
        ((TextView) section.findViewById(R.id.results_title))
                .setText(context.getString(R.string.results_engine, result.engine()));
        boolean uncertain =
                confirmedPlant.isEmpty()
                        && (result.matches().isEmpty()
                                || result.matches().get(0).score() < AppConstants.ACCEPTANCE_SCORE);
        section.findViewById(R.id.uncertain_message)
                .setVisibility(uncertain ? View.VISIBLE : View.GONE);
        LinearLayout rows = section.findViewById(R.id.match_rows);
        for (PlantMatch match : result.matches()) {
            JSONObject plant = repository.find(match.scientific());
            View item = ui.inflate(R.layout.item_match, rows);
            String title = plant == null ? match.common() : plant.optString("nombre");
            ((TextView) item.findViewById(R.id.match_name))
                    .setText(title.isBlank() ? match.scientific() : title);
            ((TextView) item.findViewById(R.id.match_scientific)).setText(match.scientific());
            PlantCharts.showMatchScore(
                    context, ui, item.findViewById(R.id.score_container), match.score());
            item.findViewById(R.id.match_detail)
                    .setVisibility(plant == null ? View.GONE : View.VISIBLE);
            item.findViewById(R.id.match_detail)
                    .setOnClickListener(v -> catalogScreen.detail(plant));
            item.findViewById(R.id.no_local_detail)
                    .setVisibility(plant == null ? View.VISIBLE : View.GONE);
            Button confirm = item.findViewById(R.id.confirm_match);
            confirm.setVisibility(resultId.isEmpty() ? View.GONE : View.VISIBLE);
            boolean chosen = confirmedPlant.equals(repository.speciesKey(match.scientific()));
            confirm.setText(chosen ? R.string.saved : R.string.confirm_match);
            confirm.setEnabled(!chosen);
            confirm.setOnClickListener(v -> onConfirm.accept(match));
        }
    }
}
