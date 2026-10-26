package dev.archangelclient.cosmetics;

import dev.archangelclient.core.module.Category;
import dev.archangelclient.core.module.Module;
import dev.archangelclient.core.setting.BooleanSetting;
import dev.archangelclient.core.setting.NumberSetting;
import dev.archangelclient.core.setting.StringSetting;
import java.util.EnumMap;
import java.util.Map;

/**
 * Cosmetics on/off plus the tuning options for effects. What is equipped is stored here too (one
 * hidden setting per slot), so loadouts follow config profiles; the cosmetics screen edits them.
 *
 * <p>Everything is visual only and renders with normal depth testing: nothing shows through walls.
 */
public class CosmeticsModule extends Module {
    public final BooleanSetting showOwn = add(new BooleanSetting("Show my cosmetics",
            "Render your equipped cosmetics in third person", true));
    public final BooleanSetting showOthers = add(new BooleanSetting("Show other players' cosmetics",
            "Render cosmetics other Archangel players have equipped", true));
    public final NumberSetting auraIntensity = add(new NumberSetting("Aura intensity",
            "Brightness of your aura's glow", 1, 0.2, 1.5, 0.1).suffix("x"));
    public final BooleanSetting afterimages = add(new BooleanSetting("Aura afterimages",
            "Holographic copies trail behind you while you move", true));
    public final NumberSetting afterimageCount = add(new NumberSetting("Afterimage count",
            "How many afterimages trail behind you", 3, 1, 8, 1).visibleWhen(afterimages::get));
    public final BooleanSetting auraRing = add(new BooleanSetting("Aura ring",
            "Glowing ring under your feet", true));
    public final BooleanSetting auraParticles = add(new BooleanSetting("Aura particles",
            "Glowing motes rise around you", true));
    public final BooleanSetting weaponsOnly = add(new BooleanSetting("Trail on weapons only",
            "Only draw swing trails while holding a sword, axe, trident or mace", true));
    public final BooleanSetting firstPersonTrail = add(new BooleanSetting("First-person trail",
            "Also draw your swing trail in first person", true));
    public final NumberSetting sparkCount = add(new NumberSetting("Hit spark count",
            "Sparks per hit with a hit effect equipped", 14, 4, 30, 1));

    private final Map<CosmeticType, StringSetting> slots = new EnumMap<>(CosmeticType.class);
    private Loadout loadout;

    public CosmeticsModule() {
        super("Cosmetics", "Capes, wings, halos, auras, swing trails and hit effects", Category.COSMETICS, true);
        for (CosmeticType type : CosmeticType.values()) {
            StringSetting slot = add(new StringSetting("equipped_" + type.key(), "", "", 64));
            slot.visibleWhen(() -> false);
            slot.onChange(() -> loadout = null);
            slots.put(type, slot);
        }
    }

    /** The local player's loadout from the active profile. */
    public Loadout loadout() {
        Loadout l = loadout;
        if (l == null) {
            Map<CosmeticType, String> map = new EnumMap<>(CosmeticType.class);
            slots.forEach((type, s) -> map.put(type, s.get()));
            loadout = l = Loadout.of(map);
        }
        return l;
    }

    public void equip(CosmeticType type, String id) {
        slots.get(type).set(id == null ? "" : id);
    }
}
