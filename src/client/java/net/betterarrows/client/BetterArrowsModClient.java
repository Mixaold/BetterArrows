package net.betterarrows.client;

import net.betterarrows.BetterArrowsMod;
import net.betterarrows.client.render.StuckArrowRenderLayer;
import net.betterarrows.network.ArrowStuckPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityRenderLayerRegistrationCallback;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;

public class BetterArrowsModClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		LivingEntityRenderLayerRegistrationCallback.EVENT.register((entityType, entityRenderer, registrationHelper, context) ->
				registerStuckArrowLayer(entityRenderer, registrationHelper, context));

		ClientPlayNetworking.registerGlobalReceiver(ArrowStuckPayload.TYPE, (payload, context) ->
				context.client().execute(() -> {
					try {
						StuckArrowClientCache.onArrowStuck(payload);
					} catch (Exception e) {
						BetterArrowsMod.logOnce("Failed to accept a stuck-arrow packet", e);
					}
				}));

		// NOT hooked to the arrow ENTITY's own unload: vanilla discards that entity almost
		// immediately after it sticks (that's how it implements "stuck" at all - see
		// AbstractArrowMixin), so treating "arrow entity unloaded" as "the visual decal should
		// disappear" deleted every arrow within a tick of it appearing. Arrows instead age out the
		// way vanilla's own do, by following the target's arrow count down as it decays - see
		// StuckArrowClientCache#getForRender - and the target's unload clears whatever is left.
		ClientEntityEvents.ENTITY_UNLOAD.register((entity, level) -> {
			if (entity instanceof LivingEntity) {
				StuckArrowClientCache.remove(entity.getId());
			}
		});

		// A plain disconnect doesn't necessarily unload every entity individually (the whole
		// ClientLevel can just be dropped at once), so without this, stale entries could survive
		// into the next world/server, where entity ids get reused starting low again - showing
		// "ghost" arrows on unrelated mobs that were never actually shot there.
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			StuckArrowClientCache.clear();
		});

		BetterArrowsMod.LOGGER.info("Better Arrows client initialized");
	}

	// LivingEntityRenderer<?, ?, ?> arrives wildcard-captured; a generic helper method lets us
	// recover a single consistent (S, M) pair to construct our layer against, via an unchecked
	// cast - the same pattern needed anywhere a generic RenderLayer is attached across all mobs.
	private static <S extends LivingEntityRenderState, M extends EntityModel<S>> void registerStuckArrowLayer(
			LivingEntityRenderer<?, ?, ?> entityRenderer,
			LivingEntityRenderLayerRegistrationCallback.RegistrationHelper registrationHelper,
			EntityRendererProvider.Context context) {
		@SuppressWarnings("unchecked")
		RenderLayerParent<S, M> renderer = (RenderLayerParent<S, M>) entityRenderer;
		registrationHelper.register(new StuckArrowRenderLayer<>(renderer, context));
	}
}
