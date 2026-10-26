package dev.archangelclient.emotes;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.archangelclient.ArchangelClient;
import dev.archangelclient.emotes.anim.Animation;
import dev.archangelclient.emotes.anim.AnimationLoader;
import java.io.Reader;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.fabricmc.fabric.api.resource.v1.reloader.SimpleResourceReloader;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jspecify.annotations.Nullable;

/**
 * The emote catalog, read from {@code assets/archangelclient/emotes/catalog.json} on every resource
 * reload. Each entry names a Bedrock animation file in {@code emotes/animations/<file>.animation.json}
 * (see {@link AnimationLoader}) and an icon in {@code textures/emote/<icon>.png}.
 */
public final class EmoteRegistry extends SimpleResourceReloader<Map<String, Emote>> {
    public static final Identifier RELOADER_ID = ArchangelClient.id("emotes");
    private static final Identifier CATALOG = ArchangelClient.id("emotes/catalog.json");

    private volatile Map<String, Emote> emotes = Map.of();

    public @Nullable Emote get(@Nullable String id) {
        return id == null ? null : emotes.get(id);
    }

    /** In catalog order, which is also the emote wheel's order. */
    public List<Emote> all() {
        return List.copyOf(emotes.values());
    }

    @Override
    protected Map<String, Emote> prepare(SharedState store) {
        ResourceManager resources = store.resourceManager();
        Map<String, Emote> loaded = new LinkedHashMap<>();
        Map<String, Map<String, Animation>> files = new HashMap<>();
        // every pack's catalog is merged, later packs override entries with the same id
        for (Resource resource : resources.getResourceStack(CATALOG)) {
            try (Reader reader = resource.openAsReader()) {
                for (JsonElement e : JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("emotes")) {
                    try {
                        Emote emote = parse(e.getAsJsonObject(), resources, files);
                        loaded.put(emote.id(), emote);
                    } catch (RuntimeException ex) {
                        ArchangelClient.LOGGER.warn("Skipping emote {}: {}", e, ex.toString());
                    }
                }
            } catch (Exception ex) {
                ArchangelClient.LOGGER.error("Failed to read emote catalog from {}", resource.sourcePackId(), ex);
            }
        }
        return loaded;
    }

    @Override
    protected void apply(Map<String, Emote> prepared, SharedState store) {
        emotes = Collections.unmodifiableMap(prepared);
        ArchangelClient.LOGGER.info("Loaded {} emotes", prepared.size());
    }

    private static Emote parse(JsonObject json, ResourceManager resources, Map<String, Map<String, Animation>> files) {
        String id = json.get("id").getAsString();
        String file = json.has("file") ? json.get("file").getAsString() : id;
        String animName = json.has("animation") ? json.get("animation").getAsString() : id;
        Map<String, Animation> animations = files.computeIfAbsent(file, f -> readFile(resources, f));
        Animation animation = animations.get(animName);
        if (animation == null) throw new IllegalArgumentException("no animation '" + animName + "' in " + file);
        String icon = json.has("icon") ? json.get("icon").getAsString() : id;
        return new Emote(id,
                json.has("name") ? json.get("name").getAsString() : id,
                json.has("description") ? json.get("description").getAsString() : "",
                ArchangelClient.id("textures/emote/" + icon + ".png"),
                animation);
    }

    private static Map<String, Animation> readFile(ResourceManager resources, String file) {
        Identifier path = ArchangelClient.id("emotes/animations/" + file + ".animation.json");
        Optional<Resource> resource = resources.getResource(path);
        if (resource.isEmpty()) throw new IllegalArgumentException("missing " + path);
        try (Reader reader = resource.get().openAsReader()) {
            return AnimationLoader.load(JsonParser.parseReader(reader).getAsJsonObject());
        } catch (Exception e) {
            throw new IllegalArgumentException("bad animation file " + path + ": " + e, e);
        }
    }
}
