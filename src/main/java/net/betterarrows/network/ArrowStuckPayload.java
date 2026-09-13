package net.betterarrows.network;

import net.betterarrows.BetterArrowsMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

/**
 * One-shot server-to-client event fired the instant an arrow sticks in a living entity, carrying
 * the exact hit point vanilla itself computed for {@code onHitEntity} but normally discards -
 * expressed RELATIVE to the target's own position at that moment, not in absolute world space, so
 * it stays meaningful however far the target walks before a client gets around to resolving it -
 * the arrow's own flight direction at that moment (used to orient the stuck arrow along its real
 * trajectory instead of guessing a direction from position), and the target's body yaw at that
 * exact moment (used to compensate for mobs - golems, villagers - that snap to face their attacker
 * within a tick or two of being hit, which would otherwise make the resolved position drift by
 * however much the body has turned between the real hit and the client resolving it).
 * See {@link net.betterarrows.mixin.AbstractArrowMixin}. Not a continuous state sync.
 */
public record ArrowStuckPayload(
		long hitId, int targetEntityId, Vec3 hitOffset, Vec3 hitDirection, float hitBodyYaw)
		implements CustomPacketPayload {
	public static final Identifier ID = Identifier.fromNamespaceAndPath(BetterArrowsMod.MOD_ID, "arrow_stuck");
	public static final Type<ArrowStuckPayload> TYPE = new Type<>(ID);

	private static final StreamCodec<RegistryFriendlyByteBuf, Vec3> VEC3_STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.DOUBLE, (Vec3 v) -> v.x,
			ByteBufCodecs.DOUBLE, (Vec3 v) -> v.y,
			ByteBufCodecs.DOUBLE, (Vec3 v) -> v.z,
			Vec3::new);

	public static final StreamCodec<RegistryFriendlyByteBuf, ArrowStuckPayload> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.LONG, ArrowStuckPayload::hitId,
			ByteBufCodecs.VAR_INT, ArrowStuckPayload::targetEntityId,
			VEC3_STREAM_CODEC, ArrowStuckPayload::hitOffset,
			VEC3_STREAM_CODEC, ArrowStuckPayload::hitDirection,
			ByteBufCodecs.FLOAT, ArrowStuckPayload::hitBodyYaw,
			ArrowStuckPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
