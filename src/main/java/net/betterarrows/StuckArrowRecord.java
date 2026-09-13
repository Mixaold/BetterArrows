package net.betterarrows;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.phys.Vec3;

/**
 * Server-side memory of one arrow having stuck in a living entity, kept in a persistent data
 * attachment on the target (see {@link BetterArrowsMod#STUCK_ARROWS}) so it can be replayed to a
 * player who starts tracking the entity AFTER the hit already happened - rejoining a saved world,
 * walking back into render distance, etc. The one-shot {@code ArrowStuckPayload} push at hit time
 * only reaches players already tracking the target at that exact moment.
 *
 * <p>{@code hitOffset} is stored RELATIVE to the target's own position at the moment of impact,
 * never as an absolute world point. The target keeps walking after being hit, and a client may
 * only resolve this record much later (rejoin, re-enter render distance); an absolute point
 * resolved against a position the entity has since left behind lands nowhere near the real wound,
 * which is exactly the "arrow jumps somewhere random after rejoining" bug.
 *
 * <p>{@code hitId} is a per-hit unique id rather than the arrow's entity id: entity ids are not
 * stable across a save/reload, so a persisted record could otherwise collide with a brand-new
 * arrow that happens to be assigned the same id, making the client's duplicate guard silently
 * swallow a real new hit.
 */
public record StuckArrowRecord(long hitId, Vec3 hitOffset, Vec3 direction, float hitBodyYaw) {
	public static final Codec<StuckArrowRecord> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.LONG.fieldOf("hitId").forGetter(StuckArrowRecord::hitId),
			Vec3.CODEC.fieldOf("hitOffset").forGetter(StuckArrowRecord::hitOffset),
			Vec3.CODEC.fieldOf("direction").forGetter(StuckArrowRecord::direction),
			Codec.FLOAT.fieldOf("hitBodyYaw").forGetter(StuckArrowRecord::hitBodyYaw)
	).apply(instance, StuckArrowRecord::new));
}
