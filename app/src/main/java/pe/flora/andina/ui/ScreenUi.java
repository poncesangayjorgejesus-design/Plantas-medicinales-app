package pe.flora.andina.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import pe.flora.andina.R;

/** Carga layouts XML; no contiene reglas de negocio ni persistencia. */
public final class ScreenUi {
    private final Context context;

    public ScreenUi(Context context) {
        this.context = context;
    }

    public View inflate(int resource, ViewGroup parent) {
        View view = LayoutInflater.from(context).inflate(resource, parent, false);
        parent.addView(view);
        return view;
    }

    public View dialog(int resource) {
        return LayoutInflater.from(context).inflate(resource, null, false);
    }

    public void message(ViewGroup parent, String value) {
        TextView view = (TextView) inflate(R.layout.item_message, parent);
        view.setText(value);
    }
}
