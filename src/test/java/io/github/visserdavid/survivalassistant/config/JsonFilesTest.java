package io.github.visserdavid.survivalassistant.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JsonFilesTest {
	@TempDir
	Path dir;

	@Test
	void aMissingFileIsNull() throws Exception {
		assertNull(JsonFiles.read(dir.resolve("none.json"), ModConfig.class));
	}

	@Test
	void anUnreadableFileIsLeftAlone() throws IOException {
		Path file = dir.resolve("broken.json");
		String broken = "{ \"homeDistance\": 64, oops";
		Files.writeString(file, broken);
		assertThrows(JsonFiles.ReadFailure.class, () -> JsonFiles.read(file, ModConfig.class));
		assertEquals(broken, Files.readString(file), "a typo never wipes someone's settings");
	}

	@Test
	void writingReplacesTheFileWithoutLeftovers() throws Exception {
		Path file = dir.resolve("cfg.json");
		ModConfig cfg = new ModConfig();
		cfg.homeDistance = 99;
		JsonFiles.write(file, cfg);
		cfg.homeDistance = 100;
		JsonFiles.write(file, cfg);
		assertEquals(100, JsonFiles.read(file, ModConfig.class).homeDistance);
		assertFalse(Files.exists(dir.resolve("cfg.json.tmp")), "the temporary file is moved into place");
	}
}
