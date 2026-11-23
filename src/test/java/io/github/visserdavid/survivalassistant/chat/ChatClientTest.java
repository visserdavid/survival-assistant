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

	/** A tiny local server that answers every request with the given status and body. */
	private static com.sun.net.httpserver.HttpServer serve(int status, byte[] body) throws java.io.IOException {
		var server = com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/", exchange -> {
			exchange.getRequestBody().readAllBytes();
			exchange.sendResponseHeaders(status, body.length);
			try (var out = exchange.getResponseBody()) {
				out.write(body);
			}
		});
		server.start();
		return server;
	}

	private static String url(com.sun.net.httpserver.HttpServer server) {
		return "http://127.0.0.1:" + server.getAddress().getPort() + "/v1/chat/completions";
	}

	@Test
	void aGoodAnswerArrives() throws Exception {
		var server = serve(200, "{\"choices\":[{\"message\":{\"content\":\"Hi there\"}}]}".getBytes());
		try {
			assertEquals("Hi there", ChatClient.complete(url(server), "{}", 5).get());
		} finally {
			server.stop(0);
		}
	}

	@Test
	void anHttpErrorIsABadResponse() throws Exception {
		var server = serve(500, "model not loaded".getBytes());
		try {
			ExecutionException e = assertThrows(ExecutionException.class, () -> ChatClient.complete(url(server), "{}", 5).get());
			assertInstanceOf(ChatClient.BadResponseException.class, e.getCause());
			assertTrue(e.getCause().getMessage().contains("500"));
		} finally {
			server.stop(0);
		}
	}

	@Test
	void aHugeAnswerIsRefusedWithoutReadingItAll() throws Exception {
		var server = serve(200, new byte[2 * 1024 * 1024]);
		try {
			ExecutionException e = assertThrows(ExecutionException.class, () -> ChatClient.complete(url(server), "{}", 5).get());
			assertInstanceOf(ChatClient.BadResponseException.class, e.getCause());
		} finally {
			server.stop(0);
		}
	}

	@Test
	void noServerIsAConnectionProblem() {
		ExecutionException e = assertThrows(ExecutionException.class,
				() -> ChatClient.complete("http://127.0.0.1:1/v1/chat/completions", "{}", 2).get());
		assertInstanceOf(java.net.ConnectException.class, e.getCause());
	}
}
