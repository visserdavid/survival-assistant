package io.github.visserdavid.survivalassistant.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.visserdavid.survivalassistant.client.skin.GuideSkins;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;

/**
 * The softly glowing eyes of the keeper (the default skin and its route outfits), drawn full-bright like a spider's
 * eyes. The player's own skins are drawn as they are.
 */
public class GuideEyesLayer extends EyesLayer<GuideRenderState, HumanoidModel<GuideRenderState>> {
	private static final RenderType EYES = RenderTypes.eyes(GuideSkins.EYES_TEXTURE);

	public GuideEyesLayer(RenderLayerParent<GuideRenderState, HumanoidModel<GuideRenderState>> parent) {
		super(parent);
	}

	@Override
	public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, GuideRenderState state, float yRot, float xRot) {
		if (GuideSkins.keeperShown(state.route)) super.submit(poseStack, collector, light, state, yRot, xRot);
	}

	@Override
	public RenderType renderType() {
		return EYES;
	}
}
