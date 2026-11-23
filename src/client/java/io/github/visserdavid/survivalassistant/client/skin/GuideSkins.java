package io.github.visserdavid.survivalassistant.client.skin;

import com.mojang.blaze3d.platform.NativeImage;
import io.github.visserdavid.survivalassistant.SurvivalAssistant;
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

/**
 * Custom guide skins from {@code config/survivalassistant/skins/}. Client only: every player picks the look of
 * the guides they see. The folder is read once the client has started (see {@link #reload()}) and again with
 * {@code /guideskin reload}.
 */
public final class GuideSkins {
	private GuideSkins() {}

	public static final Identifier DEFAULT_TEXTURE = SurvivalAssistant.id("textures/entity/guide.png");

	/** A skin to draw: texture and arm model. */
	public record Skin(Identifier texture, boolean slim) {}

	private static final Skin DEFAULT = new Skin(DEFAULT_TEXTURE, false);
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

	/** The skin chosen in the client config, or the built-in one when it isn't available. */
	public static Skin current() {
		String chosen = ClientConfig.get().guideSkin;
		if (chosen == null || chosen.equals(SkinNames.DEFAULT)) return DEFAULT;
		return SKINS.getOrDefault(chosen, DEFAULT);
	}

	/** True when a custom skin is chosen but not found (the guide then falls back to the built-in skin). */
	public static boolean chosenMissing() {
		String chosen = ClientConfig.get().guideSkin;
		return chosen != null && !chosen.equals(SkinNames.DEFAULT) && !SKINS.containsKey(chosen);
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
