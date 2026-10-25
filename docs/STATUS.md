# Status

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

## Next: milestone 3 (HUD elements, HUD editor, Target HUD)
