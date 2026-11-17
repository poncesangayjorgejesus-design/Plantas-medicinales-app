package pe.flora.andina.recognition;

import android.content.Context;
import android.graphics.Bitmap;

import org.json.JSONArray;
import org.json.JSONObject;
import org.tensorflow.lite.Interpreter;

import pe.flora.andina.model.PlantMatch;
import pe.flora.andina.model.RecognitionResult;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import javax.net.ssl.HttpsURLConnection;

/** Motor de identificación: elige Pl@ntNet o TensorFlow Lite y devuelve candidatos. */
public final class Recognizer {
    private static final int MODEL_IMAGE_SIZE = 224;
    private static final int MAX_CANDIDATES = 3;
    private static final int CONNECT_TIMEOUT_MS = 15_000;
    private static final int READ_TIMEOUT_MS = 30_000;
    private static final int JPEG_QUALITY = 88;

    private Recognizer() {}

    public static RecognitionResult identify(Context context, Bitmap photo, String key)
            throws Exception {
        if (!key.isBlank()) return identifyOnline(photo, key);
        return identifyLocal(context, photo);
    }

    public static String read(InputStream in) throws IOException {
        try (in;
                ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
            byte[] chunk = new byte[8192];
            int count;
            while ((count = in.read(chunk)) != -1) buffer.write(chunk, 0, count);
            return buffer.toString("UTF-8");
        }
    }

    private static RecognitionResult identifyLocal(Context context, Bitmap photo) throws Exception {
        byte[] bytes;
        try (InputStream in = context.getAssets().open("plantas.tflite");
                ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
            byte[] chunk = new byte[8192];
            int count;
            while ((count = in.read(chunk)) != -1) buffer.write(chunk, 0, count);
            bytes = buffer.toByteArray();
        } catch (FileNotFoundException e) {
            throw new IOException(
                    "Agrega tu clave de Pl@ntNet en Ajustes o instala el modelo local siguiendo el"
                            + " README.");
        }
        List<String> labels =
                Arrays.asList(read(context.getAssets().open("labels.txt")).trim().split("\\R"));
        ByteBuffer model = ByteBuffer.allocateDirect(bytes.length).order(ByteOrder.nativeOrder());
        model.put(bytes).rewind();
        Bitmap resized = Bitmap.createScaledBitmap(photo, MODEL_IMAGE_SIZE, MODEL_IMAGE_SIZE, true);
        int[] pixels = new int[MODEL_IMAGE_SIZE * MODEL_IMAGE_SIZE];
        resized.getPixels(pixels, 0, MODEL_IMAGE_SIZE, 0, 0, MODEL_IMAGE_SIZE, MODEL_IMAGE_SIZE);
        ByteBuffer input =
                ByteBuffer.allocateDirect(pixels.length * 12).order(ByteOrder.nativeOrder());
        for (int pixel : pixels) {
            input.putFloat((pixel >> 16) & 255);
            input.putFloat((pixel >> 8) & 255);
            input.putFloat(pixel & 255);
        }
        input.rewind();
        float[][] output = new float[1][labels.size()];
        try (Interpreter interpreter = new Interpreter(model)) {
            interpreter.run(input, output);
        }
        List<PlantMatch> matches = new ArrayList<>();
        for (int i = 0; i < labels.size(); i++) {
            String label = labels.get(i);
            if (!label.equals("__otras__")) matches.add(new PlantMatch(label, "", output[0][i]));
        }
        int other = labels.indexOf("__otras__");
        matches.sort(Comparator.comparingDouble(PlantMatch::score).reversed());
        if (other >= 0 && !matches.isEmpty() && output[0][other] >= matches.get(0).score())
            matches.clear();
        return new RecognitionResult(
                matches.subList(0, Math.min(MAX_CANDIDATES, matches.size())), "TensorFlow Lite");
    }

    private static RecognitionResult identifyOnline(Bitmap photo, String key) throws Exception {
        String url =
                "https://my-api.plantnet.org/v2/identify/all?lang=es&api-key="
                        + URLEncoder.encode(key, "UTF-8");
        HttpsURLConnection connection = (HttpsURLConnection) new URL(url).openConnection();
        String boundary = UUID.randomUUID().toString();
        connection.setInstanceFollowRedirects(false);
        connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
        connection.setReadTimeout(READ_TIMEOUT_MS);
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        try {
            try (OutputStream out = connection.getOutputStream()) {
                out.write(
                        ("--"
                                        + boundary
                                        + "\r\n"
                                        + "Content-Disposition: form-data; name=\"organs\"\r\n\r\n"
                                        + "auto\r\n"
                                        + "--"
                                        + boundary
                                        + "\r\n"
                                        + "Content-Disposition: form-data; name=\"images\";"
                                        + " filename=\"planta.jpg\"\r\n"
                                        + "Content-Type: image/jpeg\r\n\r\n")
                                .getBytes(StandardCharsets.UTF_8));
                photo.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out);
                out.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
            }
            int status = connection.getResponseCode();
            if (status == 404) return new RecognitionResult(Collections.emptyList(), "Pl@ntNet");
            if (status == 401 || status == 403)
                throw new IOException("Revisa tu clave de Pl@ntNet en Ajustes.");
            if (status == 429)
                throw new IOException("Se alcanzó el límite de consultas de Pl@ntNet.");
            if (status != 200)
                throw new IOException("Pl@ntNet respondió con código " + status + ".");
            JSONArray rows =
                    new JSONObject(read(connection.getInputStream())).getJSONArray("results");
            List<PlantMatch> matches = new ArrayList<>();
            for (int i = 0; i < Math.min(MAX_CANDIDATES, rows.length()); i++) {
                JSONObject row = rows.getJSONObject(i);
                JSONObject species = row.getJSONObject("species");
                JSONArray names = species.optJSONArray("commonNames");
                matches.add(
                        new PlantMatch(
                                species.getString("scientificNameWithoutAuthor"),
                                names == null ? "" : names.optString(0),
                                (float) row.getDouble("score")));
            }
            return new RecognitionResult(matches, "Pl@ntNet");
        } finally {
            connection.disconnect();
        }
    }
}
