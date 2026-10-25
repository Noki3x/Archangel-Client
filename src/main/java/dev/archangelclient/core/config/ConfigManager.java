package dev.archangelclient.core.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.archangelclient.ArchangelClient;
import dev.archangelclient.core.module.Module;
import dev.archangelclient.core.module.ModuleManager;
import dev.archangelclient.core.setting.Setting;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Stream;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Util;

/**
 * Saves and loads module state as JSON profiles in {@code config/archangelclient/<profile>.json}.
 * Changes are saved automatically shortly after they happen; the active profile's name is kept in
 * {@code active-profile.txt}.
 */
public final class ConfigManager {
    public static final String DEFAULT_PROFILE = "default";
    private static final int VERSION = 1;
    private static final long SAVE_DELAY_MS = 750;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final ModuleManager modules;
    private final Path dir;
    private String activeProfile = DEFAULT_PROFILE;
    private long dirtySince = -1;
    private boolean loading;

    public ConfigManager(ModuleManager modules) {
        this.modules = modules;
        this.dir = FabricLoader.getInstance().getConfigDir().resolve(ArchangelClient.MOD_ID);
    }

    /** Loads the last active profile, creating the default one on first launch. */
    public void init() {
        try {
            Files.createDirectories(dir);
            Path active = dir.resolve("active-profile.txt");
            if (Files.exists(active)) activeProfile = sanitizeName(Files.readString(active).trim());
        } catch (IOException e) {
            ArchangelClient.LOGGER.error("Could not read config directory {}", dir, e);
        }
        if (activeProfile.isEmpty() || !Files.exists(profilePath(activeProfile))) activeProfile = DEFAULT_PROFILE;
        load(activeProfile);
    }

    public String getActiveProfile() {
        return activeProfile;
    }

    public Path getDirectory() {
        return dir;
    }

    public List<String> listProfiles() {
        try (Stream<Path> files = Files.list(dir)) {
            List<String> names = files.map(p -> p.getFileName().toString())
                    .filter(n -> n.endsWith(".json"))
                    .map(n -> n.substring(0, n.length() - 5))
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .toList();
            return names.contains(activeProfile) ? names
                    : Stream.concat(Stream.of(activeProfile), names.stream()).toList();
        } catch (IOException e) {
            return List.of(activeProfile);
        }
    }

    // --- auto-save

    public void markDirty() {
        if (loading) return;
        if (dirtySince < 0) dirtySince = Util.getMillis();
    }

    /** Called every client tick; writes pending changes once they've settled. */
    public void tick() {
        if (dirtySince >= 0 && Util.getMillis() - dirtySince >= SAVE_DELAY_MS) save();
    }

    public void save() {
        dirtySince = -1;
        write(profilePath(activeProfile), serialize());
    }

    // --- profiles

    /** Saves the current profile, then switches to {@code name} (created from defaults if new). */
    public void switchTo(String name) {
        String clean = sanitizeName(name);
        if (clean.isEmpty() || clean.equals(activeProfile)) return;
        save();
        load(clean);
    }

    /** Creates a new profile as a copy of the current settings and switches to it. */
    public boolean create(String name) {
        String clean = sanitizeName(name);
        if (clean.isEmpty() || Files.exists(profilePath(clean))) return false;
        save();
        activeProfile = clean;
        save();
        writeActive();
        return true;
    }

    public boolean delete(String name) {
        if (name.equals(activeProfile)) return false;
        try {
            return Files.deleteIfExists(profilePath(name));
        } catch (IOException e) {
            ArchangelClient.LOGGER.error("Could not delete profile {}", name, e);
            return false;
        }
    }

    /** Writes the active profile to {@code exports/} and returns its JSON (for the clipboard). */
    public String exportActive() {
        save();
        String json = GSON.toJson(serialize());
        String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        Path out = dir.resolve("exports").resolve(activeProfile + "-" + stamp + ".json");
        try {
            Files.createDirectories(out.getParent());
            Files.writeString(out, json, StandardCharsets.UTF_8);
        } catch (IOException e) {
            ArchangelClient.LOGGER.error("Could not export profile to {}", out, e);
        }
        return json;
    }

    /**
     * Imports profile JSON (e.g. pasted from the clipboard) as a new profile and switches to it.
     * Returns the new profile's name, or null if the text isn't a valid profile.
     */
    public String importProfile(String json, String baseName) {
        JsonObject root;
        try {
            root = JsonParser.parseString(json).getAsJsonObject();
        } catch (RuntimeException e) {
            return null;
        }
        if (!root.has("modules")) return null;
        String name = uniqueName(sanitizeName(baseName).isEmpty() ? "imported" : sanitizeName(baseName));
        save();
        write(profilePath(name), root);
        load(name);
        return name;
    }

    // --- serialization

    private JsonObject serialize() {
        JsonObject root = new JsonObject();
        root.addProperty("version", VERSION);
        JsonObject mods = new JsonObject();
        for (Module m : modules.all()) {
            JsonObject obj = new JsonObject();
            obj.addProperty("enabled", m.isEnabled());
            obj.add("keybind", m.getKeybind().toJson());
            JsonObject settings = new JsonObject();
            for (Setting<?> s : m.getSettings()) settings.add(s.getName(), s.toJson());
            obj.add("settings", settings);
            mods.add(m.getName(), obj);
        }
        root.add("modules", mods);
        return root;
    }

    private void load(String name) {
        activeProfile = name;
        JsonObject root = read(profilePath(name));
        JsonObject mods = root != null && root.has("modules") ? root.getAsJsonObject("modules") : new JsonObject();
        loading = true;
        try {
            for (Module m : modules.all()) apply(m, mods.has(m.getName()) ? mods.getAsJsonObject(m.getName()) : null);
        } finally {
            loading = false;
        }
        writeActive();
        if (root == null) save();
        ArchangelClient.LOGGER.info("Loaded profile '{}'", name);
    }

    /** Applies a module's saved state, or resets it to defaults when {@code obj} is null. */
    private static void apply(Module m, JsonObject obj) {
        m.getKeybind().reset();
        m.getSettings().forEach(Setting::reset);
        if (obj == null) {
            m.setEnabled(m.isEnabledByDefault());
            return;
        }
        if (obj.has("keybind")) m.getKeybind().fromJson(obj.get("keybind"));
        JsonObject settings = obj.has("settings") ? obj.getAsJsonObject("settings") : new JsonObject();
        for (Setting<?> s : m.getSettings()) {
            JsonElement value = settings.get(s.getName());
            if (value != null) s.fromJson(value);
        }
        m.setEnabled(obj.has("enabled") ? obj.get("enabled").getAsBoolean() : m.isEnabledByDefault());
    }

    private JsonObject read(Path path) {
        if (!Files.exists(path)) return null;
        try {
            return JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IOException | RuntimeException e) {
            ArchangelClient.LOGGER.error("Could not read profile {}, using defaults", path, e);
            return null;
        }
    }

    private void write(Path path, JsonObject json) {
        try {
            Files.createDirectories(path.getParent());
            // write to a temp file first so a crash mid-save can't corrupt the profile
            Path tmp = path.resolveSibling(path.getFileName() + ".tmp");
            Files.writeString(tmp, GSON.toJson(json), StandardCharsets.UTF_8);
            Files.move(tmp, path, java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                    java.nio.file.StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            ArchangelClient.LOGGER.error("Could not save profile {}", path, e);
        }
    }

    private void writeActive() {
        try {
            Files.writeString(dir.resolve("active-profile.txt"), activeProfile, StandardCharsets.UTF_8);
        } catch (IOException e) {
            ArchangelClient.LOGGER.error("Could not save active profile name", e);
        }
    }

    private String uniqueName(String base) {
        String name = base;
        for (int i = 2; Files.exists(profilePath(name)); i++) name = base + "-" + i;
        return name;
    }

    private Path profilePath(String name) {
        return dir.resolve(name + ".json");
    }

    /** Profile names become file names, so keep them to a safe character set. */
    public static String sanitizeName(String name) {
        String clean = name.trim().replaceAll("[^A-Za-z0-9 _-]", "").replaceAll("\\s+", " ");
        return clean.length() > 24 ? clean.substring(0, 24).trim() : clean;
    }
}
