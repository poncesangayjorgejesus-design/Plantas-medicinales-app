package pe.flora.andina;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.widget.*;
import android.text.*;
import org.json.*;
import java.time.*;
import java.text.DateFormat;
import java.util.*;
import static pe.flora.andina.ScreenUi.*;

final class HistoryScreen {
    private final Context context;
    private final ScreenUi ui;
    private final HistoryStore store;
    private final PlantCatalog catalog;
    private final HistoryPhotos photos;
    private final Runnable refresh;
    private LocalDate historyDate;
    private String historyQuery = "";
}
