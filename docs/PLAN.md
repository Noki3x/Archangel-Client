# Archangel Client: plan

Legit PvP/cosmetics client for Fabric, Minecraft 1.21.10, Java 21, official Mojang mappings.
Hard rule: no cheats. Everything is cosmetic, UI/HUD, or performance. Target HUD and nametag
features only use information the vanilla client already shows in direct line of sight.

## Milestones
Each one ends with `./gradlew build` + `./gradlew runClient` (under Xvfb in the cloud) and a short summary.

1. **Project setup + main menu**: Gradle/Loom, fabric.mod.json, mixins, Theme, RenderUtil, Inter font,
   wings logo + window icon, ArchangelTitleScreen (logo, text buttons with hover bar, skin preview,
   version text, cosmetics button, animated light beams, fade-in).
2. **Core**: event bus, Module/ModuleManager/Category, settings, ConfigManager with profiles
   (`config/archangelclient/<profile>.json`, import/export), ClientSettingsScreen (sidebar, search,
   module cards, auto-generated settings panels), Right Shift keybind.
3. **HUD**: HudElement base, all listed elements, HUD editor (grid, snap guides, reset), Target HUD.
4. **Animations**: 1.7 blockhit, old swing/eat/drink/bow, item position/scale/rotation, swing speed.
5. **Cosmetics**: capes (animated), wings, hats/halos, aura (own player only), weapon trails, hit
   sparks, Blockbench JSON loader, CosmeticsScreen.
6. **Emotes**: keyframe system, 6 emotes, emote wheel (hold B), third-person camera, cancel on move.
7. **Nametags**: display names, wings badge, nametag settings.
8. **Shaders/visuals**: post effects, nebula skybox, brightness slider within vanilla limits, Iris page,
   performance toggles.
9. **Backend**: Node/TS, Fastify, Prisma/SQLite, hasJoined auth + JWT, REST + WebSocket sync,
   client offline fallback.
10. **Polish**: idle spin/mouse-follow on the skin preview, transitions everywhere, README.

## File structure
```
build.gradle, settings.gradle, gradle.properties, gradlew
src/main/java/dev/archangelclient/
  ArchangelClient.java
  core/{event,module,setting,config}/
  gui/theme/        Theme, Easing, Animation, Fonts
  gui/render/       RenderUtil, Colors, MenuBackground, WindowIcon
  gui/screen/       ArchangelTitleScreen, FadingScreen, PlaceholderScreen (+ later screens)
  gui/widget/       RoundedButton, PlayerPreview (+ later widgets)
  hud/ cosmetics/ emotes/ animation/ nametag/ shader/ net/
  mixin/            MinecraftMixin
src/main/resources/
  fabric.mod.json, archangelclient.mixins.json, archangelclient.accesswidener
  assets/archangelclient/{font,lang,textures/gui}/
tools/gen_logo.py   regenerates logo.svg/png and icons (needs `pip install cairosvg` + ImageMagick)
backend/            milestone 9
docs/               plan and status
```

## Assets to replace with your own art
- `assets/archangelclient/textures/gui/logo.png` / `logo.svg` (generated wings logo)
- `assets/archangelclient/textures/gui/icon_16.png`, `icon_32.png`, `icon_64.png`, `icon_128.png`, `assets/archangelclient/icon.png`
- Fonts: Inter 4.1 (OFL, license bundled), swap for Poppins if preferred.
