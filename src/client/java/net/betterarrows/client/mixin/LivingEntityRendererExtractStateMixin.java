package net.betterarrows.client.mixin;

import net.betterarrows.BetterArrowsMod;
import net.betterarrows.client.LevelRenderTracker;
import net.betterarrows.client.StuckArrowClientCache;
import net.betterarrows.client.state.StuckArrowsAccess;
import net.betterarrows.config.BetterArrowsConfig;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Populates the stuck-arrow list added by {@link LivingEntityRenderStateMixin} every time a
 * render state is extracted from a living entity, for every mob (and the player). The cache
 * lookup here is a cheap map read - all the real per-arrow work already happened once, in
 * {@link StuckArrowClientCache#onArrowStuck}, when the hit was first synced.
 *
 * <p>Also stamps the render state with a fresh pass token, which is how the render layer tells
 * "this entity got submitted twice off one extraction" (draw once) from "this entity is being
 * rendered again in a separate pass" (draw again).
 */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererExtractStateMixin<T extends LivingEntity, S extends LivingEntityRenderState> {
	@Inject(
			method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V",
			at = @At("HEAD"))
	private void betterArrows$extractRenderState(T entity, S state, float partialTick, CallbackInfo ci) {
		StuckArrowsAccess access = (StuckArrowsAccess) state;
		access.betterArrows$setRenderPass(LevelRenderTracker.nextRenderPass());
		if (!BetterArrowsConfig.get().stuckArrows.enabled) {
			// A client can opt out even on a server that still sends the hits.
			access.betterArrows$setStuckArrows(List.of());
			return;
		}
		try {
			access.betterArrows$setStuckArrows(
					StuckArrowClientCache.getForRender(entity.getId(), entity.getArrowCount()));
		} catch (Exception e) {
			access.betterArrows$setStuckArrows(List.of());
			BetterArrowsMod.logOnce("Failed to collect stuck arrows for rendering", e);
		}
	}
}
