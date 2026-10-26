package dev.archangelclient.cosmetics.model;

import java.util.function.Function;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;

/**
 * A baked Blockbench cosmetic with simple procedural motion. Animation runs in {@link #setupAnim},
 * which the renderer calls per player at draw time, so one model instance serves every player.
 *
 * <p>Bones named {@code left_wing} and {@code right_wing} flap; the root can spin and bob (halos).
 */
public class CosmeticModel extends Model<AvatarRenderState> {
    public enum Attach { HEAD, BODY }

    private final Attach attach;
    private final Animation animation;
    private final @Nullable ModelPart leftWing;
    private final @Nullable ModelPart rightWing;

    public record Animation(float flapSpeed, float flapAngle, float spin, float bob) {
        public static final Animation NONE = new Animation(0, 0, 0, 0);
    }

    public CosmeticModel(ModelPart root, Attach attach, Animation animation) {
        super(root, RenderTypes::entityTranslucent);
        this.attach = attach;
        this.animation = animation;
        Function<String, ModelPart> lookup = root.createPartLookup();
        this.leftWing = lookup.apply("left_wing");
        this.rightWing = lookup.apply("right_wing");
    }

    public Attach attach() {
        return attach;
    }

    @Override
    public void setupAnim(AvatarRenderState state) {
        super.setupAnim(state);
        float age = state.ageInTicks;
        if (animation.flapAngle() != 0 && leftWing != null && rightWing != null) {
            // gentle idle flap, wider and faster while moving or falling
            float effort = Mth.clamp(state.walkAnimationSpeed * 1.5f, 0f, 1f);
            if (state.isFallFlying) effort = 1f;
            float speed = animation.flapSpeed() * (1f + effort);
            float angle = animation.flapAngle() * (1f + effort * 0.8f) * Mth.DEG_TO_RAD;
            float flap = Mth.sin(age * speed) * angle;
            leftWing.yRot += flap;
            rightWing.yRot -= flap;
            if (state.isCrouching) {
                leftWing.yRot += 0.25f;
                rightWing.yRot -= 0.25f;
            }
        }
        if (animation.spin() != 0) root.yRot += age * animation.spin() * Mth.DEG_TO_RAD;
        if (animation.bob() != 0) root.y += Mth.sin(age * 0.08f) * animation.bob();
    }
}
