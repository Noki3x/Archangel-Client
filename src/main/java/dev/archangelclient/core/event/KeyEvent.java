package dev.archangelclient.core.event;

import org.lwjgl.glfw.GLFW;

/** A raw keyboard event. {@code action} is a GLFW action (press, release or repeat). */
public record KeyEvent(int key, int scancode, int action, int modifiers) {
    public boolean isPress() {
        return action == GLFW.GLFW_PRESS;
    }
}
