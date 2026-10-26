package dev.archangelclient.emotes.anim;

/**
 * One keyframe of a channel. {@code easing} shapes the segment that ends at this keyframe (the
 * GeckoLib convention); {@code smooth} marks Bedrock {@code "lerp_mode": "catmullrom"} keys.
 */
public record Keyframe(float time, float x, float y, float z, EmoteEasing easing, boolean smooth) {}
