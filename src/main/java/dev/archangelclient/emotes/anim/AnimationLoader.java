package dev.archangelclient.emotes.anim;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * Reads Bedrock animation files, as exported by Blockbench and used by GeckoLib:
 *
 * <pre>{@code
 * {"format_version": "1.8.0", "animations": {"animation.archangel.wave": {
 *   "loop": false, "animation_length": 2.0,
 *   "bones": {"right_arm": {"rotation": {"0.0": [0, 0, 0], "0.4": {"vector": [0, 0, 150], "easing": "easeOutBack"}}}}}}}
 * }</pre>
 *
 * <p>A channel is a constant ({@code [x, y, z]} or a number) or a map of time to keyframe. A
 * keyframe is a vector, a Bedrock {@code {"pre", "post", "lerp_mode"}} object or a GeckoLib
 * {@code {"vector", "easing"}} object. Molang expressions are not evaluated (they count as 0).
 *
 * <p>Values are applied to the vanilla Java player rig: rotations in degrees on the model part's
 * axes (negative x swings an arm forward, positive z lifts the right arm sideways), positions in
 * pixels with +y up.
 */
public final class AnimationLoader {
    private AnimationLoader() {}

    /** All animations in a file, keyed by name without the {@code animation.} prefix. */
    public static Map<String, Animation> load(JsonObject file) {
        Map<String, Animation> out = new LinkedHashMap<>();
        JsonObject animations = file.getAsJsonObject("animations");
        for (var entry : animations.entrySet()) {
            String name = entry.getKey();
            int dot = name.lastIndexOf('.');
            out.put(dot >= 0 ? name.substring(dot + 1) : name, parse(entry.getValue().getAsJsonObject()));
        }
        return out;
    }

    public static Animation parse(JsonObject json) {
        LoopMode loop = LoopMode.ONCE;
        JsonElement l = json.get("loop");
        if (l != null && l.isJsonPrimitive()) {
            JsonPrimitive p = l.getAsJsonPrimitive();
            if (p.isBoolean()) loop = p.getAsBoolean() ? LoopMode.LOOP : LoopMode.ONCE;
            else if ("hold_on_last_frame".equals(p.getAsString())) loop = LoopMode.HOLD;
        }

        Map<Bone, Animation.Track> tracks = new EnumMap<>(Bone.class);
        float end = 0;
        JsonObject bones = json.has("bones") ? json.getAsJsonObject("bones") : new JsonObject();
        for (var entry : bones.entrySet()) {
            Bone bone = Bone.byName(entry.getKey());
            if (bone == null) continue;
            JsonObject b = entry.getValue().getAsJsonObject();
            Channel rotation = channel(b.get("rotation"));
            Channel position = channel(b.get("position"));
            for (Channel c : new Channel[] {rotation, position}) {
                if (c != null) end = Math.max(end, c.keys().get(c.keys().size() - 1).time());
            }
            tracks.put(bone, new Animation.Track(rotation, position));
        }
        float length = json.has("animation_length") ? json.get("animation_length").getAsFloat() : end;
        return new Animation(length, loop, tracks);
    }

    private static @Nullable Channel channel(@Nullable JsonElement json) {
        if (json == null || json.isJsonNull()) return null;
        List<Keyframe> keys = new ArrayList<>();
        if (json.isJsonObject() && !isKeyObject(json.getAsJsonObject())) {
            for (var entry : json.getAsJsonObject().entrySet()) {
                float time = Float.parseFloat(entry.getKey());
                addKey(keys, time, entry.getValue());
            }
            keys.sort((a, b) -> Float.compare(a.time(), b.time()));
        } else {
            addKey(keys, 0, json);
        }
        return keys.isEmpty() ? null : new Channel(keys);
    }

    /** True for {@code {"vector": ...}} / {@code {"post": ...}} objects, as opposed to time maps. */
    private static boolean isKeyObject(JsonObject o) {
        return o.has("vector") || o.has("post") || o.has("pre");
    }

    private static void addKey(List<Keyframe> keys, float time, JsonElement value) {
        if (value.isJsonObject()) {
            JsonObject o = value.getAsJsonObject();
            boolean smooth = o.has("lerp_mode") && "catmullrom".equals(o.get("lerp_mode").getAsString());
            EmoteEasing easing = o.has("easing") ? EmoteEasing.byName(o.get("easing").getAsString()) : EmoteEasing.LINEAR;
            if (o.has("lerp_mode") && "step".equals(o.get("lerp_mode").getAsString())) easing = EmoteEasing.STEP;
            if (o.has("vector")) {
                keys.add(key(time, o.get("vector"), easing, smooth));
            } else {
                // a "pre" value different from "post" makes a jump at this keyframe
                if (o.has("pre")) keys.add(key(time, o.get("pre"), easing, smooth));
                if (o.has("post")) keys.add(key(time, o.get("post"), EmoteEasing.LINEAR, smooth));
            }
        } else {
            keys.add(key(time, value, EmoteEasing.LINEAR, false));
        }
    }

    private static Keyframe key(float time, JsonElement value, EmoteEasing easing, boolean smooth) {
        float x, y, z;
        if (value.isJsonArray()) {
            JsonArray a = value.getAsJsonArray();
            x = number(a.get(0));
            y = number(a.get(1));
            z = number(a.get(2));
        } else {
            x = y = z = number(value);
        }
        return new Keyframe(time, x, y, z, easing, smooth);
    }

    private static float number(JsonElement e) {
        JsonPrimitive p = e.getAsJsonPrimitive();
        if (p.isNumber()) return p.getAsFloat();
        try {
            return Float.parseFloat(p.getAsString().trim());
        } catch (NumberFormatException molang) {
            return 0;
        }
    }
}
