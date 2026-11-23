package io.github.visserdavid.survivalassistant.advice;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GuideSpacingTest {
	/** Yaw 0: the player looks towards +z. */
	private static final double SOUTH = 0;

	@Test
	void straightAheadIsInView() {
		assertTrue(GuideSpacing.inView(SOUTH, 0, 4));
		assertTrue(GuideSpacing.inView(SOUTH, 1, 4), "slightly to the side, still in the cone");
		assertFalse(GuideSpacing.inView(SOUTH, 4, 1), "to the side");
		assertFalse(GuideSpacing.inView(SOUTH, 0, -4), "behind");
		assertFalse(GuideSpacing.inView(SOUTH, 0, 10), "far away does not matter");
	}

	@Test
	void yawTurnsTheCone() {
		assertTrue(GuideSpacing.inView(90, -4, 0), "yaw 90 looks towards -x");
		assertFalse(GuideSpacing.inView(90, 0, 4));
	}

	@Test
	void tooCloseIsInTheWayEvenBehind() {
		assertTrue(GuideSpacing.inTheWay(SOUTH, 0, -1.5));
		assertFalse(GuideSpacing.inTheWay(SOUTH, 0, -3.5));
		assertTrue(GuideSpacing.inTheWay(SOUTH, 0, 0), "standing in the player");
	}

	@Test
	void bestSpotIsBehindThePlayer() {
		double behind = GuideSpacing.score(SOUTH, 0, -3.5);
		double beside = GuideSpacing.score(SOUTH, 3.5, 0);
		double front = GuideSpacing.score(SOUTH, 0, 3.5);
		assertTrue(behind > beside, "behind beats beside");
		assertEquals(Double.NEGATIVE_INFINITY, front, "never in front");
	}

	@Test
	void spotsMustBeAtTheRightDistance() {
		assertEquals(Double.NEGATIVE_INFINITY, GuideSpacing.score(SOUTH, 0, -1));
		assertEquals(Double.NEGATIVE_INFINITY, GuideSpacing.score(SOUTH, 0, -8));
		assertTrue(GuideSpacing.score(SOUTH, -3, -2) > Double.NEGATIVE_INFINITY);
	}
}
