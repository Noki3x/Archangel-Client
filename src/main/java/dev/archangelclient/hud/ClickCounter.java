package dev.archangelclient.hud;

import dev.archangelclient.core.event.EventBus;
import dev.archangelclient.core.event.MouseClickEvent;
import java.util.ArrayDeque;
import java.util.Deque;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Util;
import org.lwjgl.glfw.GLFW;

/** Counts your own left and right clicks over the last second, while playing (not in menus). */
public final class ClickCounter {
    private final Deque<Long> left = new ArrayDeque<>();
    private final Deque<Long> right = new ArrayDeque<>();

    public ClickCounter(EventBus bus) {
        bus.subscribe(MouseClickEvent.class, this::onClick);
    }

    private void onClick(MouseClickEvent event) {
        if (!event.isPress() || Minecraft.getInstance().screen != null) return;
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) left.addLast(Util.getMillis());
        else if (event.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) right.addLast(Util.getMillis());
    }

    public int left() {
        return count(left);
    }

    public int right() {
        return count(right);
    }

    private static int count(Deque<Long> clicks) {
        long cutoff = Util.getMillis() - 1000;
        while (!clicks.isEmpty() && clicks.peekFirst() < cutoff) clicks.pollFirst();
        return clicks.size();
    }
}
