package pe.flora.andina.util;

import java.time.ZoneId;

/** Reglas compartidas. Mantener las claves existentes conserva los datos instalados. */
public final class AppConstants {
    public static final String PREFERENCES_NAME = "flora";
    public static final String API_KEY = "key";
    public static final String HISTORY_KEY = "history";
    public static final int HISTORY_LIMIT = 200;
    public static final float ACCEPTANCE_SCORE = 0.55f;
    public static final ZoneId HISTORY_ZONE = ZoneId.of("America/Lima");

    private AppConstants() {}
}
