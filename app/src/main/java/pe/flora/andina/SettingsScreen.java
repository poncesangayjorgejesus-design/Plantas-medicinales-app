
package pe.flora.andina;

import android.content.Context;
import android.content.SharedPreferences;
import android.widget.*;
import static pe.flora.andina.ScreenUi.*;

/** Configuración de la clave de reconocimiento. Responsable: integrante 2. */
final class SettingsScreen {
    private final Context context;
    private final ScreenUi ui;
    private final SharedPreferences prefs;

    SettingsScreen(Context context, ScreenUi ui, SharedPreferences prefs) {
        this.context = context;
        this.ui = ui;
        this.prefs = prefs;
    }

    void show(LinearLayout page) {
        ui.label(page, "Ajustes", 30, INK, true);

        LinearLayout engine = ui.card(page, -1);

        ui.label(engine, "Motor de identificación", 21, INK, true);

        ui.label(engine, "Con clave se usa Pl@ntNet. Sin clave, se usa el modelo TensorFlow Lite instalado en la app.", 16, INK, false);

        EditText key = new EditText(context);
        key.setSingleLine(true);
        key.setHint("Clave de Pl@ntNet");

        key.setInputType(
                android.text.InputType.TYPE_CLASS_TEXT |
                        android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        );

        key.setText(prefs.getString("key", ""));
        engine.addView(key);

        ui.action(engine, "Guardar configuración", () -> {
            prefs.edit()
                    .putString("key", key.getText().toString().trim())
                    .apply();

            Toast.makeText(
                    context,
                    "Configuración guardada",
                    Toast.LENGTH_SHORT
            ).show();
        });
    }
}
