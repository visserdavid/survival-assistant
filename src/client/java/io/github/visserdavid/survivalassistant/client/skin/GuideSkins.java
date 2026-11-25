package io.github.visserdavid.survivalassistant.client.skin;

import com.mojang.blaze3d.platform.NativeImage;
import io.github.visserdavid.survivalassistant.SurvivalAssistant;
import io.github.visserdavid.survivalassistant.advice.Route;
import io.github.visserdavid.survivalassistant.advice.Routes;
import io.github.visserdavid.survivalassistant.client.hud.ClientConfig;
import io.github.visserdavid.survivalassistant.skin.SkinNames;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Custom guide skins from {@code config/survivalassistant/skins/}. Client only: every player picks the look of
 * the guides they see. The folder is read once the client has started (see {@link #reload()}) and again with
 * {@code /guideskin reload}.
 */
public final class GuideSkins {
	private GuideSkins() {}

	public static final Identifier DEFAULT_TEXTURE = SurvivalAssistant.id("textures/entity/guide.png");
	public static final Identifier CLASSIC_TEXTURE = SurvivalAssistant.id("textures/entity/guide_classic.png");
	/** Only the eyes of the keeper skins (default and route outfits), drawn full-bright by {@code GuideEyesLayer}. */
	public static final Identifier EYES_TEXTURE = SurvivalAssistant.id("textures/entity/guide_eyes.png");

	/** A skin to draw: texture and arm model. */
	public record Skin(Identifier texture, boolean slim) {}

	private static final Skin DEFAULT = new Skin(DEFAULT_TEXTURE, false);
	private static final Skin CLASSIC = new Skin(CLASSIC_TEXTURE, false);
	/** The keeper's outfit per route: textures/entity/guide_&lt;route&gt;.png (drawn by tools/make_guide_skin.py). */
	private static final Map<String, Skin> ROUTE_SKINS = Routes.ALL.stream().collect(Collectors.toUnmodifiableMap(Route::id,
			r -> new Skin(SurvivalAssistant.id("textures/entity/guide_" + r.id() + ".png"), false)));
	private static final Map<String, Skin> SKINS = new TreeMap<>();
	private static final List<String> PROBLEMS = new ArrayList<>();

	public static Path folder() {
		return FabricLoader.getInstance().getConfigDir().resolve(SurvivalAssistant.MOD_ID).resolve("skins");
	}

	/** Creates the folder with a short README (safe to call before the game window exists). */
	public static void init() {
		Path dir = folder();
		try {
			Files.createDirectories(dir);
			Path readme = dir.resolve("README.txt");
			if (!Files.exists(readme)) {
				Files.writeString(readme, """
						Survival Assistant - guide skins

						Put Minecraft skins (PNG, 64x64) in this folder, then in game:
						  /guideskin reload       read this folder again
						  /guideskin list         show the skins found here
						  /guideskin set <name>   use a skin (the file name without .png)
						  /guideskin set default  back to the built-in guide

						End the file name with _slim (for example elf_slim.png) for skins with slim (Alex) arms.
						Only you see your choice; other players see their own.
						""");
			}
		} catch (IOException e) {
			SurvivalAssistant.LOGGER.warn("Could not create the guide skins folder {}", dir, e);
		}
	}

	/**
	 * The skin for a guide whose owner follows {@code route} ("" for none): the skin chosen in the client config, or
	 * with "default" (and when the chosen skin isn't available) the keeper, dressed for the route.
	 */
	public static Skin current(String route) {
		String chosen = ClientConfig.get().guideSkin;
		if (SkinNames.CLASSIC.equals(chosen)) return CLASSIC;
		Skin own = chosen == null ? null : SKINS.get(chosen);
		if (own != null) return own;
		return ROUTE_SKINS.getOrDefault(route, DEFAULT);
	}

	/** True when the keeper is shown (default or a route outfit): only he has glowing eyes. */
	public static boolean keeperShown(String route) {
		Skin skin = current(route);
		return skin == DEFAULT || ROUTE_SKINS.containsValue(skin);
	}

	/** The built-in skins, then the ones from the folder: everything {@code /guideskin set} accepts. */
	public static List<String> choices() {
		List<String> all = new ArrayList<>(SkinNames.BUILT_IN);
		all.addAll(SKINS.keySet());
		return all;
	}

	/** True when a custom skin is chosen but not found (the guide then falls back to the built-in skin). */
	public static boolean chosenMissing() {
		String chosen = ClientConfig.get().guideSkin;
		return chosen != null && !SkinNames.BUILT_IN.contains(chosen) && !SKINS.containsKey(chosen);
	}

	/** Names of the usable skins, sorted. */
	public static List<String> names() {
		return List.copyOf(SKINS.keySet());
	}

	/** Files that were skipped during the last reload (wrong size or unreadable). */
	public static List<String> problems() {
		return List.copyOf(PROBLEMS);
	}

	/** Reads the folder again. Must run on the render (client main) thread. */
	public static void reload() {
		Minecraft mc = Minecraft.getInstance();
		for (Skin s : SKINS.values()) mc.getTextureManager().release(s.texture());
		SKINS.clear();
		PROBLEMS.clear();

		Path dir = folder();
		if (!Files.isDirectory(dir)) return;
		try (DirectoryStream<Path> files = Files.newDirectoryStream(dir, "*.{png,PNG}")) {
			for (Path file : files) {
				String fileName = file.getFileName().toString();
				SkinNames.Skin skin = SkinNames.parse(fileName).orElse(null);
				if (skin == null || SKINS.containsKey(skin.name())) continue;
				load(mc, file, fileName, skin);
			}
		} catch (IOException e) {
			SurvivalAssistant.LOGGER.warn("Could not read the guide skins folder {}", dir, e);
		}
	}

	private static void load(Minecraft mc, Path file, String fileName, SkinNames.Skin skin) {
		NativeImage image;
		try (InputStream in = Files.newInputStream(file)) {
			image = NativeImage.read(in);
		} catch (IOException | RuntimeException e) {
			PROBLEMS.add(fileName);
			SurvivalAssistant.LOGGER.warn("Could not read guide skin {}", file, e);
			return;
		}
		if (!SkinNames.validSize(image.getWidth(), image.getHeight())) {
			PROBLEMS.add(fileName + " (" + image.getWidth() + "x" + image.getHeight() + ")");
			image.close();
			return;
		}
		Identifier id = SurvivalAssistant.id(skin.texturePath());
		mc.getTextureManager().register(id, new DynamicTexture(() -> "guide skin " + skin.name(), image));
		SKINS.put(skin.name(), new Skin(id, skin.slim()));
	}
}
