package dev.archangelclient.core.event;

import net.minecraft.world.entity.Entity;

/**
 * Posted when the local player attacks an entity through the normal vanilla attack path. Used by
 * display-only features (combo counter, reach display, target HUD); it never changes the attack.
 */
public record AttackEntityEvent(Entity target) {}
