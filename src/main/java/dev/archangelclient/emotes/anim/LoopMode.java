package dev.archangelclient.emotes.anim;

/** Bedrock's {@code "loop"} field: {@code false}, {@code true} or {@code "hold_on_last_frame"}. */
public enum LoopMode {
    /** Plays once, then the emote ends. */
    ONCE,
    /** Repeats until cancelled. */
    LOOP,
    /** Plays once and holds the last pose until cancelled. */
    HOLD
}
