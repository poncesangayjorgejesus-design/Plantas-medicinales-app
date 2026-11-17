package pe.flora.andina.data;

import android.content.Context;
import android.graphics.Bitmap;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/** Administra los JPEG privados asociados a una consulta; no guarda fotografías en JSON. */
public final class PhotoStore {

    private final Context context;

    public PhotoStore(Context context) {
        this.context = context;
    }

    public void delete(String id) {
        if (!id.isEmpty()) file(id).delete();
    }

    public void delete(JSONObject row) {
        if (row != null) delete(row.optString("id"));
    }

    public File file(String id) {
        return new File(context.getFilesDir(), "consulta-" + id + ".jpg");
    }

    public void save(String id, Bitmap image) throws IOException {
        File file = file(id);

        try (FileOutputStream out = new FileOutputStream(file)) {
            if (!image.compress(Bitmap.CompressFormat.JPEG, 85, out)) {
                throw new IOException("No se pudo comprimir la imagen");
            }
        } catch (IOException e) {
            file.delete();
            throw e;
        }
    }
}
