# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

Survival Assistant is a Fabric mod for Minecraft **26.3** (unobfuscated, Java 25, Fabric Loom 1.19). A guide NPC takes the player through 18 tutorial chapters. Around it: a task card, an environment card, mob/ore info, a recipe card, a light check, safety warnings and an optional small-talk feature that uses a local language model. The README is the user-facing documentation, including all config options and commands.

## Commands

The JDK is not on PATH. Set it first in Git Bash:

```bash
export JAVA_HOME="C:/Program Files/Microsoft/jdk-25.0.5.102-hotspot"
./gradlew build                      # compile, test, jar
./gradlew test --tests "io.github.visserdavid.survivalassistant.advice.GuideChoicesTest"   # one test class
./gradlew installToModrinth          # copy the jar into the Modrinth App instance "ModTesting" (close Minecraft first: the old jar is locked)
python tools/make_lang.py src/main/resources/assets/survivalassistant/lang   # regenerate + check all language files
python tools/recipe_check.py         # check every recipe card against the game data in the Minecraft jar
```

- **Build output** lives in `%LOCALAPPDATA%/gradle-builds/survivalassistant`, not in `build/`, because OneDrive locks files there. Test results are in `test-results/test/*.xml` under that folder. The jars are also copied to the project root, where git ignores them.
- **CI** (`.github/workflows/build.yml`) runs the build. It also fails when `make_lang.py` changes any generated language file, and when `recipe_check.py` reports a mismatch.

## Architecture

**Source sets.** Loom uses split source sets:
- `src/main`: common and server code; the guide logic runs on the server.
- `src/client`: rendering, HUD, keys, the settings screen and skins.

**Server loop.** `GuideManager` is the hub, ticked from `SurvivalAssistant`. It delegates to `GuideSessions` (per-player runtime state), `GuideEntities` (the NPC), `GuideSpeech` (chat lines), `ChapterMenu` and `GuideNavigation`. Each evaluation runs in this order:
1. `advice/ContextCollector` reads the world into an immutable `SurvivalContext`.
2. `AdviceEngine`/`TipRegistry` pick tips.
3. `GuideLifecycle` decides chapter flow (continue, congratulate, offer chapter, remind, farewell).
4. `HudSnapshot` is built for the client.

**Pure logic, testable without Minecraft.** Most of `advice/` and `chat/` is plain Java:
- chapters and goals (`Chapters`, `Chapter`, `Recipes`, `Gear`, `OreTable`, `BiomeGroup`);
- the lifecycle;
- crosshair advice;
- the small-talk prompt and scheduling.

Tests live under `src/test` and cover this layer. Tests must not touch `SurvivalAssistant` itself: loading that class registers the entity type and fails outside the game. For ids, use `Identifier.fromNamespaceAndPath(SurvivalAssistant.MOD_ID, …)`. `MOD_ID` is a compile-time constant, so using it does not load the class.

**State and sync.** Both use Fabric attachments in `state/`:
- `GuideState`: per-player `GuideProgress`, saved with the player through `GuideProgressCodec`.
  - **Never rename a codec key**: old worlds use them. Unknown keys are ignored.
  - DFU's `optionalFieldOf(name, default)` leaves out values that equal the default.
- `HudState`: a `HudSnapshot` synced to the owning client only. It is not saved and is sent only when it changed.
- `SettingsPayloads`: custom payloads for the settings screen.
  - Request → Sync: the player's `GuideChoices`, the `ModConfig` as JSON, and `canEdit`.
  - ChoicesUpdate and ConfigUpdate back to the server.
  - `GuideSettings` handles them on the server and checks again that the sender is the singleplayer owner or a gamemaster-level operator.

**Config.** There are two files:
- `config/ModConfig`: server, `config/survivalassistant.json`;
- `client/hud/ClientConfig`: client, `config/survivalassistant-client.json`.

Both are Gson through `JsonFiles`, have `validate()`, and are never wiped when unreadable. The settings screen edits copies (`copy()`) and applies them with `ModConfig.update` / `ClientConfig.set`.

**Client HUD.** These are Fabric `HudElement`s, registered in `SurvivalAssistantClient.registerHud`:
- `TaskHud` → `EnvironmentCard` → `TargetCard` stack in one screen corner and share position and width through one `HudLayout` instance each frame.
- `CrosshairInfo` finds the target (mobs up to `crosshairRange` with its own raycast; ores only within normal reach). It draws under the crosshair or hands its `Info` to `TargetCard`.
- `RecipeOverlay` uses the access widener (`survivalassistant.accesswidener`) for the container screen position.

**Settings screen.** `client/settings/` extends `OptionsSubScreen` with `MenuTabBar` tabs:
- `DisplayTab`, `GuideTab` and `WorldTab` build rows through the `Rows` helper, using vanilla `OptionInstance` buttons and sliders and a tooltip per option.
- Mod Menu is a `compileOnly` dependency: `ModMenuIntegration` is only loaded when Mod Menu is installed.

## Pitfalls in 26.3

- The names are Mojang's own and differ from older tutorials: `Minecraft.gui.setScreen(...)` and `gui.screen()`, `GuiGraphicsExtractor`, `extractRenderState`/`extractWidgetRenderState`, `Identifier`. When unsure, inspect the jars with `javap`. The jars are in `~/.gradle/caches/fabric-loom/minecraftMaven/net/minecraft/minecraft-{common,clientonly}-deobf/26.3/`.
- Never create `ItemStack`s in static fields or during client start-up: it crashes with "Components not bound yet". Use `ItemIcons.of(...)` lazily.
- A cycle-button `OptionInstance` puts the caption in front by itself. Its `CaptionBasedToString` must return only the value. Sliders must return the full label.

## Language files

**Never edit `src/main/resources/assets/survivalassistant/lang/*.json` by hand.** They are generated:
- English and Dutch come from the table `L` in `tools/make_lang.py`.
- The other 13 languages come from `tools/lang/<code>.json`. There, `~` is short for `survivalassistant.`.

Every language must have exactly the English keys and the same number of `%s` placeholders. Text may only use letters, digits, punctuation and a few extra characters; symbols must be drawn as icons. `make_lang.py` enforces this.

For bulk text changes, use a small Python script in the scratchpad that edits `make_lang.py` and `tools/lang/*.json`, then rerun `make_lang.py`. Labels on settings buttons must stay short, about 28 characters including the value, or they scroll.

## Releases

- The version is in `gradle.properties`. A published GitHub release (tag `vX.Y.Z`, jar attached) triggers `.github/workflows/publish.yml`, which publishes to Modrinth through `mc-publish`, with Mod Menu as an optional dependency.
- The project body on Modrinth is `docs/listing/description.md` without its leading HTML comment.
- Modrinth tokens last at most a week. Refresh the `MODRINTH_TOKEN` secret before each release, and never print or commit the token.

## Working with the maintainer

- Write to the maintainer in Dutch.
- Push, make releases and change Modrinth only after explicit approval.
- After code changes, close Minecraft, run `installToModrinth`, and let the maintainer test in the game before committing a release.
- The README has a section on how AI was used. Keep it honest and up to date when the way the mod is made changes.
