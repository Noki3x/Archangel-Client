package dev.archangelclient.performance;

import dev.archangelclient.core.event.TickEvent;
import dev.archangelclient.core.module.Category;
import dev.archangelclient.core.module.Module;
import dev.archangelclient.core.setting.ModeSetting;
import dev.archangelclient.core.setting.NumberSetting;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ParticleStatus;

/**
 * Lighter particles and entity rendering for higher frame rates, plus a check for the performance
 * mods that help most. The particle cap is enforced by {@code ParticleEngineMixin}.
 */
public class FpsBoostModule extends Module {
    private static final String ALL = "All";
    private static final String DECREASED = "Decreased";
    private static final String MINIMAL = "Minimal";
    private static final double PERCENT = 100.0;

    /** Recommended companion mods: Fabric mod id, display name, why it helps. */
    private static final List<String[]> RECOMMENDED = List.of(
            new String[] {"sodium", "Sodium", "Rewrites chunk rendering, the biggest FPS gain"},
            new String[] {"entityculling", "Entity Culling", "Skips entities and block entities you can't see"},
            new String[] {"immediatelyfast", "ImmediatelyFast", "Speeds up HUD, text and entity drawing"},
            new String[] {"ferritecore", "FerriteCore", "Cuts memory use"});

    private final ModeSetting particles = add(new ModeSetting("Particles", "Vanilla particle amount while FPS Boost is on",
            DECREASED, ALL, DECREASED, MINIMAL));
    private final NumberSetting particleLimit = add(new NumberSetting("Particle limit", "Most particles alive at once",
            2000, 250, 16000, 250));
    private final NumberSetting entityDistance = add(new NumberSetting("Entity distance", "Vanilla entity render distance",
            75, 50, 100, 5).suffix("%"));

    private final OptionOverride<ParticleStatus> particleOption = new OptionOverride<>();
    private final OptionOverride<Double> entityDistanceOption = new OptionOverride<>();

    public FpsBoostModule() {
        super("FPS Boost", "Lighter particles and entities, plus mod tips", Category.PERFORMANCE, false);
        on(TickEvent.class, e -> apply());
    }

    private void apply() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options == null) return;
        particleOption.apply(mc.options.particles(), switch (particles.get()) {
            case ALL -> ParticleStatus.ALL;
            case MINIMAL -> ParticleStatus.MINIMAL;
            default -> ParticleStatus.DECREASED;
        });
        entityDistanceOption.apply(mc.options.entityDistanceScaling(), entityDistance.get() / PERCENT);
    }

    @Override
    protected void onDisable() {
        particleOption.restore();
        entityDistanceOption.restore();
    }

    /** Particle cap while enabled, or -1 for no extra cap. */
    public int particleCap() {
        return isEnabled() ? particleLimit.getInt() : -1;
    }

    @Override
    public List<Note> getNotes() {
        List<Note> notes = new ArrayList<>();
        for (String[] mod : RECOMMENDED) {
            boolean installed = FabricLoader.getInstance().isModLoaded(mod[0]);
            notes.add(new Note(mod[1] + (installed ? " is installed" : " is recommended"),
                    installed ? mod[2] : "modrinth.com/mod/" + mod[0] + "  ·  " + mod[2], installed));
        }
        return notes;
    }
}
