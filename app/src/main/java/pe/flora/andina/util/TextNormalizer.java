package pe.flora.andina.util;

import java.text.Normalizer;
import java.util.Locale;

/** Normaliza búsquedas; no modifica los nombres que se muestran o guardan. */
public final class TextNormalizer {
    private TextNormalizer() {}

    public static String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .trim();
    }
}
