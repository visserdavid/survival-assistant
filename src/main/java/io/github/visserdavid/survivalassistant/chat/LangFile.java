package io.github.visserdavid.survivalassistant.chat;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.visserdavid.survivalassistant.SurvivalAssistant;
import org.jspecify.annotations.Nullable;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reads the mod's own language files on the server side. Translations are normally resolved on the client,
 * but the language model needs the actual text (biome facts, task names) in the player's language.
 */
public final class LangFile {
	private static final String FALLBACK = "en_us";
	private static final Map<String, Map<String, String>> CACHE = new ConcurrentHashMap<>();

	private LangFile() {}

	/** The translation in the given language, falling back to English; null when the key doesn't exist. */
	public static @Nullable String get(String languageCode, String key) {
		String value = load(languageCode.toLowerCase(Locale.ROOT)).get(key);
		return value != null ? value : load(FALLBACK).get(key);
	}

	/** Whether the mod ships this language; otherwise the guide falls back to English. */
	public static boolean has(String languageCode) {
		return !load(languageCode.toLowerCase(Locale.ROOT)).isEmpty();
	}

	private static Map<String, String> load(String code) {
		return CACHE.computeIfAbsent(code, LangFile::read);
	}

	private static Map<String, String> read(String code) {
		Map<String, String> out = new HashMap<>();
		String path = "/assets/" + SurvivalAssistant.MOD_ID + "/lang/" + code + ".json";
		try (InputStream in = SurvivalAssistant.class.getResourceAsStream(path)) {
			if (in == null) return out;
			try (Reader r = new InputStreamReader(in, StandardCharsets.UTF_8)) {
				JsonObject obj = JsonParser.parseReader(r).getAsJsonObject();
				for (Map.Entry<String, JsonElement> e : obj.entrySet()) {
					if (e.getValue().isJsonPrimitive()) out.put(e.getKey(), e.getValue().getAsString());
				}
			}
		} catch (Exception e) {
			SurvivalAssistant.LOGGER.warn("Could not read language file {}", path, e);
		}
		return out;
	}
}
