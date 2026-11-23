package io.github.visserdavid.survivalassistant.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Reading and writing the small JSON config files (server and client). */
public final class JsonFiles {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private JsonFiles() {}

	/**
	 * Reads {@code path} as {@code type}: null for a missing or empty file.
	 *
	 * @throws ReadFailure the file exists but can't be read or parsed (the caller should leave it alone)
	 */
	public static <T> @Nullable T read(Path path, Class<T> type) throws ReadFailure {
		if (!Files.exists(path)) return null;
		try (Reader r = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
			return GSON.fromJson(r, type);
		} catch (IOException | JsonParseException e) {
			throw new ReadFailure(e);
		}
	}

	/** Writes {@code value} to a temporary file first and then moves it into place, so a crash never leaves half a file. */
	public static void write(Path path, Object value) throws IOException {
		Path tmp = path.resolveSibling(path.getFileName() + ".tmp");
		try (Writer w = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
			GSON.toJson(value, w);
		}
		try {
			Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		} catch (AtomicMoveNotSupportedException e) {
			Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING);
		}
	}

	/** {@code value} as JSON text (for sending a config over the network). */
	public static String toJson(Object value) {
		return GSON.toJson(value);
	}

	/**
	 * Parses JSON text as {@code type}.
	 *
	 * @throws JsonParseException the text is not valid JSON for this type
	 */
	public static <T> T fromJson(String json, Class<T> type) {
		T value = GSON.fromJson(json, type);
		if (value == null) throw new JsonParseException("empty");
		return value;
	}

	/** The file exists but could not be read or parsed. */
	public static final class ReadFailure extends Exception {
		ReadFailure(Throwable cause) {
			super(cause);
		}
	}
}
