package dev.archangelclient.cosmetics;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.archangelclient.ArchangelClient;
import dev.archangelclient.cosmetics.model.BlockbenchModelLoader;
import dev.archangelclient.cosmetics.model.CosmeticModel;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import net.fabricmc.fabric.api.resource.v1.reloader.SimpleResourceReloader;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jspecify.annotations.Nullable;

/**
 * The cosmetic catalog, read from {@code assets/archangelclient/cosmetics/catalog.json} on every
 * resource reload (so resource packs can retexture or add cosmetics). Models referenced by the
 * catalog are Blockbench files in {@code cosmetics/models/<name>.geo.json}.
 */
public final class CosmeticRegistry extends SimpleResourceReloader<Map<String, Cosmetic>> {
    public static final Identifier RELOADER_ID = ArchangelClient.id("cosmetics");
    private static final Identifier CATALOG = ArchangelClient.id("cosmetics/catalog.json");

    private volatile Map<String, Cosmetic> cosmetics = Map.of();

    public @Nullable Cosmetic get(@Nullable String id) {
        return id == null ? null : cosmetics.get(id);
    }

    public List<Cosmetic> all() {
        return List.copyOf(cosmetics.values());
    }

    public List<Cosmetic> byType(CosmeticType type) {
        List<Cosmetic> list = new ArrayList<>();
        for (Cosmetic c : cosmetics.values()) if (c.type() == type) list.add(c);
        return list;
    }

    @Override
    protected Map<String, Cosmetic> prepare(SharedState store) {
        ResourceManager resources = store.resourceManager();
        Map<String, Cosmetic> loaded = new LinkedHashMap<>();
        // every pack's catalog is merged, later packs override entries with the same id
        for (Resource resource : resources.getResourceStack(CATALOG)) {
            try (Reader reader = resource.openAsReader()) {
                JsonArray entries = JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("cosmetics");
                for (JsonElement e : entries) {
                    try {
                        Cosmetic c = parse(e.getAsJsonObject(), resources);
                        loaded.put(c.id(), c);
                    } catch (RuntimeException ex) {
                        ArchangelClient.LOGGER.warn("Skipping cosmetic {}: {}", e, ex.toString());
                    }
                }
            } catch (Exception ex) {
                ArchangelClient.LOGGER.error("Failed to read cosmetic catalog from {}", resource.sourcePackId(), ex);
            }
        }
        return loaded;
    }

    @Override
    protected void apply(Map<String, Cosmetic> prepared, SharedState store) {
        cosmetics = Collections.unmodifiableMap(prepared);
        ArchangelClient.LOGGER.info("Loaded {} cosmetics", prepared.size());
    }

    private static Cosmetic parse(JsonObject json, ResourceManager resources) {
        String id = json.get("id").getAsString();
        CosmeticType type = CosmeticType.byKey(json.get("type").getAsString());
        if (type == null) throw new IllegalArgumentException("unknown type " + json.get("type"));

        List<Identifier> textures = new ArrayList<>();
        List<Identifier> glow = new ArrayList<>();
        if (json.has("texture")) {
            String base = json.get("texture").getAsString();
            int frames = json.has("frames") ? Math.max(1, json.get("frames").getAsInt()) : 1;
            boolean hasGlow = json.has("glow") && json.get("glow").getAsBoolean();
            for (int i = 0; i < frames; i++) {
                String path = "textures/cosmetic/" + base + (frames > 1 ? "_" + i : "");
                textures.add(ArchangelClient.id(path + ".png"));
                if (hasGlow) glow.add(ArchangelClient.id(path + "_glow.png"));
            }
        }

        CosmeticModel model = null;
        if (json.has("model")) {
            Identifier file = ArchangelClient.id("cosmetics/models/" + json.get("model").getAsString() + ".geo.json");
            Optional<Resource> resource = resources.getResource(file);
            if (resource.isEmpty()) throw new IllegalArgumentException("missing model " + file);
            try (Reader reader = resource.get().openAsReader()) {
                CosmeticModel.Attach attach = json.has("attach")
                        ? CosmeticModel.Attach.valueOf(json.get("attach").getAsString().toUpperCase(Locale.ROOT))
                        : CosmeticModel.Attach.BODY;
                model = new CosmeticModel(BlockbenchModelLoader.load(JsonParser.parseReader(reader).getAsJsonObject()),
                        attach, animation(json.getAsJsonObject("anim")));
            } catch (java.io.IOException ex) {
                throw new IllegalArgumentException("unreadable model " + file, ex);
            }
        }

        int primary = 0xFF3B82F6, secondary = 0xFF8B5CF6;
        if (json.has("colors")) {
            JsonArray colors = json.getAsJsonArray("colors");
            primary = parseColor(colors.get(0).getAsString());
            secondary = colors.size() > 1 ? parseColor(colors.get(1).getAsString()) : primary;
        }
        return new Cosmetic(id,
                json.has("name") ? json.get("name").getAsString() : id,
                json.has("description") ? json.get("description").getAsString() : "",
                type,
                Rarity.byKey(json.has("rarity") ? json.get("rarity").getAsString() : "common"),
                List.copyOf(textures), List.copyOf(glow),
                json.has("frameTicks") ? json.get("frameTicks").getAsInt() : 2,
                model, json.has("emissive") && json.get("emissive").getAsBoolean(), primary, secondary);
    }

    private static CosmeticModel.Animation animation(@Nullable JsonObject anim) {
        if (anim == null) return CosmeticModel.Animation.NONE;
        return new CosmeticModel.Animation(f(anim, "flapSpeed"), f(anim, "flapAngle"), f(anim, "spin"), f(anim, "bob"));
    }

    private static float f(JsonObject obj, String key) {
        return obj.has(key) ? obj.get(key).getAsFloat() : 0f;
    }

    private static int parseColor(String hex) {
        String s = hex.startsWith("#") ? hex.substring(1) : hex;
        long v = Long.parseLong(s, 16);
        return s.length() <= 6 ? (int) (0xFF000000L | v) : (int) v;
    }
}
