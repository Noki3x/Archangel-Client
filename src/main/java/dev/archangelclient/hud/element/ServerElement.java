package dev.archangelclient.hud.element;

import dev.archangelclient.hud.TextHudElement;

/** The address of the server you're playing on. */
public class ServerElement extends TextHudElement {
    public ServerElement() {
        super("Server Address", "Shows the address of the current server", 0f, 1f, false);
    }

    @Override
    protected String text(boolean editor) {
        var server = mc().getCurrentServer();
        if (server != null) return server.ip;
        return mc().isSingleplayer() ? "Singleplayer" : "Not connected";
    }
}
