package dev.archangelclient.shader;

import dev.archangelclient.ArchangelClient;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import net.fabricmc.loader.api.FabricLoader;
import org.jspecify.annotations.Nullable;

/**
 * Talks to Iris, when it's installed, to list and switch shader packs. Iris is optional and not a
 * compile dependency, so everything goes through reflection and fails soft: if Iris changes its
 * internals, the Shader Packs page says so instead of crashing.
 */
public final class IrisBridge {
    private static final String IRIS = "net.irisshaders.iris.Iris";
    private static final String API = "net.irisshaders.iris.api.v0.IrisApi";

    /** Resolved once: {@link #isShaderPackInUse} is asked every frame. */
    private static @Nullable Object api;
    private static @Nullable Method inUse;
    private static boolean apiFailed;

    private IrisBridge() {}

    public static boolean isInstalled() {
        return FabricLoader.getInstance().isModLoaded("iris");
    }

    /** True while an Iris shader pack is rendering; our own post effects and sky step aside then. */
    public static boolean isShaderPackInUse() {
        if (apiFailed || !isInstalled()) return false;
        try {
            if (inUse == null) {
                api = Class.forName(API).getMethod("getInstance").invoke(null);
                inUse = Class.forName(API).getMethod("isShaderPackInUse");
            }
            return (boolean) inUse.invoke(api);
        } catch (ReflectiveOperationException | LinkageError e) {
            apiFailed = true;
            ArchangelClient.LOGGER.warn("Couldn't query Iris; assuming no shader pack", e);
            return false;
        }
    }

    /** Packs in the shaderpacks folder (zips and folders Iris accepts), sorted by name. */
    public static List<String> listPacks() throws ReflectiveOperationException, IOException {
        Path dir = shaderpacksDirectory();
        Method valid = Class.forName(IRIS).getMethod("isValidShaderpack", Path.class);
        List<String> packs = new ArrayList<>();
        if (!Files.isDirectory(dir)) return packs;
        try (Stream<Path> entries = Files.list(dir)) {
            for (Path p : entries.sorted().toList()) {
                if ((boolean) valid.invoke(null, p)) packs.add(p.getFileName().toString());
            }
        }
        return packs;
    }

    public static Path shaderpacksDirectory() throws ReflectiveOperationException {
        return (Path) Class.forName(IRIS).getMethod("getShaderpacksDirectory").invoke(null);
    }

    /** The selected pack's file name, or null when shaders are off. */
    public static @Nullable String activePack() throws ReflectiveOperationException {
        Object config = config();
        if (!(boolean) config.getClass().getMethod("areShadersEnabled").invoke(config)) return null;
        Optional<?> name = (Optional<?>) config.getClass().getMethod("getShaderPackName").invoke(config);
        return name.map(Object::toString).orElse(null);
    }

    /** Switches to {@code pack}, or turns shaders off when it's null, then reloads Iris. */
    public static void select(@Nullable String pack) throws ReflectiveOperationException {
        Object config = config();
        Class<?> type = config.getClass();
        if (pack != null) type.getMethod("setShaderPackName", String.class).invoke(config, pack);
        type.getMethod("setShadersEnabled", boolean.class).invoke(config, pack != null);
        type.getMethod("save").invoke(config);
        Class.forName(IRIS).getMethod("reload").invoke(null);
        ArchangelClient.LOGGER.info("Iris shader pack set to {}", pack == null ? "off" : pack);
    }

    private static Object config() throws ReflectiveOperationException {
        return Class.forName(IRIS).getMethod("getIrisConfig").invoke(null);
    }
}
