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
    /** Upper bound for the TTF raster scale; matches the largest GUI scale we expect to see. */
    public static final int FONT_MAX_OVERSAMPLE = 8;

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
    public static final int SUCCESS = 0xFF22C55E;
    public static final int WARNING = 0xFFF59E0B;
    public static final int NOTE_HEIGHT = 24;
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
    public static final long SCREEN_FADE_OUT_MS = 140;
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

    // 3D player preview (title screen, Locker)
    /** Starting body yaw: turned a little so the cape and wings show. */
    public static final float PREVIEW_REST_YAW = -25f;
    /** Idle spin starts this long after the last drag. */
    public static final long PREVIEW_IDLE_DELAY_MS = 2500;
    public static final float PREVIEW_SPIN_DEG_PER_SEC = 18f;
    /** Spin eases in over this long instead of starting at full speed. */
    public static final long PREVIEW_SPIN_RAMP_MS = 1200;
    /** How far the head turns and tilts toward the cursor (degrees). */
    public static final float PREVIEW_HEAD_MAX_YAW = 50f;
    public static final float PREVIEW_HEAD_MAX_PITCH = 30f;
    /** Cursor distance (GUI pixels) that maps to about 45 degrees of head turn. */
    public static final float PREVIEW_FOLLOW_RANGE = 60f;
    /** Head smoothing rate (per second): higher snaps faster to the cursor. */
    public static final float PREVIEW_FOLLOW_RATE = 8f;

    // Startup intro (first title screen of a session). Times in ms from when the menu first opens.
    /** The big centered logo fades and scales in over this long... */
    public static final long INTRO_LOGO_IN_MS = 650;
    /** ...holds... */
    public static final long INTRO_LOGO_HOLD_MS = 550;
    /** ...then shrinks and glides to its place in the menu. */
    public static final long INTRO_LOGO_MOVE_MS = 900;
    /** Big logo size as a fraction of the shorter screen side. */
    public static final float INTRO_LOGO_FRACTION = 0.42f;
    /** Starting scale of the big logo as it fades in. */
    public static final float INTRO_LOGO_START_SCALE = 0.82f;
    /** Menu items fade in one after another once the logo starts moving. */
    public static final long INTRO_ITEMS_DELAY_MS = 350;
    public static final long INTRO_ITEM_STAGGER_MS = 80;
    public static final long INTRO_ITEM_FADE_MS = 420;
    /** Each menu item rises this far (GUI pixels) as it fades in. */
    public static final int INTRO_ITEM_RISE = 6;
    /** The player starts walking in this long after the logo settles. */
    public static final long INTRO_WALK_DELAY_MS = 150;
    public static final long INTRO_WALK_MS = 1500;
    /** Then turns from walking sideways to face the viewer. */
    public static final long INTRO_TURN_MS = 500;
    /** Body yaw while walking in from the right (degrees, preview convention). */
    public static final float INTRO_WALK_YAW = 90f;
    /** Limb swing per block walked, matching an entity walking normally. */
    public static final float INTRO_WALK_CYCLE_PER_BLOCK = 4f;
    public static final float INTRO_WALK_SPEED = 0.85f;
    /** Soft halo behind the big logo. */
    public static final int INTRO_LOGO_GLOW = 0xFF3B82F6;
    /** Halo radius relative to the logo size. */
    public static final float INTRO_LOGO_GLOW_RADIUS = 0.75f;
    /** Radial glows: number of stacked discs, and each disc's alpha at full strength. */
    public static final int RADIAL_GLOW_STEPS = 16;
    public static final int RADIAL_GLOW_ALPHA = 0x07;

    // Light beams (fan of rays from above the top-right corner)
    public static final int BEAM_COUNT = 6;
    public static final float BEAM_ORIGIN_X = 0.85f;
    public static final float BEAM_ANGLE_RAD = 0.55f;
    public static final float BEAM_FAN_RAD = 0.9f;
    public static final float BEAM_SWAY_RAD = 0.05f;
    public static final int BEAM_ALPHA = 0x30;
    public static final int BEAM_WHITE = 0xFFE6ECFF;
    public static final int BEAM_TINT = 0xFF8FB4FF;
}
