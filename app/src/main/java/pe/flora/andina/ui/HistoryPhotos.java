package pe.flora.andina.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.net.Uri;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.LinearLayout;

import org.json.JSONObject;

import pe.flora.andina.R;
import pe.flora.andina.data.PhotoStore;

/** Carga miniaturas visibles y abre el visor XML de una fotografía. */
public final class HistoryPhotos {
    private final Context context;
    private final ScreenUi ui;
    private final PhotoStore photos;

    public HistoryPhotos(Context context, ScreenUi ui, PhotoStore photos) {
        this.context = context;
        this.ui = ui;
        this.photos = photos;
    }

    public void show(LinearLayout entry, JSONObject row) {
        String id = row.optString("id");
        if (id.isEmpty() || !photos.file(id).isFile()) return;
        View item = ui.inflate(R.layout.item_history_photo, entry);
        ImageView thumbnail = item.findViewById(R.id.history_thumbnail);
        // Decode thumbnails only when the row is close to the visible viewport.
        ViewTreeObserver.OnScrollChangedListener onScroll = () -> loadThumbnail(thumbnail, id);
        thumbnail.addOnAttachStateChangeListener(
                new View.OnAttachStateChangeListener() {
                    public void onViewAttachedToWindow(View view) {
                        view.getViewTreeObserver().addOnScrollChangedListener(onScroll);
                        view.post(onScroll::onScrollChanged);
                    }

                    public void onViewDetachedFromWindow(View view) {
                        view.getViewTreeObserver().removeOnScrollChangedListener(onScroll);
                    }
                });
        item.findViewById(R.id.view_photo)
                .setOnClickListener(
                        v -> {
                            ImageView full = (ImageView) ui.dialog(R.layout.dialog_photo);
                            full.setImageURI(Uri.fromFile(photos.file(id)));
                            new AlertDialog.Builder(context)
                                    .setTitle(R.string.photo_title)
                                    .setView(full)
                                    .setPositiveButton(R.string.close, null)
                                    .show();
                        });
    }

    private void loadThumbnail(ImageView thumbnail, String id) {
        if (thumbnail.getDrawable() != null || !thumbnail.getLocalVisibleRect(new Rect())) return;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 4;
        thumbnail.setImageBitmap(
                BitmapFactory.decodeFile(photos.file(id).getAbsolutePath(), options));
    }
}
