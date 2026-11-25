package io.github.visserdavid.survivalassistant.client;

import io.github.visserdavid.survivalassistant.client.skin.GuideSkins;
import io.github.visserdavid.survivalassistant.entity.GuideEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.Identifier;

/**
 * Renders the guide with the player model geometry: the built-in keeper (dressed for the owner's route), the
 * classic traveller, or the player's own pick from the skins folder (see {@link GuideSkins}). Skins whose name ends
 * with "_slim" use the slim (Alex) arms.
 *
 * <p>Uses a {@code HumanoidRenderState} on purpose: in 26.3 every {@code AvatarRenderState} is sent to the vanilla
 * player renderer, which draws the default player skin instead of ours. The second skin layer (jacket, sleeves,
 * pants) still shows, because those parts are children of the body and limbs in the player model layers. The
 * keeper's eyes glow ({@link GuideEyesLayer}).
 */
public class GuideEntityRenderer extends HumanoidMobRenderer<GuideEntity, GuideRenderState, HumanoidModel<GuideRenderState>> {
	private final HumanoidModel<GuideRenderState> wide;
	private final HumanoidModel<GuideRenderState> slim;

	public GuideEntityRenderer(EntityRendererProvider.Context context) {
		this(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)));
	}

	private GuideEntityRenderer(EntityRendererProvider.Context context, HumanoidModel<GuideRenderState> wide) {
		super(context, wide, 0.5f);
		this.wide = wide;
		this.slim = new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM));
		addLayer(new GuideEyesLayer(this));
	}

	@Override
	public GuideRenderState createRenderState() {
		return new GuideRenderState();
	}

	@Override
	public void extractRenderState(GuideEntity entity, GuideRenderState state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.route = entity.getRoute();
		// The renderer draws with its one model field. Only the player's own skins can have slim arms, and every
		// guide then uses that same skin, so the field only changes when the player picks a skin with other arms.
		HumanoidModel<GuideRenderState> wanted = GuideSkins.current(state.route).slim() ? slim : wide;
		if (this.model != wanted) this.model = wanted;
	}

	@Override
	public Identifier getTextureLocation(GuideRenderState state) {
		return GuideSkins.current(state.route).texture();
	}
}
