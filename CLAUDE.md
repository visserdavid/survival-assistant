# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

Survival Assistant is a Fabric mod for Minecraft **26.3** (unobfuscated, Java 25, Fabric Loom 1.19), with a 26.2 build on a side branch. A guide NPC takes the player through 18 tutorial chapters. Around it: a task card, an environment card, mob/ore info, a recipe card, a light check, safety warnings and an optional small-talk feature that uses a local language model. The README is the user-facing documentation, including all config options and commands.

## Commands

The JDK is not on PATH. Set it first in Git Bash:

```bash
export JAVA_HOME="C:/Program Files/Microsoft/jdk-25.0.5.102-hotspot"
./gradlew build                      # compile, test, jar
./gradlew test --tests "io.github.visserdavid.survivalassistant.advice.GuideChoicesTest"   # one test class
./gradlew installToModrinth          # copy the jar into the Modrinth App instance "ModTesting" (close Minecraft first: the old jar is locked)
python tools/make_lang.py src/main/resources/assets/survivalassistant/lang   # regenerate + check all language files
python tools/recipe_check.py         # check every recipe card against the game data in the Minecraft jar
python tools/add_step_texts.py <texts.py>        # add the texts of new goal steps to all 15 languages (then make_lang)
python tools/prep_chapter.py Stappentest <goal> [--spawn] [--revoke <advancement>] [--route <id|free|none>] [--done 6,3]  # put a test world's guide at a goal (Minecraft closed; backups .before-prep)
python tools/set_gear.py Stappentest <goal>      # empty the inventory and give gear for that goal's test
python tools/test_schematics.py [name]          # write WorldEdit schematics for tests into ModTesting (//schem load <name>, //paste)
python tools/test_stair.py Stappentest [floorY]  # data pack that digs a 3x3 staircase from the player down (ores stay), once on world load
```

The world scripts take a world folder or the name of a world in the ModTesting instance, and need `nbtlib`. `set_gear.py` never gives the items a test is about, nor gear that completes the goal by itself (no netherite for `upgrade_template`); add a kit to its `KITS` for every new goal with steps.

- **Build output** lives in `%LOCALAPPDATA%/gradle-builds/survivalassistant`, not in `build/`, because OneDrive locks files there. Test results are in `test-results/test/*.xml` under that folder. The jars are also copied to the project root, where git ignores them.
- **CI** (`.github/workflows/build.yml`) runs the build. It also fails when `make_lang.py` changes any generated language file, and when `recipe_check.py` reports a mismatch.

## Architecture

**Source sets.** Loom uses split source sets:
- `src/main`: common and server code; the guide logic runs on the server.
- `src/client`: rendering, HUD, keys, the settings screen and skins.

**Server loop.** `GuideManager` is the hub, ticked from `SurvivalAssistant` (each player once a second, spread over the ticks). What players ask for (the `/guide` commands, the settings screen) lives in `GuideActions`. It delegates to `GuideSessions` (per-player runtime state), `GuideEntities` (the NPC), `GuideSpeech` (chat lines, sounds only to the owning player), `ChapterMenu` and `GuideNavigation` (death waypoint; the fallback home is a crafting table, chest or bed the player placed in the overworld). Each evaluation runs in this order:
1. `advice/ContextCollector` reads the world into an immutable `SurvivalContext`.
2. `AdviceEngine`/`TipRegistry` pick tips.
3. `GuideLifecycle` decides chapter flow (continue, congratulate, offer chapter, remind, farewell). Chapter days and
   the chapter-menu question count in **played time** (`GuideProgress.playedTicks`, raised once a second while the
   guide is active), not the world clock: a frozen daylight cycle or `/time set` must not break them.
4. `HudSnapshot` is built for the client.

**Pure logic, testable without Minecraft.** Most of `advice/` and `chat/` is plain Java:
- chapters and goals (`Chapters`, `Chapter`, `Recipes`, `Gear`, `OreTable`, `BiomeGroup`);
- goal steps (`Steps`: per goal an ordered list, the current step is the first that isn't done; never saved) and
  routes (`Routes`, `Route`: an order over the chapters; `GuideProgress.route` is `""` until the player answers);
- the lifecycle;
- crosshair advice;
- the small-talk prompt and scheduling.

Tests live under `src/test` and cover this layer. Tests must not touch `SurvivalAssistant` itself: loading that class registers the entity type and fails outside the game. For ids, use `Identifier.fromNamespaceAndPath(SurvivalAssistant.MOD_ID, …)`. `MOD_ID` is a compile-time constant, so using it does not load the class.

**State and sync.** Both use Fabric attachments in `state/`:
- `GuideState`: per-player `GuideProgress`, saved with the player through `GuideProgressCodec`.
  - **Never rename a codec key**: old worlds use them. Unknown keys are ignored; fields are lenient (a wrong type falls back to the default instead of losing all progress). Replaced keys stay readable (`asked_at` → `asked_played`).
  - DFU's `optionalFieldOf(name, default)` leaves out values that equal the default.
- `HudState`: a `HudSnapshot` synced to the owning client only. It is not saved and is sent only when it changed. Its stream codec lives in `HudCodec`, so it can be unit tested (registering the attachment needs the game).
- `SettingsPayloads`: custom payloads for the settings screen.
  - Request → Sync: the player's `GuideChoices`, the `ModConfig` as JSON, and `canEdit`.
  - ChoicesUpdate and ConfigUpdate back to the server.
  - `GuideSettings` handles them on the server (at most one change per second per player) and checks again that the sender is the singleplayer owner or an **admin** operator (level 3). Others never receive the config JSON, because it holds the chat endpoint.
  - Small talk: `chatEnabled` is the server's master switch; players can only switch it off for themselves.

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

- The version is in `gradle.properties`. A published GitHub release (tag `vX.Y.Z`, jar attached) triggers `.github/workflows/publish.yml`, which publishes to Modrinth through `mc-publish`, with Mod Menu as an optional dependency. It fails when the tag differs from `version` in `gradle.properties`, and it would also publish to CurseForge once `CURSEFORGE_ID`/`CURSEFORGE_TOKEN` are set.
- **26.2 build:** branch `mc-26.2` is `main` plus one commit: 26.2 versions in `gradle.properties` (`version=X+26.2`), `setInvulnerable` instead of `setPermanentlyInvulnerable` in `GuideEntity`, and `Util.getPlatform().openPath` instead of `Blaze3D.openPath` in `DisplayTab` (no method exists in both versions, so there is no shared jar). For a release, rebase it onto `main`, build, test in the Modrinth instance "ModTesting 26.2" (`-PmodrinthInstance`), and publish as tag `vX+26.2` with `--target mc-26.2 --latest=false`; `publish.yml` needs no change.
- The project body on Modrinth is `docs/listing/description.md` without its leading HTML comment.
- Modrinth tokens last at most a week. Refresh the `MODRINTH_TOKEN` secret before each release, and never print or commit the token.

## Working with the maintainer

- Write to the maintainer in Dutch.
- Push, make releases and change Modrinth only after explicit approval.
- After code changes, close Minecraft, run `installToModrinth`, and let the maintainer test in the game before committing a release.
- The README has a section on how AI was used. Keep it honest and up to date when the way the mod is made changes.
