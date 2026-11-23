package io.github.visserdavid.survivalassistant.chat;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * Talks to an OpenAI-compatible chat endpoint, such as llama.cpp's {@code llama-server}.
 * Everything is asynchronous: the game never waits for the model.
 */
public final class ChatClient {
	private static final HttpClient HTTP = HttpClient.newBuilder()
			// Plain HTTP/1.1: Java would otherwise try an h2c upgrade, which small local servers don't always handle.
			.version(HttpClient.Version.HTTP_1_1)
			.connectTimeout(Duration.ofSeconds(3))
			.build();

	private ChatClient() {}

	/** Sends the request body and completes with the raw text of the first answer. */
	public static CompletableFuture<String> complete(String endpoint, String requestJson, int timeoutSeconds) {
		HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint))
				.timeout(Duration.ofSeconds(Math.max(1, timeoutSeconds)))
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(requestJson, StandardCharsets.UTF_8))
				.build();
		return HTTP.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
				.thenApply(response -> {
					if (response.statusCode() != 200) {
						String body = response.body();
						throw new IllegalStateException("HTTP " + response.statusCode() + ": "
								+ (body.length() > 200 ? body.substring(0, 200) : body));
					}
					return parse(response.body());
				});
	}

	/** Extracts {@code choices[0].message.content} from a chat completion response. */
	static String parse(String json) {
		JsonObject root = JsonParser.parseString(json).getAsJsonObject();
		JsonObject message = root.getAsJsonArray("choices").get(0).getAsJsonObject().getAsJsonObject("message");
		return message.has("content") && !message.get("content").isJsonNull() ? message.get("content").getAsString() : "";
	}
}
