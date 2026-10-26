package dev.archangelclient.gui.theme;

/**
 * Every color, size and timing used by the Archangel UI lives here, so the look can be tuned in one
 * place. Colors are ARGB.
 */
public final class Theme {
    private Theme() {}

    // Background
    public static final int BG_DARK = 0xFF16171B;
    public static final int BG_LIGHT = 0xFF1E1F24;
    public static final int VIGNETTE = 0xFF0B0C0E;
    public static final float VIGNETTE_STRENGTH = 0.55f;

    // Text
    public static final int TEXT = 0xFFFFFFFF;
    public static final int TEXT_MUTED = 0xFF9CA0AB;
    public static final int TEXT_DIM = 0xFF5E626C;

    // Accents
    public static final int ACCENT = 0xFF3B82F6;
    public static final int ACCENT_LIGHT = 0xFF60A5FA;
    public static final int ACCENT_PURPLE = 0xFF8B5CF6;

    // Panels and buttons
    public static final int PANEL = 0xB01E1F24;
    public static final int HOVER_BAR = 0x33FFFFFF;
    public static final int RADIUS = 6;
    public static final int RADIUS_LARGE = 10;

    // Client settings screen
    public static final int PANEL_SOLID = 0xD418191D;
    public static final int SIDEBAR = 0x40000000;
    public static final int CARD = 0x12FFFFFF;
    public static final int CARD_HOVER = 0x1FFFFFFF;
    public static final int FIELD = 0x1AFFFFFF;
    public static final int DIVIDER = 0x14FFFFFF;
    public static final int TOGGLE_OFF = 0x40FFFFFF;
    public static final int DANGER = 0xFFEF4444;
    public static final int SETTINGS_MAX_WIDTH = 520;
    public static final int SETTINGS_MAX_HEIGHT = 300;
    public static final int SIDEBAR_WIDTH = 112;
    public static final int CARD_HEIGHT = 64;
    public static final int GAP = 6;
    public static final int ROW_HEIGHT = 28;
    public static final long VIEW_SLIDE_MS = 220;

    // Glow
    public static final int GLOW_LAYERS = 6;
    public static final int GLOW_ALPHA = 0x18;

    // Animation timings (milliseconds)
    public static final long HOVER_MS = 150;
    public static final long SCREEN_FADE_MS = 300;
    public static final long BEAM_CYCLE_MS = 14_000;

    // Title screen layout (GUI pixels)
    public static final int MENU_BUTTON_WIDTH = 150;
    public static final int MENU_BUTTON_HEIGHT = 18;
    public static final int MENU_BUTTON_GAP = 3;
    public static final int MENU_GROUP_GAP = 8;
    public static final int LOGO_SIZE = 56;
    public static final int LOGO_MIN_SIZE = 28;
    public static final int TITLE_GAP = 6;
    public static final int PREVIEW_WIDTH = 140;
    public static final int PREVIEW_HEIGHT = 170;
    public static final int CORNER_MARGIN = 6;
    public static final float MENU_COLUMN_X = 0.35f;
    public static final float PREVIEW_COLUMN_X = 0.75f;

    // Light beams (fan of rays from above the top-right corner)
    public static final int BEAM_COUNT = 6;
    public static final int BEAM_SEGMENTS = 14;
    public static final float BEAM_ORIGIN_X = 0.85f;
    public static final float BEAM_ANGLE_RAD = 0.55f;
    public static final float BEAM_FAN_RAD = 0.9f;
    public static final float BEAM_SWAY_RAD = 0.05f;
    public static final int BEAM_ALPHA = 0x30;
    public static final int BEAM_WHITE = 0xFFE6ECFF;
    public static final int BEAM_TINT = 0xFF8FB4FF;
}
