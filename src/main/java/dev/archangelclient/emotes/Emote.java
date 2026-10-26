package dev.archangelclient.emotes;

import dev.archangelclient.emotes.anim.Animation;
import net.minecraft.resources.Identifier;

/** A playable emote from the catalog: its display name, wheel icon and animation. */
public record Emote(String id, String name, String description, Identifier icon, Animation animation) {}
