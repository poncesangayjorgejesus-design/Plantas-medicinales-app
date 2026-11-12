package pe.flora.andina;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.widget.*;


final class ScreenUi {
    static final int INK = 0xff183629, GREEN = 0xff246044, PAPER = 0xffF2F5EA;
    private final Context context;

    ScreenUi(Context context) { this.context = context; }

    int dp(int value) { return Math.round(value * context.getResources().getDisplayMetrics().density); }

    LinearLayout column() {
        LinearLayout box = new LinearLayout(context);
        box.setOrientation(LinearLayout.VERTICAL);
        return box;
    }

    TextView label(LinearLayout parent, String value, int size, int color, boolean bold) {
        TextView view = new TextView(context);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        if (bold) view.setTypeface(null, Typeface.BOLD);
        view.setPadding(0, dp(4), 0, dp(7));
        parent.addView(view);
        return view;
    }

    GradientDrawable surface(int color, int radius) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(color);
        shape.setCornerRadius(dp(radius));
        return shape;
    }

    LinearLayout card(LinearLayout parent, int color) {
        LinearLayout box = column();
        box.setBackground(surface(color, 20));
        box.setPadding(dp(18), dp(14), dp(18), dp(14));
        LinearLayout.LayoutParams space = new LinearLayout.LayoutParams(-1, -2);
        space.bottomMargin = dp(12);
        parent.addView(box, space);
        return box;
    }

    Button action(LinearLayout parent, String value, Runnable callback) {
        Button button = new Button(context);
        button.setText(value);
        button.setAllCaps(false);
        button.setOnClickListener(view -> callback.run());
        parent.addView(button, new LinearLayout.LayoutParams(-1, -2));
        return button;
    }

}
