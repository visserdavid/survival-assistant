package io.github.visserdavid.survivalassistant.chat;

import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

class ChatClientTest {
	@Test
	void readsTheFirstAnswer() {
		assertEquals("Hello!", ChatClient.parse("{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"Hello!\"}}]}"));
	}

	@Test
	void strangeAnswersAreEmpty() {
		assertEquals("", ChatClient.parse("{}"));
		assertEquals("", ChatClient.parse("{\"choices\":[]}"));
		assertEquals("", ChatClient.parse("{\"choices\":[{}]}"));
		assertEquals("", ChatClient.parse("{\"choices\":[{\"message\":{\"content\":null}}]}"));
		assertEquals("", ChatClient.parse("{\"choices\":[{\"message\":{\"content\":{\"a\":1}}}]}"));
		assertEquals("", ChatClient.parse("[1,2]"));
		assertEquals("", ChatClient.parse("not json {"));
		assertEquals("", ChatClient.parse(""));
	}

	@Test
	void badEndpointFailsTheFutureInsteadOfThrowing() {
		var future = ChatClient.complete("not a uri with spaces", "{}", 1);
		ExecutionException e = assertThrows(ExecutionException.class, future::get);
		assertInstanceOf(ChatClient.BadResponseException.class, e.getCause());
	}
}
