package io.github.visserdavid.survivalassistant.chat;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CompletionException;

/**
 * Talks to an OpenAI-compatible chat endpoint, such as llama.cpp's {@code llama-server}.
 * Everything is asynchronous: the game never waits for the model.
 */
public final class ChatClient {
	private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
	/** Longest error body quoted in the log. */
	private static final int ERROR_BODY_CHARS = 200;
	/** A short chat reply is a few hundred bytes; anything far beyond this is not an answer we can use. */
	private static final int MAX_RESPONSE_BYTES = 256 * 1024;

	private static final HttpClient HTTP = HttpClient.newBuilder()
			// Plain HTTP/1.1: Java would otherwise try an h2c upgrade, which small local servers don't always handle.
			.version(HttpClient.Version.HTTP_1_1)
			.connectTimeout(CONNECT_TIMEOUT)
			.build();

	private ChatClient() {}

	/** The server answered, but not with a usable reply (HTTP error, oversized or malformed body). */
	public static final class BadResponseException extends IOException {
		BadResponseException(String message) {
			super(message);
		}
	}

	/**
	 * Sends the request body and completes with the text of the first answer ("" when the answer has no text).
	 * Never throws: a bad endpoint, a connection problem or a bad answer completes the future exceptionally.
	 */
	public static CompletableFuture<String> complete(String endpoint, String requestJson, int timeoutSeconds) {
		HttpRequest request;
		try {
			request = HttpRequest.newBuilder(URI.create(endpoint))
					.timeout(Duration.ofSeconds(timeoutSeconds))
					.header("Content-Type", "application/json")
					.POST(HttpRequest.BodyPublishers.ofString(requestJson, StandardCharsets.UTF_8))
					.build();
		} catch (IllegalArgumentException e) {
			return CompletableFuture.failedFuture(new BadResponseException("Invalid chat endpoint '" + endpoint + "': " + e.getMessage()));
		}
		// the request timeout covers waiting for the headers; orTimeout also covers a body that trickles in forever
		return HTTP.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream())
				.thenApply(response -> {
					byte[] bytes = readAtMost(response.body(), MAX_RESPONSE_BYTES + 1);
					String body = new String(bytes, 0, Math.min(bytes.length, MAX_RESPONSE_BYTES), StandardCharsets.UTF_8);
					int status = response.statusCode();
					if (status < 200 || status >= 300) {
						throw new CompletionException(new BadResponseException("HTTP " + status + ": "
								+ (body.length() > ERROR_BODY_CHARS ? body.substring(0, ERROR_BODY_CHARS) : body)));
					}
					if (bytes.length > MAX_RESPONSE_BYTES) {
						throw new CompletionException(new BadResponseException("Reply too large (over " + MAX_RESPONSE_BYTES + " bytes)"));
					}
					return parse(body);
				})
				.orTimeout(timeoutSeconds + BODY_GRACE_SECONDS, TimeUnit.SECONDS);
	}

	/** Extra time for reading the body after the headers arrived. */
	private static final int BODY_GRACE_SECONDS = 5;

	/** Reads at most {@code limit} bytes and closes the stream (so an endless body can't fill the memory). */
	static byte[] readAtMost(InputStream in, int limit) {
		try (in) {
			return in.readNBytes(limit);
		} catch (IOException e) {
			throw new CompletionException(e);
		}
	}

	/**
	 * Extracts {@code choices[0].message.content} from a chat completion response: "" when the response has no
	 * text there, so a strange answer is treated like an empty one.
	 */
	static String parse(String json) {
		try {
			JsonElement root = JsonParser.parseString(json);
			if (!root.isJsonObject()) return "";
			JsonElement choices = root.getAsJsonObject().get("choices");
			if (choices == null || !choices.isJsonArray()) return "";
			JsonArray list = choices.getAsJsonArray();
			if (list.isEmpty() || !list.get(0).isJsonObject()) return "";
			JsonElement message = list.get(0).getAsJsonObject().get("message");
			if (message == null || !message.isJsonObject()) return "";
			JsonObject msg = message.getAsJsonObject();
			JsonElement content = msg.get("content");
			return content != null && content.isJsonPrimitive() ? content.getAsString() : "";
		} catch (JsonParseException e) {
			return "";
		}
	}
}
