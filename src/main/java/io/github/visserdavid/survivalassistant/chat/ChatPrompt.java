package io.github.visserdavid.survivalassistant.chat;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Builds the request for the language model and cleans up its reply. Pure logic: no Minecraft, no I/O.
 *
 * <p>The model only makes small talk. Game facts (recipes, ore heights, what to do next) stay with the advice engine,
 * because a small model will happily invent them. The only facts it gets are the checked biome texts from the
 * language files.
 */
public final class ChatPrompt {
	public enum Role { SYSTEM, USER, ASSISTANT }

	public record Turn(Role role, String text) {}

	/**
	 * Everything the model needs to know about this moment.
	 *
	 * @param languageCode Minecraft language code of the player, for example {@code nl_nl}
	 * @param biomePro     checked advantage of the biome (from the language file), or null
	 * @param biomeCon     checked disadvantage of the biome, or null
	 * @param currentTask  short label of the goal the player is working on, or null
	 */
	public record Scene(String languageCode, String guideName, String playerName, Surroundings surroundings,
			@Nullable String biomePro, @Nullable String biomeCon, @Nullable String currentTask) {}

	/** Longest reply shown in chat. */
	public static final int MAX_REPLY_CHARS = 260;

	private ChatPrompt() {}

	// ---------------------------------------------------------------- building

	/** Messages for a spontaneous remark about a change. */
	public static List<Turn> forChange(Scene scene, List<Turn> history, ChangeDetector.Change change) {
		return messages(scene, history, changeNote(scene, change));
	}

	/** Messages for an answer to something the player said. */
	public static List<Turn> forPlayer(Scene scene, List<Turn> history, String playerText) {
		return messages(scene, history, playerText.strip());
	}

	/** The user line that describes a change; stored in the history so roles keep alternating. */
	public static String changeNote(Scene scene, ChangeDetector.Change change) {
		Surroundings s = scene.surroundings();
		String what = switch (change.kind()) {
			case THUNDER_START -> "A thunderstorm just started.";
			case SUNRISE -> "The sun just came up: the player made it through the night.";
			case RAIN_START -> "It just started raining.";
			case RAIN_STOP -> "The rain just stopped.";
			case NIGHTFALL -> "Night just fell.";
			case ENTER_CAVE -> "The player just went underground, into a cave.";
			case LEAVE_CAVE -> "The player just came back up to the surface.";
			case NEW_BIOME -> "The player is now in a new biome: " + readable(s.biomeName()) + ".";
			case NEW_MOB -> {
				boolean hostile = s.hostile().containsKey(change.subject());
				int count = hostile ? s.hostile().getOrDefault(change.subject(), 1) : s.friendly().getOrDefault(change.subject(), 1);
				yield "The player now sees " + (hostile ? "a monster: " : "") + readable(change.subject())
						+ (count > 1 ? " (" + count + ")" : "") + ".";
			}
		};
		return "[" + what + " Make one short, spontaneous remark about it to " + scene.playerName() + ".]";
	}

	/** The user line for a musing: the idea comes from the handwritten (checked) English text. */
	public static String museNote(Scene scene, String idea) {
		return "[You feel like sharing a thought about this place. Idea: " + idea + " Put it in your own words in one or "
				+ "two playful sentences, or turn it into a light question for " + scene.playerName() + ".]";
	}

	private static List<Turn> messages(Scene scene, List<Turn> history, String lastUserText) {
		List<Turn> out = new ArrayList<>();
		out.add(new Turn(Role.SYSTEM, systemPrompt(scene)));
		// Many chat templates (Gemma, for one) demand strictly alternating user/assistant turns starting with user.
		for (Turn t : history) {
			if (t.role() == Role.SYSTEM) continue;
			if (out.size() == 1 && t.role() == Role.ASSISTANT) continue;
			append(out, t);
		}
		append(out, new Turn(Role.USER, lastUserText));
		return out;
	}

	/** Adds a turn, merging it into the previous one when the role repeats. */
	private static void append(List<Turn> out, Turn turn) {
		Turn last = out.get(out.size() - 1);
		if (last.role() == turn.role()) {
			out.set(out.size() - 1, new Turn(turn.role(), last.text() + "\n" + turn.text()));
		} else {
			out.add(turn);
		}
	}

	public static String systemPrompt(Scene scene) {
		Surroundings s = scene.surroundings();
		StringBuilder b = new StringBuilder();
		b.append("You are ").append(scene.guideName()).append(", a friendly companion who walks along with ")
				.append(scene.playerName()).append(" in the game Minecraft. You make small talk: a casual remark, ")
				.append("a light joke, a curious question. Nothing deep.\n\n")
				.append("Rules:\n")
				.append("- Always reply in ").append(languageName(scene.languageCode())).append(".\n")
				.append("- Vary your wording: don't repeat phrases or openings you already used in this conversation.\n")
				.append("- At most two short sentences, like a chat message. No lists, no emoji, no quotation marks, ")
				.append("no actions between asterisks.\n")
				.append("- Talk to the player directly. Never put your own name in front of your reply.\n")
				.append("- Never give recipes, ore heights, numbers or step-by-step survival instructions: you might be wrong. ")
				.append("If the player asks for that, say the task card or a right-click on you will help.\n")
				.append("- You can only talk. You can't give items, fight or build.\n")
				.append("- If you don't know something, say so cheerfully.\n\n")
				.append("What you see right now:\n")
				.append("- Biome: ").append(readable(s.biomeName())).append('\n');
		if (scene.biomePro() != null || scene.biomeCon() != null) {
			b.append("- About this biome (true facts you may use): ");
			if (scene.biomePro() != null) b.append(scene.biomePro());
			if (scene.biomePro() != null && scene.biomeCon() != null) b.append("; ");
			if (scene.biomeCon() != null) b.append(scene.biomeCon());
			b.append('\n');
		}
		b.append("- Weather: ").append(s.thundering() ? "thunderstorm" : s.raining() ? "rain" : "dry").append('\n')
				.append("- Time: ").append(s.phase().name().toLowerCase(Locale.ROOT)).append('\n')
				.append("- Place: ").append(s.underground() ? "underground, in a cave" : "outside or near the surface").append('\n')
				.append("- Animals and other creatures nearby: ").append(list(s.friendly())).append('\n')
				.append("- Monsters nearby: ").append(list(s.hostile())).append('\n');
		if (scene.currentTask() != null) {
			b.append("- The player is working on: ").append(scene.currentTask()).append('\n');
		}
		return b.toString();
	}

	/** "3 cow, 1 fox", most numerous first; "none" when empty. */
	static String list(Map<String, Integer> mobs) {
		if (mobs.isEmpty()) return "none";
		StringBuilder b = new StringBuilder();
		mobs.entrySet().stream()
				.sorted(Map.Entry.<String, Integer>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
				.limit(6)
				.forEach(e -> {
					if (!b.isEmpty()) b.append(", ");
					b.append(e.getValue()).append(' ').append(readable(e.getKey()));
				});
		return b.toString();
	}

	static String readable(String id) {
		return id.replace('_', ' ');
	}

	static String languageName(String code) {
		String lang = code.toLowerCase(Locale.ROOT);
		int sep = lang.indexOf('_');
		if (sep > 0) lang = lang.substring(0, sep);
		String name = Locale.forLanguageTag(lang).getDisplayLanguage(Locale.ENGLISH);
		return name.isEmpty() ? "English" : name;
	}

	// ---------------------------------------------------------------- JSON

	/** Request body for an OpenAI-compatible {@code /v1/chat/completions} endpoint such as llama-server. */
	/** @param temperature sent along when 0 or higher; below 0 the server's own setting is used */
	public static String toRequestJson(List<Turn> messages, String model, int maxTokens, double temperature) {
		StringBuilder b = new StringBuilder("{\"model\":").append(quote(model))
				.append(",\"max_tokens\":").append(maxTokens)
				.append(temperature >= 0 ? ",\"temperature\":" + String.format(Locale.ROOT, "%.2f", temperature) : "")
				.append(",\"stream\":false")
				// Reasoning models (Qwen and others) would otherwise think first: slow and pointless for small talk.
				.append(",\"chat_template_kwargs\":{\"enable_thinking\":false}")
				.append(",\"messages\":[");
		for (int i = 0; i < messages.size(); i++) {
			Turn t = messages.get(i);
			if (i > 0) b.append(',');
			b.append("{\"role\":").append(quote(t.role().name().toLowerCase(Locale.ROOT)))
					.append(",\"content\":").append(quote(t.text())).append('}');
		}
		return b.append("]}").toString();
	}

	static String quote(String s) {
		StringBuilder b = new StringBuilder(s.length() + 2).append('"');
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			switch (c) {
				case '"' -> b.append("\\\"");
				case '\\' -> b.append("\\\\");
				case '\n' -> b.append("\\n");
				case '\r' -> b.append("\\r");
				case '\t' -> b.append("\\t");
				default -> {
					if (c < 0x20) b.append(String.format("\\u%04x", (int) c));
					else b.append(c);
				}
			}
		}
		return b.append('"').toString();
	}

	// ---------------------------------------------------------------- cleaning the reply

	private static final Pattern THINK = Pattern.compile("(?s)<think>.*?(</think>|$)");
	private static final Pattern ACTIONS = Pattern.compile("\\*[^*]{1,60}\\*");
	private static final Pattern SENTENCE_END = Pattern.compile("[.!?](?=\\s|$)");

	/** Makes the raw model output fit for one chat line; empty when nothing usable is left. */
	public static String cleanReply(@Nullable String raw, String guideName) {
		if (raw == null) return "";
		String s = THINK.matcher(raw).replaceAll("");
		s = ACTIONS.matcher(s).replaceAll("");
		s = stripSymbols(s);
		s = s.replaceAll("\\s+", " ").strip();
		String prefix = guideName + ":";
		if (s.regionMatches(true, 0, prefix, 0, prefix.length())) s = s.substring(prefix.length()).strip();
		s = stripQuotes(s);
		return shorten(s);
	}

	/**
	 * Removes emoji and other pictographs: Minecraft's font has no glyphs for them, so they would show up as boxes.
	 * Letters, digits, punctuation and spaces of every script stay.
	 */
	static String stripSymbols(String s) {
		StringBuilder b = new StringBuilder(s.length());
		s.codePoints().forEach(cp -> {
			int type = Character.getType(cp);
			boolean symbol = type == Character.OTHER_SYMBOL || type == Character.SURROGATE || type == Character.PRIVATE_USE
					|| type == Character.UNASSIGNED
					|| cp == 0x200D // zero-width joiner (glues emoji together)
					|| (cp >= 0xFE00 && cp <= 0xFE0F) // variation selectors (emoji presentation)
					|| (cp >= 0x1F3FB && cp <= 0x1F3FF); // skin tone modifiers
			if (!symbol) b.appendCodePoint(cp);
		});
		return b.toString();
	}

	private static String stripQuotes(String s) {
		if (s.length() >= 2) {
			char a = s.charAt(0), z = s.charAt(s.length() - 1);
			if ((a == '"' && z == '"') || (a == '“' && z == '”') || (a == '„' && z == '”')) {
				return s.substring(1, s.length() - 1).strip();
			}
		}
		return s;
	}

	/** At most two sentences and {@link #MAX_REPLY_CHARS} characters, cut at a sentence or word boundary. */
	static String shorten(String s) {
		var m = SENTENCE_END.matcher(s);
		int sentences = 0;
		while (m.find()) {
			if (++sentences == 2) {
				s = s.substring(0, m.end());
				break;
			}
		}
		if (s.length() <= MAX_REPLY_CHARS) return s;
		String cut = s.substring(0, MAX_REPLY_CHARS);
		int lastEnd = -1;
		var m2 = SENTENCE_END.matcher(cut);
		while (m2.find()) lastEnd = m2.end();
		if (lastEnd > MAX_REPLY_CHARS / 2) return cut.substring(0, lastEnd);
		int space = cut.lastIndexOf(' ');
		return (space > 0 ? cut.substring(0, space) : cut) + "...";
	}

	/** Keeps the history short: the last {@code maxTurns} user/assistant pairs. */
	public static void trimHistory(List<Turn> history, int maxTurns) {
		int max = Math.max(0, maxTurns) * 2;
		while (history.size() > max) history.remove(0);
		while (!history.isEmpty() && history.get(0).role() != Role.USER) history.remove(0);
	}

}
