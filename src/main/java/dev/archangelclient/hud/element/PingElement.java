package dev.archangelclient.hud.element;

import dev.archangelclient.hud.TextHudElement;

/** Your latency to the server, as shown in the vanilla player list. */
public class PingElement extends TextHudElement {
    public PingElement() {
        super("Ping", "Shows your latency to the server", 0f, 0.12f, false);
    }

    @Override
    protected String text(boolean editor) {
        var connection = mc().getConnection();
        var player = mc().player;
        if (connection == null || player == null || mc().isSingleplayer()) return "0 ms";
        var info = connection.getPlayerInfo(player.getUUID());
        return (info == null ? 0 : info.getLatency()) + " ms";
    }
}
