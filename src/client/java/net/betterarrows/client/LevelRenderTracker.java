package net.betterarrows.client;

import net.minecraft.world.phys.Vec3;

/**
 * Tracks whether we are currently inside the world's entity submission, so an arrow is only ever
 * RESOLVED (the once-per-arrow, frozen-forever step) against a pose that actually describes the
 * world.
 *
 * <p>A {@code LivingEntityRenderer} is not only used for the world: the inventory screen draws the
 * player through {@code GuiEntityRenderer}, on a PoseStack of its own, from a render state it
 * extracts separately. Resolving there produces an offset that is meaningless in the world - and
 * because resolution is deliberately frozen the first time it succeeds, and the frozen values live
 * on the shared cache entry rather than on the render state, that meaningless value would then be
 * the arrow's position in the world too, for the rest of the session. Getting shot and opening the
 * inventory before the arrow is ever drawn in the world is enough to hit this. The GUI pose is not
 * wild enough to reject on plausibility either - it is a small translate plus a 180 degree roll, so
 * a resolution made there looks perfectly reasonable and is simply mirrored and wrong.
 *
 * <p>The bracket is {@code LevelRenderer#submitEntities}, fed by
 * {@code LevelRendererSubmitEntitiesMixin}. Fabric's {@code LevelRenderEvents.START_MAIN} /
 * {@code END_MAIN} are NOT usable for this: they fire inside {@code addMainPass}, the GPU draw
 * phase, which runs after {@code submitFeatures} - so they are always "closed" at the moment a
 * render layer is actually invoked.
 *
 * <p>Fail-open by design, and the flag that decides that is set by the same hook that sets the
 * bracket: if the hook never fires at all (a rendering pipeline that submits entities some other
 * way), {@link #isRenderingLevel()} keeps returning true rather than the mod silently drawing
 * nothing.
 */
public final class LevelRenderTracker {
	private static boolean seenLevelEntitySubmit;
	private static boolean insideLevelEntitySubmit;

	/**
	 * The camera position the render pipeline itself is using for this frame's entities.
	 *
	 * <p>Not the same thing as {@code Minecraft.getInstance().gameRenderer.mainCamera().position()},
	 * and the difference is the whole reason this exists. {@code LevelRenderer#submitEntities}
	 * places every entity at {@code entityPos - levelRenderState.cameraRenderState.pos} - a
	 * position SNAPSHOTTED into the frame's render state - while the live camera object keeps
	 * moving and, with a third-person camera mod in the way, sits somewhere else entirely by the
	 * time a layer submits. Resolving a hit point against the live camera therefore lands it in
	 * the wrong place in the pose, and since a resolve is frozen for the life of the arrow, the
	 * error is permanent: the arrow hangs in the air beside the model instead of on it.
	 */
	private static Vec3 entitySubmitCameraPos = Vec3.ZERO;

	private LevelRenderTracker() {
	}

	public static void setEntitySubmitCameraPos(Vec3 pos) {
		if (pos != null) {
			entitySubmitCameraPos = pos;
		}
	}

	/** The camera the pose stack is actually built around while entities are being submitted. */
	public static Vec3 entitySubmitCameraPos() {
		return entitySubmitCameraPos;
	}

	public static void beginLevelEntitySubmit() {
		seenLevelEntitySubmit = true;
		insideLevelEntitySubmit = true;
	}

	public static void endLevelEntitySubmit() {
		insideLevelEntitySubmit = false;
	}

	public static boolean isRenderingLevel() {
		return insideLevelEntitySubmit || !seenLevelEntitySubmit;
	}

	private static long passCounter;

	/**
	 * Hands out a fresh token for one extraction of one entity's render state, so the render layer
	 * can tell "submitted twice off a single extraction" (draw once) from "rendered again in a
	 * genuinely separate pass" (draw again). Kept here rather than as a static field on a mixin so
	 * it is plain, ordinary state with no merge semantics to reason about.
	 */
	public static long nextRenderPass() {
		return ++passCounter;
	}
}
