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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reads the mod's own language files on the server side. Translations are normally resolved on the client,
 * but the language model needs the actual text (biome facts, task names) in the player's language.
 */
public final class LangFile {
	private static final String FALLBACK = "en_us";
	/** The language files the mod ships (see tools/make_lang.py); any other code the client sends means English. */
	static final Set<String> SHIPPED = Set.of("ar_sa", "de_at", "de_ch", "de_de", "en_us", "es_ar", "es_cl", "es_ec",
			"es_es", "es_mx", "es_uy", "es_ve", "fr_ca", "fr_ch", "fr_fr", "hi_in", "id_id", "it_it", "nl_be", "nl_nl",
			"pl_pl", "pt_br", "pt_pt", "ro_ro", "ru_ru", "uk_ua", "zh_cn");
	private static final Map<String, Map<String, String>> CACHE = new ConcurrentHashMap<>();

	private LangFile() {}

	/** The translation in the given language, falling back to English; null when the key doesn't exist. */
	public static @Nullable String get(String languageCode, String key) {
		String code = languageCode.toLowerCase(Locale.ROOT);
		String value = has(code) ? load(code).get(key) : null;
		return value != null ? value : load(FALLBACK).get(key);
	}

	/** Whether the mod ships this language; otherwise the guide falls back to English. */
	public static boolean has(String languageCode) {
		return SHIPPED.contains(languageCode.toLowerCase(Locale.ROOT));
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
