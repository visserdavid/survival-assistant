package io.github.visserdavid.survivalassistant;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Reader;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/** Every shipped language file must match English: same keys, same number of %s placeholders. */
class LangFilesTest {
	private static final Pattern PLACEHOLDER = Pattern.compile("%(?:\\d+\\$)?s");

	private static Path langDir() throws URISyntaxException {
		var url = LangFilesTest.class.getClassLoader().getResource("assets/survivalassistant/lang/en_us.json");
		assertNotNull(url, "en_us.json on the test classpath");
		return Path.of(url.toURI()).getParent();
	}

	private static JsonObject read(Path file) throws IOException {
		try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
			return JsonParser.parseReader(r).getAsJsonObject();
		}
	}

	private static int placeholders(String s) {
		Matcher m = PLACEHOLDER.matcher(s);
		int n = 0;
		while (m.find()) n++;
		return n;
	}

	@Test
	void allLanguagesMatchEnglish() throws Exception {
		Path dir = langDir();
		JsonObject english = read(dir.resolve("en_us.json"));
		List<Path> files;
		try (Stream<Path> s = Files.list(dir)) {
			files = s.filter(p -> p.toString().endsWith(".json")).toList();
		}
		assertTrue(files.size() >= 15, "15 languages plus regional copies, found " + files.size());

		for (Path file : files) {
			JsonObject lang = read(file);
			String name = file.getFileName().toString();
			assertEquals(english.keySet(), lang.keySet(), name + " has different keys");
			for (Map.Entry<String, JsonElement> e : english.entrySet()) {
				String text = lang.get(e.getKey()).getAsString();
				assertFalse(text.isBlank(), name + " " + e.getKey() + " is empty");
				assertEquals(placeholders(e.getValue().getAsString()), placeholders(text),
						name + " " + e.getKey() + " placeholder count: " + text);
			}
		}
	}
}
