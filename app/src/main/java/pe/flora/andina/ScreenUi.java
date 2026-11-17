package pe.flora.andina;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.widget.*;
import android.content.res.ColorStateList;
import android.graphics.drawable.RippleDrawable;
import android.view.Gravity;

final class ScreenUi {

    static final int FOREST = 0xff174D38;
    static final int GREEN = 0xff287653;
    static final int INK = 0xff24352D;
    static final int PAPER = 0xffF7F8F0;
    static final int SAGE = 0xffDCE9DA;
    static final int WHITE = 0xffffffff;
    static final int MUTED = 0xff718078;
    static final int BORDER = 0xffE1E8DF;
    static final int DANGER = 0xffB94B43;

    private final Context context;

    ScreenUi(Context context) {
        this.context = context;
    }

    int dp(int value) {
        return Math.round(
                value * context.getResources()
                        .getDisplayMetrics().density
        );
    }

    LinearLayout column() {
        LinearLayout box = new LinearLayout(context);
        box.setOrientation(LinearLayout.VERTICAL);
        return box;
    }

    TextView label(
            LinearLayout parent,
            String value,
            int size,
            int color,
            boolean bold
    ) {
        TextView view = new TextView(context);

        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);

        if (bold) {
            view.setTypeface(
                    Typeface.create("sans-serif", Typeface.BOLD)
            );
        } else {
            view.setTypeface(
                    Typeface.create("sans-serif", Typeface.NORMAL)
            );
        }

        view.setLineSpacing(dp(2), 1.05f);
        view.setPadding(0, dp(5), 0, dp(9));

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

        GradientDrawable background = surface(color, 22);

        if (color != GREEN) {
            background.setStroke(dp(1), 0xffDFE8DF);
        }

        box.setBackground(background);
        box.setPadding(dp(18), dp(18), dp(18), dp(18));
        box.setElevation(dp(2));

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(-1, -2);

        params.bottomMargin = dp(14);

        parent.addView(box, params);

        return box;
    }

    Button action(LinearLayout parent, String value, Runnable callback) {

        Button button = new Button(context);

        button.setText(value);
        button.setAllCaps(false);
        button.setTextSize(15);
        button.setTypeface(
                Typeface.create("sans-serif-medium", Typeface.NORMAL)
        );

        boolean danger = value.toLowerCase(
                java.util.Locale.ROOT
        ).contains("eliminar") ||
                value.toLowerCase(
                        java.util.Locale.ROOT
                ).contains("borrar");

        boolean secondary = value.toLowerCase(
                java.util.Locale.ROOT
        ).contains("filtrar") ||
                value.toLowerCase(
                        java.util.Locale.ROOT
                ).contains("ver todas");

        int backgroundColor;
        int textColor;

        if (danger) {
            backgroundColor = 0xffA94B42;
            textColor = 0xffffffff;
        } else if (secondary) {
            backgroundColor = 0xffE5EFE5;
            textColor = GREEN;
        } else {
            backgroundColor = GREEN;
            textColor = 0xffffffff;
        }

        GradientDrawable shape = surface(backgroundColor, 16);

        button.setBackground(
                new RippleDrawable(
                        ColorStateList.valueOf(0x22000000),
                        shape,
                        null
                )
        );

        button.setTextColor(textColor);
        button.setGravity(Gravity.CENTER);
        button.setPadding(dp(18), dp(12), dp(18), dp(12));
        button.setMinHeight(dp(52));
        button.setElevation(dp(1));

        button.setOnClickListener(view -> callback.run());

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(-1, -2);

        params.topMargin = dp(4);
        params.bottomMargin = dp(9);

        parent.addView(button, params);

        return button;
    }

    EditText input(LinearLayout parent, String hint) {

        EditText field = new EditText(context);

        field.setSingleLine(true);
        field.setTextSize(16);
        field.setTextColor(INK);
        field.setHintTextColor(0xff89968A);
        field.setHint(hint);

        field.setPadding(
                dp(15),
                dp(12),
                dp(15),
                dp(12)
        );

        GradientDrawable background = surface(
                0xffffffff,
                14
        );

        background.setStroke(
                dp(1),
                0xffD8E3D8
        );

        field.setBackground(background);
        field.setMinHeight(dp(54));

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(-1, -2);

        params.bottomMargin = dp(12);

        parent.addView(field, params);

        return field;
    }

    void input(EditText field) {

        field.setTextColor(INK);
        field.setHintTextColor(MUTED);
        field.setTextSize(15);

        field.setPadding(
                dp(15),
                dp(13),
                dp(15),
                dp(13)
        );

        GradientDrawable background = surface(WHITE, 13);

        background.setStroke(dp(1), BORDER);

        field.setBackground(background);
    }
}
