package dev.archangelclient.cosmetics.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * Reads Blockbench "Bedrock Entity" geometry ({@code .geo.json}, format 1.12+, box UV) and bakes it
 * into a vanilla {@link ModelPart} tree, so cosmetics can be modeled in Blockbench and exported as is.
 *
 * <p>Bedrock geometry is Y-up with the feet at y=0; Java entity models are Y-down with the neck at
 * y=0 (24 px above the feet) and X mirrored. Bone pivots and cubes are converted the same way
 * Blockbench's own Java exporter does it. Rotated cubes become a child part with that rotation.
 */
public final class BlockbenchModelLoader {
    private static final float NECK_HEIGHT = 24f;

    private BlockbenchModelLoader() {}

    public static ModelPart load(JsonObject file) {
        JsonObject geometry = firstGeometry(file);
        JsonObject description = geometry.getAsJsonObject("description");
        int texWidth = description != null && description.has("texture_width") ? description.get("texture_width").getAsInt() : 64;
        int texHeight = description != null && description.has("texture_height") ? description.get("texture_height").getAsInt() : 64;

        Map<String, JsonObject> bones = new LinkedHashMap<>();
        JsonArray boneArray = geometry.has("bones") ? geometry.getAsJsonArray("bones") : new JsonArray();
        for (JsonElement e : boneArray) {
            JsonObject bone = e.getAsJsonObject();
            bones.put(bone.get("name").getAsString(), bone);
        }

        MeshDefinition mesh = new MeshDefinition();
        Map<String, PartDefinition> built = new HashMap<>();
        for (String name : bones.keySet()) build(name, bones, built, mesh.getRoot(), 0);
        return LayerDefinition.create(mesh, texWidth, texHeight).bakeRoot();
    }

    private static JsonObject firstGeometry(JsonObject file) {
        if (file.has("minecraft:geometry")) return file.getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
        // legacy 1.8 format: {"geometry.name": {...}}
        for (Map.Entry<String, JsonElement> e : file.entrySet()) {
            if (e.getKey().startsWith("geometry.") && e.getValue().isJsonObject()) return e.getValue().getAsJsonObject();
        }
        throw new IllegalArgumentException("No geometry in file");
    }

    private static PartDefinition build(String name, Map<String, JsonObject> bones, Map<String, PartDefinition> built,
                                        PartDefinition root, int depth) {
        PartDefinition existing = built.get(name);
        if (existing != null) return existing;
        if (depth > 64) throw new IllegalArgumentException("Bone hierarchy too deep or cyclic at " + name);
        JsonObject bone = bones.get(name);
        if (bone == null) throw new IllegalArgumentException("Unknown parent bone " + name);

        String parentName = bone.has("parent") ? bone.get("parent").getAsString() : null;
        PartDefinition parent = parentName != null ? build(parentName, bones, built, root, depth + 1) : root;
        float[] parentPivot = parentName != null ? javaPivot(bones.get(parentName)) : new float[3];
        float[] pivot = javaPivot(bone);
        float[] rot = javaRotation(bone, "rotation");

        CubeListBuilder cubes = CubeListBuilder.create();
        JsonArray cubeArray = bone.has("cubes") ? bone.getAsJsonArray("cubes") : new JsonArray();
        Map<JsonObject, Integer> rotatedCubes = new LinkedHashMap<>();
        for (int i = 0; i < cubeArray.size(); i++) {
            JsonObject cube = cubeArray.get(i).getAsJsonObject();
            if (cube.has("rotation")) rotatedCubes.put(cube, i);
            else addCube(cubes, cube, pivot);
        }

        PartPose pose = PartPose.offsetAndRotation(pivot[0] - parentPivot[0], pivot[1] - parentPivot[1], pivot[2] - parentPivot[2],
                rot[0], rot[1], rot[2]);
        PartDefinition part = parent.addOrReplaceChild(name, cubes, pose);
        rotatedCubes.forEach((cube, index) -> {
            float[] cubePivot = cube.has("pivot") ? toJava(vec(cube, "pivot")) : pivot;
            float[] cubeRot = javaRotation(cube, "rotation");
            CubeListBuilder single = CubeListBuilder.create();
            addCube(single, cube, cubePivot);
            part.addOrReplaceChild(name + "_cube" + index, single, PartPose.offsetAndRotation(
                    cubePivot[0] - pivot[0], cubePivot[1] - pivot[1], cubePivot[2] - pivot[2], cubeRot[0], cubeRot[1], cubeRot[2]));
        });
        built.put(name, part);
        return part;
    }

    /** Adds a box-UV cube, positioned relative to the Java-space pivot of the part that owns it. */
    private static void addCube(CubeListBuilder builder, JsonObject cube, float[] pivot) {
        float[] origin = vec(cube, "origin");
        float[] size = vec(cube, "size");
        float inflate = cube.has("inflate") ? cube.get("inflate").getAsFloat() : 0f;
        int u = 0, v = 0;
        if (cube.has("uv") && cube.get("uv").isJsonArray()) {
            JsonArray uv = cube.getAsJsonArray("uv");
            u = Mth.floor(uv.get(0).getAsFloat());
            v = Mth.floor(uv.get(1).getAsFloat());
        }
        // Bedrock x..x+w maps to Java -(x+w)..-x, and y..y+h to (24-y-h)..(24-y)
        float minX = -(origin[0] + size[0]) - pivot[0];
        float minY = NECK_HEIGHT - (origin[1] + size[1]) - pivot[1];
        float minZ = origin[2] - pivot[2];
        builder.texOffs(u, v).mirror(cube.has("mirror") && cube.get("mirror").getAsBoolean())
                .addBox(minX, minY, minZ, size[0], size[1], size[2], new CubeDeformation(inflate));
    }

    private static float[] javaPivot(JsonObject bone) {
        return bone.has("pivot") ? toJava(vec(bone, "pivot")) : new float[3];
    }

    private static float[] toJava(float[] bedrock) {
        return new float[] {-bedrock[0], NECK_HEIGHT - bedrock[1], bedrock[2]};
    }

    /** Mirroring X and Y is a 180 degree turn about Z, so X and Y rotations flip sign. */
    private static float[] javaRotation(JsonObject obj, String key) {
        if (!obj.has(key)) return new float[3];
        float[] deg = vec(obj, key);
        return new float[] {-deg[0] * Mth.DEG_TO_RAD, -deg[1] * Mth.DEG_TO_RAD, deg[2] * Mth.DEG_TO_RAD};
    }

    private static float[] vec(JsonObject obj, String key) {
        JsonArray a = obj.getAsJsonArray(key);
        return new float[] {a.get(0).getAsFloat(), a.get(1).getAsFloat(), a.get(2).getAsFloat()};
    }
}
