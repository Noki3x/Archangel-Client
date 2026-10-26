package dev.archangelclient.gui.widget;

import dev.archangelclient.cosmetics.CosmeticType;
import dev.archangelclient.cosmetics.Loadout;
import dev.archangelclient.cosmetics.render.CosmeticRenderData;
import dev.archangelclient.gui.render.Colors;
import dev.archangelclient.gui.theme.Theme;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Full 3D preview of the signed-in player wearing a loadout, drawn through the real player
 * renderer (so it shows exactly what the world shows, cosmetic layer included). Works on the title
 * screen too: the render state is built by hand, no player entity is needed. Drag to rotate; the
 * idle spin and mouse-follow arrive with the polish pass in milestone 10.
 */
public class CosmeticPreview extends UiWidget {
    private static final float DRAG_SENSITIVITY = 2.5f;
    private static final long SWING_PERIOD_MS = 1600;
    private static final long SWING_MS = 300;

    private final Supplier<PlayerSkin> skin;
    private final AvatarRenderState state = new AvatarRenderState();
    private Loadout loadout = Loadout.EMPTY;
    private float rotation = -25f;
    private float pitch = -5f;
    private boolean dragging;
    private double lastMouseX, lastMouseY;
    private boolean demoSwing;
    private int offsetX, offsetY;
    private final boolean showcase;

    /** @param showcase demonstrate effects too: hold a sword and loop a swing when a trail is worn */
    public CosmeticPreview(boolean showcase) {
        this.showcase = showcase;
        Minecraft mc = Minecraft.getInstance();
        this.skin = mc.player != null ? mc.player::getSkin : mc.getSkinManager().createLookup(mc.getGameProfile(), false);
    }

    public void setLoadout(Loadout loadout) {
        this.loadout = loadout;
        // show swing trails with a sword in hand and a looping swing
        boolean wantsSword = showcase && loadout.get(CosmeticType.TRAIL) != null;
        if (wantsSword != demoSwing) {
            demoSwing = wantsSword;
            ItemStack stack = wantsSword ? new ItemStack(Items.DIAMOND_SWORD) : ItemStack.EMPTY;
            state.rightHandItemStack = stack;
            Minecraft.getInstance().getItemModelResolver()
                    .updateForTopItem(state.rightHandItemState, stack, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, null, null, 0);
            state.rightArmPose = wantsSword ? HumanoidModel.ArmPose.ITEM : HumanoidModel.ArmPose.EMPTY;
        }
    }

    /** Screen-space shift applied to the panel this frame (the preview ignores the GUI pose). */
    public void setRenderOffset(int dx, int dy) {
        this.offsetX = dx;
        this.offsetY = dy;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        long now = Util.getMillis();
        int x0 = x + offsetX, y0 = y + offsetY;
        // soft elliptical platform glow under the feet
        int cx = x0 + width / 2, floor = y0 + height - 12;
        for (int i = 0; i < 10; i++) {
            float t = i / 9f;
            int rw = Math.round(width * 0.36f * (1 - t * 0.85f)), rh = Math.max(1, Math.round(7 * (1 - t)));
            g.fill(cx - rw, floor - rh, cx + rw, floor + rh, Colors.withAlpha(Theme.ACCENT, 0x0A));
        }

        state.skin = skin.get();
        state.entityType = EntityType.PLAYER;
        state.ageInTicks = (now % 1_000_000L) / 50f;
        state.bodyRot = 180f + rotation;
        state.yRot = 0;
        state.xRot = pitch;
        state.lightCoords = 0xF000F0;
        state.boundingBoxHeight = 1.8f;
        state.boundingBoxWidth = 0.6f;
        state.eyeHeight = 1.62f;
        state.scale = 1f;
        state.isInvisible = false;
        state.walkAnimationSpeed = 0;
        state.attackTime = 0;
        if (demoSwing) {
            long t = now % SWING_PERIOD_MS;
            if (t < SWING_MS) state.attackTime = t / (float) SWING_MS;
        }
        state.setData(CosmeticRenderData.KEY, loadout.isEmpty() ? null : new CosmeticRenderData(loadout, true, true, -1));

        float scale = height / 2.75f;
        Quaternionf view = new Quaternionf().rotateZ(Mth.PI);
        Quaternionf tilt = new Quaternionf().rotateX(pitch * 0.4f * Mth.DEG_TO_RAD);
        view.mul(tilt);
        Vector3f translation = new Vector3f(0, state.boundingBoxHeight / 2 + 0.12f, 0);
        g.submitEntityRenderState(state, scale, translation, view, tilt, x0, y0, x0 + width, y0 + height);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || !isHovered(mouseX, mouseY)) return false;
        dragging = true;
        lastMouseX = mouseX;
        lastMouseY = mouseY;
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button) {
        if (!dragging) return false;
        rotation -= (float) (mouseX - lastMouseX) * DRAG_SENSITIVITY;
        pitch = Mth.clamp(pitch - (float) (mouseY - lastMouseY) * DRAG_SENSITIVITY * 0.5f, -30f, 30f);
        lastMouseX = mouseX;
        lastMouseY = mouseY;
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        boolean was = dragging;
        dragging = false;
        return was;
    }
}
