# Status

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

## Next: milestone 2 (core: events, modules, settings, config profiles, ClientSettingsScreen)
