# Status

## Milestone 4: done (2026-10-25)
Animations, all visual only (nothing changes what's sent to the server). Tested in a world
(screenshots: docs/screenshots/m4-*.png).

- `1.7 Animations` module: blockhit (hold use with a sword: old block pose, swing still plays in
  it), old swing (no attack-cooldown dip), old eat/drink and old bow (swing shows while using),
  "swing while using" (local-only `LivingEntity.swing(hand, false)`, no swing packet), and the
  blocking arm pose on your own player in third person (`AvatarRendererMixin`).
- `Custom Swing` module: swing speed 0.5-2x (`LivingEntityMixin#getCurrentSwingDuration`, your
  own player only) and a smooth (eased) hit.
- `Item Position` module: X/Y/Z offset, scale, X/Y/Z rotation, mirrored for the left hand.
- Hooks live in `ItemInHandRendererMixin` (Mojang name for HeldItemRenderer).


## Milestone 3: done (2026-10-25)
HUD system, HUD editor and Target HUD. Build passes; tested in a singleplayer world against a
200 HP husk (screenshots: docs/screenshots/m3-*.png).

- `hud/HudElement`: a Module with hidden x/y settings (0..1 of free space, so layouts survive
  resizes), scale, background and text color; saved per profile. `HudManager` draws them.
- Elements: FPS, CPS (left/right), Keystrokes (press animation, CPS on mouse buttons), Ping,
  Coordinates + direction, Armor Status, Potion Effects, Clock, Speed, Server Address, Memory,
  Combo, Reach Display, Target HUD.
- `CombatTracker` (combo, reach, target, damage samples) and `ClickCounter` (CPS) only observe
  your own vanilla attacks/clicks (`AttackEntityEvent`, `MouseClickEvent` via `MouseHandlerMixin`).
- Target HUD: shows only after you hit an entity, only while it's alive, within 8 blocks and in
  direct line of sight (`hasLineOfSight`), hides 1-3 s (default 2.5) after your last hit. Model
  preview, name, distance, eased health bar red -> yellow -> green, damage graph. Verified it hides
  when a block is placed between you and the target.
- `HudEditorScreen`: grid, center lines, snapping to screen edges/center and other elements with
  guide lines (hold Shift to disable), scroll to resize, arrow keys nudge, right-click opens the
  element's settings, Reset layout. Opens from Client settings > HUD > "Edit HUD Layout" or an
  optional keybind in vanilla Controls.


## Milestone 2: done (2026-10-25)
Core systems and the Client settings screen. Build passes; tested in the running game under Xvfb
(title screen and in a singleplayer world). Screenshots: docs/screenshots/m2-*.png.

- `core/event`: `EventBus` plus `TickEvent`, `Render2DEvent` (Fabric HUD layer), `Render3DEvent`
  (after entities), `KeyEvent` (`KeyboardHandlerMixin`), `AttackEntityEvent` (observe only, never cancels).
- `core/module`: `Module` (handlers declared with `on(...)` are subscribed only while enabled),
  `ModuleManager` (keybinds toggle modules only when no screen is open), `Category`.
- `core/setting`: Boolean, Number (slider, step snapping, suffix), Mode, Color (#AARRGGBB), Keybind;
  `visibleWhen(...)` hides options that don't apply.
- `core/config/ConfigManager`: `config/archangelclient/<profile>.json`, auto-save 750 ms after a change
  and on exit, atomic writes, create/switch/delete, export (file in `exports/` + clipboard),
  import from clipboard, open folder.
- `ClientSettingsScreen`: category sidebar, search (or just start typing), module cards with toggles,
  settings pages generated from each module's settings, keybind capture, reset to defaults,
  Profiles page. Opens from the menu's Client button and in game with Right Shift (rebindable in
  vanilla Controls). Vanilla blur behind it in game, the animated backdrop on the menu.
- First modules: FPS (HUD; becomes a movable HUD element in milestone 3), Menu Style (Visuals),
  Unfocused FPS (Performance, via `FramerateLimitTrackerMixin`).
- Widgets: `UiWidget` base, Toggle, Slider, Dropdown, ColorPicker (HSB + alpha), KeybindButton,
  TextField, TextButton, ScrollPanel, ModuleCard.


## Milestone 1: done (2026-10-25)
`./gradlew build` passes and `./gradlew runClient` reaches the Archangel title screen under Xvfb.
Screenshot: docs/screenshots/m1-title-screen.png.

Changes made in the build session:
- Target moved to Minecraft 1.21.11 (latest 1.21.x), Fabric Loader 0.18.4, Fabric API 0.141.6+1.21.11,
  Loom 1.14.10, Gradle 9.2.1.
- 1.21.11 Mojang names: `ResourceLocation` is now `Identifier`, `Util` moved to `net.minecraft.util`,
  `AbstractButton.renderWidget` is final (override `renderContents`), `Minecraft.ON_OSX` replaced by
  `Util.getPlatform()`.
- Access widener namespace set to `named` (required with Mojang mappings).
- Light beams reworked into a fan of soft rays from the top-right, as in reference 1.
- Logo shrinks on short windows so the menu column never overlaps the version text.

Known gaps, scheduled for later milestones:
- "Client" and "Cosmetics" buttons open placeholder screens (milestones 2 and 5).
- Skin preview supports drag-to-rotate (vanilla widget); idle spin + mouse follow is milestone 10.
- Rotated beam segments show faint diamond banding at their ends; smooth with a shader in milestone 8.

## Running locally
`./gradlew runClient` (add `--args="--width 1700 --height 950"` for a bigger window).
In the cloud: `Xvfb :99 &` then `DISPLAY=:99 ./gradlew runClient`.

## Next: milestone 5 (cosmetics: capes, wings, halos, aura, trails, cosmetics screen)
