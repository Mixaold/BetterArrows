package net.betterarrows.client;

import net.betterarrows.BetterArrowsMod;
import net.betterarrows.network.ArrowStuckPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Client-side-only cache of stuck-arrow hit points, keyed by target entity id.
 *
 * <p>{@link Entry#resolvedPartPath}/{@link Entry#boneLocalOffset}/{@link Entry#boneLocalDirection}
 * start unresolved (null) and are computed exactly once, the first time
 * {@link net.betterarrows.client.render.StuckArrowRenderLayer} draws this arrow: it walks the
 * target's model to find the specific cube closest to the real hit point, then freezes the
 * offset/direction in THAT cube's own local space plus an identifier (part path + cube index) for
 * re-locating that same cube on later frames.
 *
 * <p>A direct {@code ModelPart} reference isn't kept: {@code ModelPart#translateAndRotate} only
 * applies that one part's own transform, not its ancestors', so replaying it alone would be wrong
 * for any part nested deeper than a direct child of the model root. The render layer instead
 * re-walks the tree each frame (which composes the full ancestor chain correctly by construction)
 * and renders the arrow the moment it revisits the matching (path, cubeIndex).
 *
 * <p>The values must NOT be re-derived from world space on later frames: inverting a pose matrix
 * and immediately re-applying that SAME matrix via PoseStack always cancels back to the original
 * world value (M * M^-1 * v = v) regardless of how the entity/bone has since moved - freezing the
 * local values once and replaying them through each frame's (different) pose for that same cube
 * is what makes the arrow follow that part's animation (walking, attacking, flying, etc.).
 */
public final class StuckArrowClientCache {
	/** Give up re-resolving an arrow after this many consecutive failed attempts. */
	private static final int MAX_RESOLVE_ATTEMPTS = 120;

	/**
	 * How long a freshly arrived record is immune to being trimmed against vanilla's arrow count.
	 * The hit packet and the entity-data update that carries the new count are two separate
	 * packets; they are ordered, so in practice the count is already there - but if it ever were
	 * not, trimming would delete the newest record's OLDER sibling, i.e. silently erase an arrow
	 * that is genuinely stuck. This makes that impossible, at the cost of a record occasionally
	 * outliving vanilla's count by a second or two. Only ever delays removal, never causes it.
	 */
	private static final long TRIM_GRACE_NANOS = 2_000_000_000L;

	public static final class Entry {
		public final long hitId;
		/** Hit point relative to the target's position at impact - see {@code StuckArrowRecord}. */
		public final Vec3 hitOffset;
		public final Vec3 hitDirection;
		public final float hitBodyYaw;
		public @Nullable String resolvedPartPath;
		public int resolvedCubeIndex = -1;
		public @Nullable Vec3 boneLocalOffset;
		public @Nullable Vec3 boneLocalDirection;
		/**
		 * Counts failed resolve attempts so an arrow that can never match a cube (a model with no
		 * cubes at all, a model a mod swapped out from under us) stops costing a full model tree
		 * walk every single frame, forever.
		 */
		public int resolveAttempts;
		/** Guards against drawing this arrow more than once for the same extracted render state. */
		public long lastRenderedPass = Long.MIN_VALUE;
		private final long arrivalNanos = System.nanoTime();

		private Entry(long hitId, Vec3 hitOffset, Vec3 hitDirection, float hitBodyYaw) {
			this.hitId = hitId;
			this.hitOffset = hitOffset;
			this.hitDirection = hitDirection;
			this.hitBodyYaw = hitBodyYaw;
		}

		public boolean isResolved() {
			return this.resolvedPartPath != null && this.boneLocalOffset != null && this.boneLocalDirection != null;
		}

		public boolean givenUp() {
			return this.resolveAttempts >= MAX_RESOLVE_ATTEMPTS;
		}
	}

	private static final class Target {
		final List<Entry> entries = new ArrayList<>();
		/**
		 * Highest arrow count ever observed on this entity, or -1 before the first observation.
		 *
		 * <p>Records are dropped only when the count falls BELOW this, i.e. when vanilla actually
		 * healed an arrow out. Comparing records against the raw count instead is a trap: a new
		 * hit reaches the client as our own packet immediately, while the matching arrow-count
		 * update only leaves the server at the end of the tick. In that window there is one more
		 * record than vanilla admits to, and a raw comparison "corrects" it by deleting the OLDEST
		 * record - so firing a second arrow made the first one vanish and the second appear to
		 * have replaced it.
		 */
		int highWaterArrowCount = -1;
	}

	private static final Map<Integer, Target> BY_TARGET = new HashMap<>();

	private StuckArrowClientCache() {
	}

	public static void onArrowStuck(ArrowStuckPayload payload) {
		Minecraft client = Minecraft.getInstance();
		if (client.level == null) {
			return;
		}
		if (!isFinite(payload.hitOffset()) || !isFinite(payload.hitDirection())
				|| !Float.isFinite(payload.hitBodyYaw())) {
			// A non-finite value would propagate straight into an inverted pose matrix and take
			// the whole entity's rendering (not just this arrow) down with it.
			return;
		}
		Entity targetEntity = client.level.getEntity(payload.targetEntityId());
		if (!(targetEntity instanceof LivingEntity)) {
			return;
		}

		Target target = BY_TARGET.computeIfAbsent(payload.targetEntityId(), id -> new Target());
		// Defensive: a backlog resend (see BetterArrowsMod's START_TRACKING listener) could in
		// principle reach a player who already got this same arrow via the original live push.
		for (Entry existing : target.entries) {
			if (existing.hitId == payload.hitId()) {
				return;
			}
		}
		target.entries.add(new Entry(payload.hitId(), payload.hitOffset(), payload.hitDirection(), payload.hitBodyYaw()));
		trimOldest(target, BetterArrowsMod.MAX_STUCK_ARROWS);
	}

	/**
	 * Called once per extracted render state (see {@code LivingEntityRendererExtractStateMixin}),
	 * both to hand the render layer its list and to keep that list in step with vanilla's own
	 * arrow count, which decays one arrow at a time. Without this the client would keep drawing
	 * arrows vanilla has long since healed off, and the list would only ever grow.
	 */
	public static List<Entry> getForRender(int targetEntityId, int arrowCount) {
		Target target = BY_TARGET.get(targetEntityId);
		if (target == null) {
			return List.of();
		}

		if (target.highWaterArrowCount >= 0 && arrowCount < target.highWaterArrowCount) {
			// Vanilla healed an arrow out. Mirror that by dropping the oldest record, so arrows
			// age off exactly as vanilla's own do. A count that merely has not caught up with a
			// brand new hit is NOT a decay and must not delete anything.
			trimToArrowCount(target, arrowCount);
		}
		target.highWaterArrowCount = Math.max(target.highWaterArrowCount, arrowCount);
		trimOldest(target, BetterArrowsMod.MAX_STUCK_ARROWS);

		if (target.entries.isEmpty()) {
			BY_TARGET.remove(targetEntityId);
			return List.of();
		}
		return target.entries;
	}

	/** Called when the target entity itself unloads - drops its whole arrow list. */
	public static void remove(int targetEntityId) {
		BY_TARGET.remove(targetEntityId);
	}

	/**
	 * Called on disconnect (see {@code BetterArrowsModClient}'s {@code ClientPlayConnectionEvents.DISCONNECT}
	 * hook). A plain disconnect (leaving a world/server) does not necessarily fire an unload event
	 * for every entity individually - the whole {@code ClientLevel} can simply be dropped at once -
	 * so without this, stale entries keyed by entity id could survive into the next world/server
	 * joined, where ids are reused starting low again, showing "ghost" arrows on unrelated mobs
	 * that were never actually shot.
	 */
	public static void clear() {
		BY_TARGET.clear();
	}

	/** Oldest-first, but never touches a record still inside its arrival grace period. */
	private static void trimToArrowCount(Target target, int arrowCount) {
		int excess = target.entries.size() - Math.max(arrowCount, 0);
		if (excess <= 0) {
			return;
		}
		long now = System.nanoTime();
		int i = 0;
		while (excess > 0 && i < target.entries.size()) {
			if (now - target.entries.get(i).arrivalNanos >= TRIM_GRACE_NANOS) {
				target.entries.remove(i);
				excess--;
			} else {
				i++;
			}
		}
	}

	private static void trimOldest(Target target, int keep) {
		int excess = target.entries.size() - Math.max(keep, 0);
		for (int i = 0; i < excess; i++) {
			target.entries.remove(0);
		}
	}

	private static boolean isFinite(Vec3 v) {
		return Double.isFinite(v.x) && Double.isFinite(v.y) && Double.isFinite(v.z);
	}
}
