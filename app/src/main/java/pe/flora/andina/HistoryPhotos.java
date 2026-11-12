package pe.flora.andina;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.*;
import android.net.Uri;
import android.view.*;
import android.widget.*;
import org.json.JSONObject;

final class HistoryPhotos {
    private final Context context;
    private final ScreenUi ui;
    private final PhotoStore photos;
    HistoryPhotos(Context context, ScreenUi ui, PhotoStore photos) {
        this.context = context;
        this.ui = ui;
        this.photos = photos;
    }

    void show(LinearLayout entry, JSONObject row) {
        String id = row.optString("id");
        if (id.isEmpty() || !photos.file(id).isFile()) return;
        ImageView thumbnail = new ImageView(context);
        thumbnail.setScaleType(ImageView.ScaleType.CENTER_CROP);
        thumbnail.setContentDescription("Foto guardada de la consulta");
        // Decode thumbnails only when the row is close to the visible viewport.
        ViewTreeObserver.OnScrollChangedListener onScroll = () -> loadThumbnail(thumbnail, id);
        thumbnail.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            public void onViewAttachedToWindow(View view) {
                view.getViewTreeObserver().addOnScrollChangedListener(onScroll);
                view.post(onScroll::onScrollChanged);
            }
            public void onViewDetachedFromWindow(View view) {
                view.getViewTreeObserver().removeOnScrollChangedListener(onScroll);
            }
        });
        entry.addView(thumbnail, new LinearLayout.LayoutParams(-1, ui.dp(140)));
        ui.action(entry, "Ver foto", () -> {
            ImageView full = new ImageView(context);
            full.setAdjustViewBounds(true);
            full.setImageURI(Uri.fromFile(photos.file(id)));
            new AlertDialog.Builder(context).setTitle("Foto de la consulta")
                    .setView(full).setPositiveButton("Cerrar", null).show();
        });
    }

    private void loadThumbnail(ImageView thumbnail, String id) {
        if (thumbnail.getDrawable() != null || !thumbnail.getLocalVisibleRect(new Rect())) return;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 4;
        thumbnail.setImageBitmap(BitmapFactory.decodeFile(photos.file(id).getAbsolutePath(), options));
    }

}
