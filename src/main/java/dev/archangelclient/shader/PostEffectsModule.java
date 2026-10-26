package dev.archangelclient.shader;

import dev.archangelclient.core.module.Category;
import dev.archangelclient.core.module.Module;
import dev.archangelclient.core.setting.BooleanSetting;
import dev.archangelclient.core.setting.ColorSetting;
import dev.archangelclient.core.setting.NumberSetting;

/**
 * Built-in post-processing (custom GLSL, run through Minecraft's post chain): motion blur, bloom,
 * color grading, vignette and chromatic aberration. Only changes how the frame looks; applied to
 * the world, never to the HUD or menus.
 */
public class PostEffectsModule extends Module {
    public final BooleanSetting motionBlur = add(new BooleanSetting("Motion blur", "Blend each frame with the previous ones", false));
    public final NumberSetting motionBlurStrength = add(new NumberSetting("Motion blur strength",
            "How much of the previous frames stays visible", 0.5, 0.1, 0.9, 0.05).visibleWhen(motionBlur::get));

    public final BooleanSetting bloom = add(new BooleanSetting("Bloom", "Soft glow around bright light", true));
    public final NumberSetting bloomIntensity = add(new NumberSetting("Bloom intensity", "Strength of the glow",
            0.7, 0.1, 2, 0.05).visibleWhen(bloom::get));
    public final NumberSetting bloomThreshold = add(new NumberSetting("Bloom threshold", "How bright something must be to glow",
            0.75, 0.3, 1, 0.05).visibleWhen(bloom::get));
    public final NumberSetting bloomRadius = add(new NumberSetting("Bloom radius", "How far the glow spreads",
            1.5, 0.5, 4, 0.1).visibleWhen(bloom::get));

    public final BooleanSetting colorGrading = add(new BooleanSetting("Color grading", "Saturation, contrast and tint", true));
    public final NumberSetting saturation = add(new NumberSetting("Saturation", "0 is grayscale, 1 is unchanged",
            1.15, 0, 2, 0.05).visibleWhen(colorGrading::get));
    public final NumberSetting contrast = add(new NumberSetting("Contrast", "1 is unchanged",
            1.05, 0.5, 1.5, 0.05).visibleWhen(colorGrading::get));
    public final ColorSetting tint = add(new ColorSetting("Tint color", "Color mixed into the image",
            0xFF8B5CF6, false).visibleWhen(colorGrading::get));
    public final NumberSetting tintStrength = add(new NumberSetting("Tint strength", "How much of the tint color to mix in",
            0.1, 0, 1, 0.05).visibleWhen(colorGrading::get));

    public final BooleanSetting vignette = add(new BooleanSetting("Vignette", "Darken the edges of the screen", true));
    public final NumberSetting vignetteStrength = add(new NumberSetting("Vignette strength", "How dark the edges get",
            0.35, 0.05, 1, 0.05).visibleWhen(vignette::get));

    public final BooleanSetting chromatic = add(new BooleanSetting("Chromatic aberration", "Split colors toward the edges", false));
    public final NumberSetting chromaticAmount = add(new NumberSetting("Aberration amount", "How far colors split",
            0.6, 0.1, 2, 0.05).visibleWhen(chromatic::get));

    public PostEffectsModule() {
        super("Post Effects", "Bloom, motion blur, color grading and more", Category.VISUALS, false);
    }

    @Override
    protected void onDisable() {
        PostEffects.INSTANCE.close();
    }
}
