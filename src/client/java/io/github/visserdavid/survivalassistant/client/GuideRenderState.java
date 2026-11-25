package io.github.visserdavid.survivalassistant.client;

import net.minecraft.client.renderer.entity.state.HumanoidRenderState;

/** The guide's render state: a humanoid plus the owner's route, which decides the keeper's outfit. */
public class GuideRenderState extends HumanoidRenderState {
	/** The owner's route id, or "" when there is none. */
	public String route = "";
}
