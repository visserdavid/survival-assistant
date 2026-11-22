package io.github.visserdavid.survivalassistant.client;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.Identifier;
import io.github.visserdavid.survivalassistant.client.skin.GuideSkins;
import io.github.visserdavid.survivalassistant.entity.GuideEntity;

/**
 * Renders the guide with the player model: the built-in skin, or the player's own pick from the skins folder
 * (see {@link GuideSkins}). Skins whose name ends with "_slim" use the slim (Alex) arms.
 */
public class GuideEntityRenderer extends HumanoidMobRenderer<GuideEntity, AvatarRenderState, PlayerModel> {
	private final PlayerModel wide;
	private final PlayerModel slim;

	public GuideEntityRenderer(EntityRendererProvider.Context context) {
		this(context, new PlayerModel(context.bakeLayer(ModelLayers.PLAYER), false));
	}

	private GuideEntityRenderer(EntityRendererProvider.Context context, PlayerModel wide) {
		super(context, wide, 0.5f);
		this.wide = wide;
		this.slim = new PlayerModel(context.bakeLayer(ModelLayers.PLAYER_SLIM), true);
	}

	@Override
	public AvatarRenderState createRenderState() {
		return new AvatarRenderState();
	}

	@Override
	public void extractRenderState(GuideEntity entity, AvatarRenderState state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		// all guides share the chosen skin, so switching the model here is safe
		this.model = GuideSkins.current().slim() ? slim : wide;
	}

	@Override
	public Identifier getTextureLocation(AvatarRenderState state) {
		return GuideSkins.current().texture();
	}
}
