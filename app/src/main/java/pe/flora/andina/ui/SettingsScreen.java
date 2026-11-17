package pe.flora.andina.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import pe.flora.andina.R;
import pe.flora.andina.util.AppConstants;

/** Conecta el formulario XML con la clave guardada en las preferencias. */
public final class SettingsScreen {
    private final Context context;
    private final ScreenUi ui;
    private final SharedPreferences prefs;

    public SettingsScreen(Context context, ScreenUi ui, SharedPreferences prefs) {
        this.context = context;
        this.ui = ui;
        this.prefs = prefs;
    }

    public void show(LinearLayout page) {
        View screen = ui.inflate(R.layout.screen_settings, page);
        EditText key = screen.findViewById(R.id.api_key);
        key.setText(prefs.getString(AppConstants.API_KEY, ""));
        screen.findViewById(R.id.save_settings)
                .setOnClickListener(
                        v -> {
                            prefs.edit()
                                    .putString(
                                            AppConstants.API_KEY, key.getText().toString().trim())
                                    .apply();
                            Toast.makeText(
                                            context,
                                            R.string.configuration_saved,
                                            Toast.LENGTH_SHORT)
                                    .show();
                        });
    }
}
