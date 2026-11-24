package io.github.visserdavid.survivalassistant.state;

import io.github.visserdavid.survivalassistant.advice.HudSnapshot;
import io.github.visserdavid.survivalassistant.advice.Navigation;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HudCodecTest {
	private static HudSnapshot roundTrip(HudSnapshot s) {
		ByteBuf buf = Unpooled.buffer();
		HudCodec.STREAM_CODEC.encode(buf, s);
		return HudCodec.STREAM_CODEC.decode(buf);
	}

	@Test
	void theTaskCardSurvivesTheTrip() {
		List<String> completed = new ArrayList<>();
		for (int i = 0; i < HudSnapshot.MAX_COMPLETED; i++) completed.add("goal_" + i);
		HudSnapshot s = new HudSnapshot(true, 7, completed,
				List.of(new HudSnapshot.Entry("wood", 3, 10), new HudSnapshot.Entry("farm", 0, 0)),
				new HudSnapshot.Entry("brew_potion.water", 1, 3), "creeper",
				Optional.of(new Navigation.Waypoint(Navigation.Kind.DEATH, new Navigation.Mark("minecraft:the_nether", -1234, -60, 98765), 5000)),
				true);
		assertEquals(s, roundTrip(s));
		assertEquals(HudSnapshot.INACTIVE, roundTrip(HudSnapshot.INACTIVE));
		HudSnapshot noStep = new HudSnapshot(true, 1, List.of(), List.of(new HudSnapshot.Entry("wood", 0, 10)),
				HudSnapshot.NO_STEP, "", Optional.empty(), false);
		assertEquals(noStep, roundTrip(noStep));
	}
}
