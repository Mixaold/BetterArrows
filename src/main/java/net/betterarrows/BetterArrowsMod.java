package net.betterarrows;

import net.betterarrows.config.BetterArrowsConfig;
import net.betterarrows.network.ArrowStuckPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Block;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class BetterArrowsMod implements ModInitializer {
	public static final String MOD_ID = "betterarrows";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	/**
	 * Upper bound on how many stuck-arrow records a single entity keeps, on both sides. Vanilla's
	 * own arrow count decays (and above 30 decays every tick), so in practice
	 * {@code LivingEntityArrowCountMixin} keeps lists far shorter than this; the cap is the
	 * backstop that guarantees the persisted NBT, the client cache and the catch-up packet burst
	 * a joining player pays for all stay bounded no matter what.
	 */
	public static final int MAX_STUCK_ARROWS = 32;

	/**
	 * Persistent (NBT-backed) per-entity backlog of stuck-arrow hits, so a player who starts
	 * tracking a target AFTER the hit already happened (rejoin, another player joining) can still
	 * be caught up. See {@link StuckArrowRecord}.
	 */
	public static final AttachmentType<List<StuckArrowRecord>> STUCK_ARROWS = AttachmentRegistry.createPersistent(
			Identifier.fromNamespaceAndPath(MOD_ID, "stuck_arrows"), StuckArrowRecord.CODEC.listOf());

	/**
	 * Blocks that already make their own noise when a projectile hits them - a bell rings, amethyst
	 * chimes, a target thuds. Layering a material "clack" on top of those sounds wrong, so this mod
	 * stays silent for them and leaves vanilla's own reaction alone.
	 */
	public static final TagKey<Block> NO_IMPACT_SOUND = TagKey.create(
			Registries.BLOCK, Identifier.fromNamespaceAndPath(MOD_ID, "no_impact_sound"));
	/**
	 * Blocks an arrow skates across instead of biting into or bouncing off.
	 *
	 * <p>A tag rather than a friction test on purpose. Slipperiness is the obvious signal - ice is
	 * 0.98 against a default of 0.6 - but slime sits at 0.8 and honey lower still, and an arrow
	 * skidding across a slime block is exactly wrong: those are sticky, not slippery. The vanilla
	 * ice tag says what is meant, and a datapack can add a modded frozen surface without touching
	 * code.
	 */
	public static final TagKey<Block> ARROWS_SLIDE_ON = TagKey.create(
			Registries.BLOCK, Identifier.fromNamespaceAndPath(MOD_ID, "arrows_slide_on"));

	private static final Set<String> LOGGED_ONCE = ConcurrentHashMap.newKeySet();

	/**
	 * Logs a failure the first time it is seen and stays silent afterwards. Everything this mod
	 * does runs either per server tick or per render frame, so an unguarded log of a recurring
	 * failure would itself become the problem it is reporting.
	 */
	public static void logOnce(String message, Throwable error) {
		if (LOGGED_ONCE.add(message)) {
			LOGGER.error("[{}] {} (further occurrences suppressed)", MOD_ID, message, error);
		}
	}

	@Override
	public void onInitialize() {
		BetterArrowsConfig.load();

		PayloadTypeRegistry.clientboundPlay().register(ArrowStuckPayload.TYPE, ArrowStuckPayload.STREAM_CODEC);

		EntityTrackingEvents.START_TRACKING.register((entity, player) -> {
			if (!(entity instanceof LivingEntity target)) {
				return;
			}
			// A client without this mod (vanilla, or the mod disabled) never registered the
			// channel - sending anyway is wasted bandwidth at best and a disconnect at worst.
			if (!ServerPlayNetworking.canSend(player, ArrowStuckPayload.TYPE)) {
				return;
			}
			List<StuckArrowRecord> records = target.getAttachedOrElse(STUCK_ARROWS, List.of());

			// Vanilla does NOT persist an entity's arrow count - it is synched entity data only,
			// and comes back as 0 after a world reload. Our records DO persist, so after a reload
			// the two disagree, and the client's "never show more arrows than vanilla has" clamp
			// would delete every arrow the moment the player walked back into range. Our persisted
			// records are the surviving truth here, so vanilla's count is restored from them.
			// Done on first tracking rather than on entity load because this is exactly the moment
			// the entity is being sent to a client, and it needs no extra lifecycle hook.
			if (!records.isEmpty() && target.getArrowCount() < records.size()) {
				target.setArrowCount(records.size());
			}

			for (StuckArrowRecord record : records) {
				ServerPlayNetworking.send(player, new ArrowStuckPayload(
						record.hitId(), target.getId(), record.hitOffset(), record.direction(), record.hitBodyYaw()));
			}
		});

		LOGGER.info("Better Arrows initialized");
	}
}
