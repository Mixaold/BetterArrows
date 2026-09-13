package net.betterarrows.client.mixin;

import net.betterarrows.client.LevelRenderTracker;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Brackets the exact call that submits world entities, so
 * {@link net.betterarrows.client.render.StuckArrowRenderLayer} only ever RESOLVES an arrow (the
 * once-per-arrow, frozen-forever step) against a pose that describes the world - see
 * {@link LevelRenderTracker}.
 *
 * <p>This has to be {@code submitEntities} specifically. {@code LevelRenderEvents.START_MAIN} and
 * {@code END_MAIN} look like the obvious bracket but are not: they fire inside {@code addMainPass},
 * which is the GPU draw phase and runs strictly AFTER {@code submitFeatures} (where entity render
 * layers are actually invoked and collected). Gating on those events leaves the flag false for
 * every single layer submit, so nothing ever resolves and no arrow is ever drawn at all.
 */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererSubmitEntitiesMixin {
	// require = 0 on both: LevelRenderTracker is deliberately fail-open, so if a future Minecraft
	// renames this method the mod falls back to resolving anywhere rather than refusing to start.
	@Inject(method = "submitEntities", at = @At("HEAD"), require = 0)
	private void betterArrows$beginEntitySubmit(PoseStack poseStack, LevelRenderState levelRenderState,
			SubmitNodeCollector collector, CallbackInfo ci) {
		LevelRenderTracker.beginLevelEntitySubmit();
		// Taken from the frame's own render state, never from the live camera - see the field's
		// javadoc in LevelRenderTracker for what goes wrong otherwise.
		if (levelRenderState != null && levelRenderState.cameraRenderState != null) {
			LevelRenderTracker.setEntitySubmitCameraPos(levelRenderState.cameraRenderState.pos);
		}
	}

	@Inject(method = "submitEntities", at = @At("RETURN"), require = 0)
	private void betterArrows$endEntitySubmit(PoseStack poseStack, LevelRenderState levelRenderState,
			SubmitNodeCollector collector, CallbackInfo ci) {
		LevelRenderTracker.endLevelEntitySubmit();
	}
}
