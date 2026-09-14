package ro.aquanano.pulselab;

import android.content.Context;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Persistent downloaded vectors and their optional audio loops. */
public final class PresetStore {
    private static final long MAX_VECTOR_BYTES = 1_000_000;
    private static final long MAX_AUDIO_BYTES = 250_000_000;

    public static final class LocalPreset {
        public final String id;
        public final String name;
        public final String description;
        public final File vectorFile;
        public final File audioFile;

        LocalPreset(String id, String name, String description, File vectorFile, File audioFile) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.vectorFile = vectorFile;
            this.audioFile = audioFile;
        }
    }

    private PresetStore() { }

    public static List<LocalPreset> list(Context context) {
        File[] dirs = root(context).listFiles(File::isDirectory);
        if (dirs == null) return Collections.emptyList();
        List<LocalPreset> result = new ArrayList<>();
        for (File dir : dirs) {
            try {
                JSONObject metadata = new JSONObject(readText(new File(dir, "metadata.json"), 1_000_000));
                File vector = new File(dir, "vector.csv");
                String audioName = metadata.optString("audio_file", "");
                File audio = audioName.isEmpty() ? null : new File(dir, audioName);
                if (!vector.isFile() || (audio != null && !audio.isFile())) continue;
                result.add(new LocalPreset(metadata.getString("id"), metadata.getString("name"),
                    metadata.optString("description", ""), vector, audio));
            } catch (Exception ignored) { }
        }
        result.sort(Comparator.comparing(p -> p.name.toLowerCase(Locale.ROOT)));
        return result;
    }

    public static LocalPreset find(Context context, String id) {
        if (id == null) return null;
        for (LocalPreset preset : list(context)) if (id.equals(preset.id)) return preset;
        return null;
    }

    public static LocalPreset download(Context context, JSONObject item, URL catalogUrl) throws Exception {
        String id = item.getString("id");
        if (!id.matches("[A-Za-z0-9._-]{1,80}")) throw new Exception("ID preset invalid");
        String name = item.getString("name");
        String description = item.optString("description", "");
        URL vectorUrl = checkedUrl(catalogUrl, item.getString("vector"));
        String audioPath = item.optString("audio", "");
        URL audioUrl = audioPath.isEmpty() ? null : checkedUrl(catalogUrl, audioPath);

        File directory = new File(root(context), id);
        if (!directory.exists() && !directory.mkdirs()) throw new Exception("Nu pot crea directorul local");
        File vector = new File(directory, "vector.csv");
        downloadFile(vectorUrl, vector, MAX_VECTOR_BYTES, item.optString("vector_sha256", ""));

        File audio = null;
        String audioName = "";
        if (audioUrl != null) {
            String remoteName = new File(audioUrl.getPath()).getName();
            String extension = remoteName.contains(".") ? remoteName.substring(remoteName.lastIndexOf('.')) : ".audio";
            if (!extension.matches("\\.[A-Za-z0-9]{1,8}")) extension = ".audio";
            audioName = "audio" + extension.toLowerCase(Locale.ROOT);
            audio = new File(directory, audioName);
            downloadFile(audioUrl, audio, MAX_AUDIO_BYTES, item.optString("audio_sha256", ""));
        }

        JSONObject metadata = new JSONObject();
        metadata.put("id", id);
        metadata.put("name", name);
        metadata.put("description", description);
        metadata.put("audio_file", audioName);
        writeText(new File(directory, "metadata.json"), metadata.toString(2));
        return new LocalPreset(id, name, description, vector, audio);
    }

    public static String fetchCatalog(URL url) throws Exception {
        HttpURLConnection connection = open(url);
        int length = connection.getContentLength();
        if (length > MAX_VECTOR_BYTES) throw new Exception("Catalog prea mare");
        try (InputStream input = connection.getInputStream(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int read;
            long total = 0;
            while ((read = input.read(buffer)) >= 0) {
                total += read;
                if (total > MAX_VECTOR_BYTES) throw new Exception("Catalog prea mare");
                output.write(buffer, 0, read);
            }
            return output.toString(StandardCharsets.UTF_8.name());
        } finally { connection.disconnect(); }
    }

    private static void downloadFile(URL url, File destination, long maximum, String expectedHash) throws Exception {
        File part = new File(destination.getParentFile(), destination.getName() + ".part");
        HttpURLConnection connection = open(url);
        int length = connection.getContentLength();
        if (length > maximum) throw new Exception("Fișier prea mare");
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream input = connection.getInputStream(); FileOutputStream output = new FileOutputStream(part)) {
            byte[] buffer = new byte[32 * 1024];
            int read;
            long total = 0;
            while ((read = input.read(buffer)) >= 0) {
                total += read;
                if (total > maximum) throw new Exception("Fișier prea mare");
                digest.update(buffer, 0, read);
                output.write(buffer, 0, read);
            }
        } catch (Exception e) {
            part.delete();
            throw e;
        } finally { connection.disconnect(); }
        String actual = hex(digest.digest());
        if (!expectedHash.isEmpty() && !actual.equalsIgnoreCase(expectedHash)) {
            part.delete();
            throw new Exception("Checksum incorect");
        }
        if (destination.exists() && !destination.delete()) throw new Exception("Nu pot actualiza fișierul local");
        if (!part.renameTo(destination)) throw new Exception("Nu pot finaliza descărcarea");
    }

    private static HttpURLConnection open(URL url) throws Exception {
        if (!"https".equalsIgnoreCase(url.getProtocol()) || !"aquanano.eu".equalsIgnoreCase(url.getHost()))
            throw new Exception("Adresă de descărcare neacceptată");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(12_000);
        connection.setReadTimeout(45_000);
        connection.setInstanceFollowRedirects(false);
        connection.setRequestProperty("User-Agent", "AquaRitm-Android");
        int status = connection.getResponseCode();
        if (status != HttpURLConnection.HTTP_OK) {
            connection.disconnect();
            throw new Exception("Server HTTP " + status);
        }
        return connection;
    }

    private static URL checkedUrl(URL base, String path) throws Exception {
        URL result = new URL(base, path);
        if (!"https".equalsIgnoreCase(result.getProtocol()) || !"aquanano.eu".equalsIgnoreCase(result.getHost()))
            throw new Exception("Adresă externă neacceptată");
        return result;
    }

    private static File root(Context context) {
        File external = context.getExternalFilesDir("presets");
        File root = external != null ? external : new File(context.getFilesDir(), "presets");
        if (!root.exists()) root.mkdirs();
        return root;
    }

    private static String readText(File file, long maximum) throws Exception {
        if (file.length() > maximum) throw new Exception("Fișier prea mare");
        try (FileInputStream input = new FileInputStream(file); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) >= 0) output.write(buffer, 0, read);
            return output.toString(StandardCharsets.UTF_8.name());
        }
    }

    private static void writeText(File file, String value) throws Exception {
        try (FileOutputStream output = new FileOutputStream(file)) {
            output.write(value.getBytes(StandardCharsets.UTF_8));
        }
    }

    private static String hex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte value : bytes) result.append(String.format(Locale.US, "%02x", value & 0xff));
        return result.toString();
    }
}
