package io.github.visserdavid.survivalassistant.chat;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ChatPromptTest {
	private static final Surroundings FOREST = new Surroundings("minecraft:dark_forest", "dark_forest", true, false,
			Surroundings.Phase.DUSK, false, Map.of("cow", 3, "fox", 1), Map.of("zombie", 1));

	private static ChatPrompt.Scene scene() {
		return new ChatPrompt.Scene("nl_nl", "Gids", "Steve", FOREST,
				"Enorme donkere eiken", "Monsters ook overdag", "Hak hout");
	}

	@Test
	void systemPromptDescribesTheScene() {
		String p = ChatPrompt.systemPrompt(scene());
		assertTrue(p.contains("reply in Dutch"), p);
		assertTrue(p.contains("Biome: dark forest"));
		assertTrue(p.contains("Enorme donkere eiken; Monsters ook overdag"));
		assertTrue(p.contains("Weather: rain"));
		assertTrue(p.contains("Time: dusk"));
		assertTrue(p.contains("3 cow, 1 fox"), "most numerous first");
		assertTrue(p.contains("Monsters nearby: 1 zombie"));
		assertTrue(p.contains("working on: Hak hout"));
	}

	@Test
	void languageNames() {
		assertEquals("Dutch", ChatPrompt.languageName("nl_nl"));
		assertEquals("English", ChatPrompt.languageName("en_us"));
		assertEquals("German", ChatPrompt.languageName("de_de"));
	}

	@Test
	void changeNoteNamesTheMob() {
		String note = ChatPrompt.changeNote(scene(), new ChangeDetector.Change(ChangeDetector.Kind.NEW_MOB, "cow"));
		assertTrue(note.contains("sees cow (3)"), note);
		String monster = ChatPrompt.changeNote(scene(), new ChangeDetector.Change(ChangeDetector.Kind.NEW_MOB, "zombie"));
		assertTrue(monster.contains("a monster: zombie"), monster);
	}

	@Test
	void rolesAlternateStartingWithUser() {
		List<ChatPrompt.Turn> history = List.of(
				new ChatPrompt.Turn(ChatPrompt.Role.ASSISTANT, "orphan"),
				new ChatPrompt.Turn(ChatPrompt.Role.USER, "hoi"),
				new ChatPrompt.Turn(ChatPrompt.Role.ASSISTANT, "hallo!"),
				new ChatPrompt.Turn(ChatPrompt.Role.USER, "nog eentje"));
		List<ChatPrompt.Turn> m = ChatPrompt.forPlayer(scene(), history, "  mooi weer hè  ");
		assertEquals(ChatPrompt.Role.SYSTEM, m.get(0).role());
		assertEquals(ChatPrompt.Role.USER, m.get(1).role(), "an assistant turn can't come first");
		for (int i = 2; i < m.size(); i++) assertNotEquals(m.get(i - 1).role(), m.get(i).role(), "roles alternate");
		ChatPrompt.Turn last = m.get(m.size() - 1);
		assertEquals("nog eentje\nmooi weer hè", last.text(), "repeated user turns are merged");
	}

	@Test
	void requestJsonIsEscaped() {
		String json = ChatPrompt.toRequestJson(List.of(new ChatPrompt.Turn(ChatPrompt.Role.USER, "zei \"hoi\"\nen\\dan")),
				"local", 80, 0.8);
		assertTrue(json.contains("\"content\":\"zei \\\"hoi\\\"\\nen\\\\dan\""), json);
		assertTrue(json.contains("\"temperature\":0.80"));
		assertTrue(json.contains("\"enable_thinking\":false"));
	}

	@Test
	void cleanReplyStripsNoise() {
		assertEquals("Wat een bui, hè?", ChatPrompt.cleanReply("<think>hmm</think>\n Gids: \"Wat een bui, hè?\"", "Gids"));
		assertEquals("Kijk, koeien!", ChatPrompt.cleanReply("*zwaait* Kijk, koeien!", "Gids"));
		assertEquals("Een. Twee!", ChatPrompt.cleanReply("Een. Twee! Drie. Vier.", "Gids"), "at most two sentences");
		assertEquals("", ChatPrompt.cleanReply("<think>never closed", "Gids"));
		assertEquals("", ChatPrompt.cleanReply(null, "Gids"));
	}

	@Test
	void longRepliesAreCut() {
		String reply = ChatPrompt.cleanReply("woord ".repeat(100), "Gids");
		assertTrue(reply.length() <= ChatPrompt.MAX_REPLY_CHARS + 3, reply);
		assertTrue(reply.endsWith("..."));
	}

	@Test
	void historyIsTrimmedToPairs() {
		List<ChatPrompt.Turn> h = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			h.add(new ChatPrompt.Turn(ChatPrompt.Role.USER, "u" + i));
			h.add(new ChatPrompt.Turn(ChatPrompt.Role.ASSISTANT, "a" + i));
		}
		ChatPrompt.trimHistory(h, 3);
		assertEquals(6, h.size());
		assertEquals("u7", h.get(0).text());
	}
}
