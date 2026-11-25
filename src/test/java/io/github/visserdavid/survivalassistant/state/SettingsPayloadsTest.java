package io.github.visserdavid.survivalassistant.state;

import io.github.visserdavid.survivalassistant.advice.GuideChoices;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.network.codec.StreamCodec;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SettingsPayloadsTest {
	private static <T> T roundTrip(StreamCodec<ByteBuf, T> codec, T value) {
		ByteBuf buf = Unpooled.buffer();
		codec.encode(buf, value);
		return codec.decode(buf);
	}

	@Test
	void choicesSurviveTheTrip() {
		GuideChoices c = new GuideChoices(false, "Ödön", GuideChoices.MUSINGS_ALL, true, "farmer");
		assertEquals(c, roundTrip(SettingsPayloads.CHOICES_CODEC, c));
		GuideChoices empty = new GuideChoices(true, "", GuideChoices.MUSINGS_OFF, false, "free");
		assertEquals(empty, roundTrip(SettingsPayloads.CHOICES_CODEC, empty));
	}

	@Test
	void syncAndUpdatesSurviveTheTrip() {
		var sync = new SettingsPayloads.Sync(new GuideChoices(true, "", GuideChoices.MUSINGS_ON, false, "free"), "Pim",
				"{\"homeDistance\": 64}", true, false);
		assertEquals(sync, roundTrip(SettingsPayloads.Sync.CODEC, sync));
		var config = new SettingsPayloads.ConfigUpdate("{\"musings\": false}");
		assertEquals(config, roundTrip(SettingsPayloads.ConfigUpdate.CODEC, config));
		var choices = new SettingsPayloads.ChoicesUpdate(new GuideChoices(false, "Bram", GuideChoices.MUSINGS_OFF, true, "free"));
		assertEquals(choices, roundTrip(SettingsPayloads.ChoicesUpdate.CODEC, choices));
	}
}
