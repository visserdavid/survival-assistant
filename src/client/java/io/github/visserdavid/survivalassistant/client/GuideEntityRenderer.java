package io.github.visserdavid.survivalassistant.client;

import io.github.visserdavid.survivalassistant.client.skin.GuideSkins;
import io.github.visserdavid.survivalassistant.entity.GuideEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;

/**
 * Renders the guide with the player model geometry: the built-in skin, or the player's own pick from the skins
 * folder (see {@link GuideSkins}). Skins whose name ends with "_slim" use the slim (Alex) arms.
 *
 * <p>Uses {@link HumanoidRenderState} on purpose: in 26.3 every {@code AvatarRenderState} is sent to the vanilla
 * player renderer, which draws the default player skin instead of ours. The second skin layer (jacket, sleeves,
 * pants) still shows, because those parts are children of the body and limbs in the player model layers.
 */
public class GuideEntityRenderer extends HumanoidMobRenderer<GuideEntity, HumanoidRenderState, HumanoidModel<HumanoidRenderState>> {
	private final HumanoidModel<HumanoidRenderState> wide;
	private final HumanoidModel<HumanoidRenderState> slim;

	public GuideEntityRenderer(EntityRendererProvider.Context context) {
		this(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)));
	}

	private GuideEntityRenderer(EntityRendererProvider.Context context, HumanoidModel<HumanoidRenderState> wide) {
		super(context, wide, 0.5f);
		this.wide = wide;
		this.slim = new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM));
	}

	@Override
	public HumanoidRenderState createRenderState() {
		return new HumanoidRenderState();
	}

	@Override
	public void extractRenderState(GuideEntity entity, HumanoidRenderState state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		// The renderer draws with its one model field. Every guide uses the same chosen skin, so the field only
		// changes when the player picks a skin with other arms, never between two guides in one frame.
		HumanoidModel<HumanoidRenderState> wanted = GuideSkins.current().slim() ? slim : wide;
		if (this.model != wanted) this.model = wanted;
	}

	@Override
	public Identifier getTextureLocation(HumanoidRenderState state) {
		return GuideSkins.current().texture();
	}
}
