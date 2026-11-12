package pe.flora.andina;

import android.content.Context;
import android.graphics.Bitmap;
import java.io.*;
import org.json.JSONObject;

/** Archivos privados de fotografías. Integrante 2. */
final class PhotoStore {

    private final Context context;

    PhotoStore(Context context) {
        this.context = context;
    }

    void delete(String id) {
        if (!id.isEmpty()) file(id).delete();
    }

    void delete(JSONObject row) {
        if (row != null) delete(row.optString("id"));
    }

    File file(String id) {
        return new File(
                context.getFilesDir(),
                "consulta-" + id + ".jpg"
        );
    }

    void save(String id, Bitmap image) throws IOException {
        File file = file(id);

        try (FileOutputStream out = new FileOutputStream(file)) {
            if (!image.compress(
                    Bitmap.CompressFormat.JPEG, 85, out)) {
                throw new IOException(
                        "No se pudo comprimir la imagen"
                );
            }
        } catch (IOException e) {
            file.delete();
            throw e;
        }
    }
}