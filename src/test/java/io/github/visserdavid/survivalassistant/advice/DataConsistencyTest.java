package io.github.visserdavid.survivalassistant.advice;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The tips, chapters, recipes, biomes, language file and task card icons all fit together. */
class DataConsistencyTest {
	private static final String P = "survivalassistant.";
	private static JsonObject english;

	@BeforeAll
	static void readEnglish() throws IOException {
		try (InputStream in = DataConsistencyTest.class.getClassLoader()
				.getResourceAsStream("assets/survivalassistant/lang/en_us.json")) {
			assertNotNull(in, "en_us.json on the test classpath");
			english = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
		}
	}

	/** Every tip: the urgent ones and the goals of every chapter. */
	private static List<Tip> allTips() {
		List<Tip> tips = new ArrayList<>(TipRegistry.URGENT);
		for (Chapter c : Chapters.ALL) tips.addAll(c.goals());
		return tips;
	}

	private static void hasKey(String key) {
		assertTrue(english.has(key), "missing in en_us.json: " + key);
	}

	@Test
	void tipIdsAreUnique() {
		Set<String> seen = new HashSet<>();
		for (Tip t : allTips()) assertTrue(seen.add(t.id()), "duplicate tip id " + t.id());
	}

	@Test
	void everyTipHasItsTexts() {
		for (Tip t : allTips()) {
			for (int v = 1; v <= t.variants(); v++) hasKey(t.translationKey(v));
			if (t.kind() == Tip.Kind.MILESTONE) {
				hasKey(P + "task." + t.id());
				hasKey(P + "praise." + t.id());
			} else {
				hasKey(P + "alert." + t.id());
			}
		}
		hasKey(P + "alert." + SafeSignal.ID);
	}

	@Test
	void everyChapterHasItsTexts() {
		for (Chapter c : Chapters.ALL) {
			hasKey(c.titleKey());
			hasKey(P + "chapter." + c.id() + ".desc");
			hasKey(P + "chapter." + c.id() + ".start");
		}
	}

	@Test
	void everyBiomeGroupHasAProAndACon() {
		for (BiomeGroup g : BiomeGroup.values()) {
			hasKey(P + "biome." + g.key() + ".pro");
			hasKey(P + "biome." + g.key() + ".con");
		}
	}

	@Test
	void everyRecipeBelongsToAGoalOrStep() {
		Set<String> tasks = new HashSet<>();
		for (Chapter c : Chapters.ALL) for (Tip t : c.goals()) tasks.add(t.id());
		for (Steps.Step s : Steps.all()) tasks.add(s.id());
		for (String task : Recipes.BY_TASK.keySet()) assertTrue(tasks.contains(task), "recipe for unknown task " + task);
	}

	@Test
	void everyStepBelongsToAGoalAndHasItsTexts() {
		Set<String> goals = new HashSet<>();
		for (Chapter c : Chapters.ALL) for (Tip t : c.goals()) goals.add(t.id());
		Set<String> seen = new HashSet<>();
		Steps.BY_GOAL.forEach((goal, steps) -> {
			assertTrue(goals.contains(goal), "steps for unknown goal " + goal);
			assertTrue(steps.size() >= 2, "a single step adds nothing: " + goal);
			for (Steps.Step s : steps) {
				assertTrue(s.id().startsWith(goal + "."), "step id must start with its goal: " + s.id());
				assertEquals(goal, s.goal());
				assertTrue(seen.add(s.id()), "duplicate step id " + s.id());
				hasKey(s.labelKey());
				hasKey(s.tipKey());
			}
		});
		hasKey(P + "settings.show_steps");
		hasKey(P + "settings.show_steps.tip");
	}

	@Test
	void everyTipHasATaskCardIcon() throws IOException {
		// TaskIcons lives in the client source set, which the tests can't load: read its source instead.
		Path source = Path.of("src/client/java/io/github/visserdavid/survivalassistant/client/hud/TaskIcons.java");
		String text = Files.readString(source, StandardCharsets.UTF_8);
		Set<String> icons = new HashSet<>();
		Matcher m = Pattern.compile("put\\(\"([a-z0-9_]+)\"").matcher(text);
		while (m.find()) icons.add(m.group(1));
		assertTrue(text.contains("put(SafeSignal.ID,"), "icon for the safe signal");

		List<String> missing = new ArrayList<>();
		for (Tip t : allTips()) if (!icons.contains(t.id())) missing.add(t.id());
		assertEquals(List.of(), missing, "tips without an icon in TaskIcons");
	}
}
