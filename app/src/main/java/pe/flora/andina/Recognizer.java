
package pe.flora.andina;

import android.content.Context;
import android.graphics.Bitmap;
import org.json.*;
import org.tensorflow.lite.Interpreter;
import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import javax.net.ssl.HttpsURLConnection;

final class Recognizer {
    record Match(String scientific, String common, float score) {}
    record Result(List<Match> matches, String engine) {}

    static Result identify(Context context, Bitmap photo, String key) throws Exception {
        if (!key.isBlank()) return online(photo, key);
        return local(context, photo);
    }

    static String read(InputStream in) throws IOException {
        try (in; ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
            byte[] chunk = new byte[8192];
            int count;
            while ((count = in.read(chunk)) != -1) buffer.write(chunk, 0, count);
            return buffer.toString("UTF-8");
        }
    }

    private static Result local(Context context, Bitmap photo) throws Exception {
        byte[] bytes;
        try (InputStream in = context.getAssets().open("plantas.tflite");
             ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
            byte[] chunk = new byte[8192];
            int count;
            while ((count = in.read(chunk)) != -1) buffer.write(chunk, 0, count);
            bytes = buffer.toByteArray();
        } catch (FileNotFoundException e) {
            throw new IOException("Agrega tu clave de Pl@ntNet en Ajustes o instala el modelo local siguiendo el README.");
        }
        List<String> labels = Arrays.asList(read(context.getAssets().open("labels.txt")).trim().split("\\R"));
        ByteBuffer model = ByteBuffer.allocateDirect(bytes.length).order(ByteOrder.nativeOrder());
        model.put(bytes).rewind();
        Bitmap resized = Bitmap.createScaledBitmap(photo, 224, 224, true);
        int[] pixels = new int[224 * 224];
        resized.getPixels(pixels, 0, 224, 0, 0, 224, 224);
        ByteBuffer input = ByteBuffer.allocateDirect(pixels.length * 12).order(ByteOrder.nativeOrder());
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
        List<Match> matches = new ArrayList<>();
        for (int i = 0; i < labels.size(); i++) {
            String label = labels.get(i);
            if (!label.equals("__otras__")) matches.add(new Match(label, "", output[0][i]));
        }
        int other = labels.indexOf("__otras__");
        matches.sort(Comparator.comparingDouble(Match::score).reversed());
        if (other >= 0 && !matches.isEmpty() && output[0][other] >= matches.get(0).score()) matches.clear();
        return new Result(matches.subList(0, Math.min(3, matches.size())), "TensorFlow Lite");
    }

    private static Result online(Bitmap photo, String key) throws Exception {
        String url = "https://my-api.plantnet.org/v2/identify/all?lang=es&api-key="
                + URLEncoder.encode(key, "UTF-8");
        HttpsURLConnection connection = (HttpsURLConnection) new URL(url).openConnection();
        String boundary = UUID.randomUUID().toString();
        connection.setInstanceFollowRedirects(false);
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(30000);
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        try {
            try (OutputStream out = connection.getOutputStream()) {
                out.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"organs\"\r\n\r\nauto\r\n"
                        + "--" + boundary + "\r\nContent-Disposition: form-data; name=\"images\"; filename=\"planta.jpg\"\r\n"
                        + "Content-Type: image/jpeg\r\n\r\n").getBytes(StandardCharsets.UTF_8));
                photo.compress(Bitmap.CompressFormat.JPEG, 88, out);
                out.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
            }
            int status = connection.getResponseCode();
            if (status == 404) return new Result(Collections.emptyList(), "Pl@ntNet");
            if (status == 401 || status == 403) throw new IOException("Revisa tu clave de Pl@ntNet en Ajustes.");
            if (status == 429) throw new IOException("Se alcanzó el límite de consultas de Pl@ntNet.");
            if (status != 200) throw new IOException("Pl@ntNet respondió con código " + status + ".");
            JSONArray rows = new JSONObject(read(connection.getInputStream())).getJSONArray("results");
            List<Match> matches = new ArrayList<>();
            for (int i = 0; i < Math.min(3, rows.length()); i++) {
                JSONObject row = rows.getJSONObject(i);
                JSONObject species = row.getJSONObject("species");
                JSONArray names = species.optJSONArray("commonNames");
                matches.add(new Match(species.getString("scientificNameWithoutAuthor"),
                        names == null ? "" : names.optString(0), (float) row.getDouble("score")));
            }
            return new Result(matches, "Pl@ntNet");
        } finally {
            connection.disconnect();
        }
    }
}
