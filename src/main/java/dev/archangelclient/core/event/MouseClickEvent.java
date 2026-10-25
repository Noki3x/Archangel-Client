package dev.archangelclient.core.event;

import org.lwjgl.glfw.GLFW;

/** A raw mouse button event. {@code action} is a GLFW action (press or release). */
public record MouseClickEvent(int button, int action) {
    public boolean isPress() {
        return action == GLFW.GLFW_PRESS;
    }
}
