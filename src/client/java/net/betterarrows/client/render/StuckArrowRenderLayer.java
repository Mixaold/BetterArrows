package net.betterarrows.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.betterarrows.BetterArrowsMod;
import net.betterarrows.config.BetterArrowsConfig;
import net.betterarrows.client.LevelRenderTracker;
import net.betterarrows.client.StuckArrowClientCache;
import net.betterarrows.client.mixin.ModelPartAccessor;
import net.betterarrows.client.state.StuckArrowsAccess;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.object.projectile.ArrowModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.TippableArrowRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.ArrowRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.List;
import java.util.Map;

/**
 * Renders each stuck arrow anchored to the specific model cube closest to the real hit point,
 * oriented along the arrow's real flight direction at the moment of impact.
 *
 * <p>Resolution (once, the first time an arrow is drawn - see
 * {@link StuckArrowClientCache.Entry}): walk the target model, which hands us each cube together
 * with its fully-composed current-frame pose (ancestors included); invert that pose to project the
 * real (camera-relative) hit point into that cube's own local space (converting to pixel units -
 * cube bounds and part offsets are authored in 1/16-block units, see
 * {@code ModelPart#translateAndRotate}), measure the clamped distance to the cube, and keep the
 * closest one. The winning cube's identity (part path + cube index), offset (clamped onto the cube
 * so the arrow anchors to real geometry instead of floating past it) and direction are frozen.
 *
 * <p>The hit point arrives RELATIVE to the target's position at the moment of impact and is turned
 * back into a world point against the target's CURRENT position here. Storing it absolutely
 * instead is only correct while the target has not moved since being hit - which is why that
 * version looked right in the same session and jumped somewhere arbitrary after a rejoin, where
 * resolution runs against a position the mob walked away from long ago.
 *
 * <p>Every later frame we re-walk the tree and render the arrow the moment it revisits that same
 * (path, cubeIndex) - at that point in the traversal, poseStack is already correctly positioned
 * through the FULL ancestor chain for this frame's pose, which is what makes the arrow follow that
 * specific part's animation (a swinging arm, a walking leg, a crouching spider), not just the
 * entity's overall root position. A plain {@code ModelPart} reference isn't kept and replayed
 * directly, because {@code translateAndRotate} only applies one part's own transform, not its
 * ancestors' - wrong for anything nested deeper than a direct child of root.
 */
public class StuckArrowRenderLayer<S extends LivingEntityRenderState, M extends EntityModel<S>> extends RenderLayer<S, M> {
	private static final Identifier ARROW_TEXTURE = TippableArrowRenderer.NORMAL_ARROW_LOCATION;
	private static final float PIXELS_PER_BLOCK = 16.0F;

	/**
	 * Sanity bound (in model pixels, squared) on how far the hit point may sit from the nearest
	 * cube before we refuse to freeze a resolution against it.
	 *
	 * <p>This used to be 256 pixels squared - sixteen BLOCKS - which is not a sanity bound at all:
	 * it accepted whatever cube happened to be nearest no matter how absurdly far away, so a hit
	 * that resolved against nothing sensible still got drawn, hanging in the air beside the model.
	 * That showed up hard on models replaced by Entity Model Features, where hits near the head
	 * were landing on a custom headwear part six pixels out.
	 *
	 * <p>Twelve pixels, three quarters of a block. It was six, and six was measured against
	 * VANILLA models: on a model replaced by a resource pack the drawn mesh is simply not where
	 * the hitbox is, and real hits were landing 6.6 and 7.5 pixels from the nearest cube - just
	 * outside - so every arrow on such an entity was silently dropped and the mod appeared to have
	 * stopped working entirely. Loose enough now for a replaced mesh, still far tighter than the
	 * sixteen BLOCKS this once was, and the search always takes the NEAREST cube regardless, so a
	 * wider bound only ever decides whether to draw at all - never where.
	 */
	private static final float MAX_RESOLVE_DIST_SQ = 144.0F;

	/**
	 * Penalty, in the same pixel-squared units as the search, for each level a cube sits below the
	 * root. It exists to make the search prefer a real BONE over a decorative shell hung off it.
	 *
	 * <p>Every model layers an inflated copy over the base: vanilla's own hat and jacket, and far
	 * more so a replaced one - Fresh Animations inflates headwear by 0.49 pixels, the jacket by
	 * 0.24, sleeves and trousers by 0.25. That shell is by construction the NEAREST surface to an
	 * arrow arriving from outside, so a plain nearest-cube search picks it every single time. The
	 * log bore that out: every arrow landed on {@code EMF_headwear}, {@code EMF_jacket},
	 * {@code EMF_left_sleeve} - never once on the bone underneath.
	 *
	 * <p>That is the wrong thing to attach to. The shell is animated by whatever drew it and need
	 * not move with the bone at all, which is exactly the reported "the arrow is in the head but
	 * does not turn with it". The bone always moves.
	 *
	 * <p>Sized to beat an inflation of half a pixel and nothing more: at a typical four-pixel
	 * standoff the shell is about 2 units of distSq closer than the bone, so 4 per level flips that
	 * reliably while staying far too small to pull an arrow from the head onto the body.
	 */
	private static final float DEPTH_PENALTY = 4.0F;

	/**
	 * Sanity bound on the resolved offset. Deliberately enormous - **do not tighten this.**
	 *
	 * <p>It used to be 1.5 blocks, on the reasoning that no part of a mesh sits further than that
	 * from the bone owning it. That reasoning is wrong. The offset is a coordinate inside a bone's
	 * OWN space, so how large it is depends entirely on where the modeller put that bone's pivot,
	 * and says nothing whatever about whether the answer is right. Entity Model Features authors
	 * parts with pivots at the model origin rather than at the geometry, which puts a perfectly
	 * correct head-hit at 1.88 blocks - just past the old limit. Every arrow on every entity using
	 * such a model was therefore rejected AFTER being resolved correctly, retried 120 times, and
	 * never drawn.
	 *
	 * <p>The real quality test is {@link #MAX_RESOLVE_DIST_SQ}, which measures the distance from
	 * the hit to the actual cube and is the number that means something. This one survives only to
	 * stop a pathological pose injecting a huge translate into the frame.
	 */
	private static final double MAX_LOCAL_OFFSET_BLOCKS = 16.0;

	/**
	 * How many arrows on one entity may be resolved in a single frame. Resolution is the only
	 * step that costs a full model tree walk per arrow, and a backlog arrives all at once - a
	 * player rejoining next to a heavily-shot mob would otherwise pay for every one of its arrows
	 * in the same frame, as a visible hitch. Spread over a few frames it is imperceptible: the
	 * remaining arrows simply appear a frame or two later.
	 */
	private static final int MAX_RESOLVES_PER_FRAME = 4;

	private final ArrowModel arrowModel;
	private final ArrowRenderState arrowRenderState = new ArrowRenderState();

	public StuckArrowRenderLayer(RenderLayerParent<S, M> renderer, EntityRendererProvider.Context context) {
		super(renderer);
		this.arrowModel = new ArrowModel(context.bakeLayer(ModelLayers.ARROW));
	}

	private static final class Best {
		/** What selection compares: {@link #distSq} plus the depth penalty. Never the raw distance. */
		float score = Float.MAX_VALUE;
		float distSq = Float.MAX_VALUE;
		String partPath;
		int cubeIndex = -1;
		Vec3 offsetBlocks;
		Matrix4f poseMatrix;
	}

	@Override
	public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, S state, float yRot, float xRot) {
		List<StuckArrowClientCache.Entry> arrows = ((StuckArrowsAccess) state).betterArrows$getStuckArrows();
		if (arrows.isEmpty()) {
			return;
		}

		// This mod is cosmetic; nothing it can get wrong about one entity's model is worth taking
		// the whole render loop down for. Anything unexpected - a mod-supplied model whose
		// structure breaks an assumption here - degrades to "no arrow drawn on this entity".
		try {
			submitArrows(poseStack, collector, light, state, arrows);
		} catch (Exception e) {
			BetterArrowsMod.logOnce("Failed to render stuck arrows on " + state.getClass().getName(), e);
		}
	}

	private void submitArrows(PoseStack poseStack, SubmitNodeCollector collector, int light, S state,
			List<StuckArrowClientCache.Entry> arrows) {
		// How many (still-unmatched) resolved arrows we're looking for in this traversal - every
		// resolved entry counts once. Entries that never resolved are simply never matched.
		boolean canResolve = LevelRenderTracker.isRenderingLevel();
		int resolveBudget = MAX_RESOLVES_PER_FRAME;
		int remaining = 0;
		for (StuckArrowClientCache.Entry entry : arrows) {
			if (canResolve && resolveBudget > 0 && !entry.isResolved() && !entry.givenUp()) {
				resolveBudget--;
				resolve(entry, poseStack, state);
			}
			if (entry.isResolved()) {
				remaining++;
			}
		}
		if (remaining == 0) {
			return;
		}

		renderMatching(getParentModel().root(), poseStack, "", arrows, collector, light, state, remaining);
	}

	// Mirrors ModelPart#visit's own traversal exactly (same push/translateAndRotate/iterate
	// cubes/recurse into children/pop, same "" -> "/name" -> "/name/childName" path scheme) but
	// stops as soon as every resolved arrow on this entity has been matched, instead of always
	// walking every cube in the model - visit() itself has no way to end early. Correctness must
	// stay identical to the plain visit()-based version this replaced; only the early exit differs.
	private int renderMatching(ModelPart part, PoseStack poseStack, String path, List<StuckArrowClientCache.Entry> arrows,
			SubmitNodeCollector collector, int light, S state, int remaining) {
		ModelPartAccessor accessor = (ModelPartAccessor) (Object) part;
		List<ModelPart.Cube> cubes = accessor.betterArrows$cubes();
		Map<String, ModelPart> children = accessor.betterArrows$children();
		if (cubes.isEmpty() && children.isEmpty()) {
			return remaining;
		}

		poseStack.pushPose();
		part.translateAndRotate(poseStack);

		// Matched on the part path alone. The frozen offset is already expressed in THIS part's
		// space, so which cube it came from does not enter the drawing - and the anchor part is
		// often one that carries no cubes at all (see ANCHOR in searchCubes), which a cube-indexed
		// match could never find.
		for (StuckArrowClientCache.Entry entry : arrows) {
			if (remaining <= 0) {
				break;
			}
			if (path.equals(entry.resolvedPartPath)) {
				renderArrow(poseStack, collector, light, state, entry);
				remaining--;
			}
		}

		if (remaining > 0) {
			String childPath = path + "/";
			for (Map.Entry<String, ModelPart> child : children.entrySet()) {
				if (remaining == 0) {
					break;
				}
				remaining = renderMatching(child.getValue(), poseStack, childPath + child.getKey(), arrows, collector, light, state, remaining);
			}
		}

		poseStack.popPose();
		return remaining;
	}

	/** Rate limit for the draw-time diagnostic below: one line per this many draws. */
	private static final int DEBUG_DRAW_EVERY = 60;

	private static int debugDrawCounter;

	private void renderArrow(PoseStack poseStack, SubmitNodeCollector collector, int light, S state, StuckArrowClientCache.Entry entry) {
		Vec3 offset = entry.boneLocalOffset;
		Vec3 dir = entry.boneLocalDirection;
		if (offset == null || dir == null) {
			return;
		}

		// Some resource packs (custom-entity-model animation packs in particular) end up causing
		// our layer to be invoked more than once for the same entity off a single extraction -
		// refuse to draw the same arrow twice regardless of why, rather than chasing every
		// possible cause. Keyed on the extraction, not on the frame, so a genuinely separate
		// render pass (a shader mod's shadow pass) still gets its own draw.
		long pass = ((StuckArrowsAccess) state).betterArrows$getRenderPass();
		if (entry.lastRenderedPass == pass) {
			return;
		}
		entry.lastRenderedPass = pass;

		if (BetterArrowsConfig.get().debugLogging && ++debugDrawCounter % DEBUG_DRAW_EVERY == 0) {
			// The one thing that cannot be reasoned about from outside: whether the bone the arrow
			// is anchored to actually MOVES. Printed alongside the head yaw it should be moving
			// with, so a few seconds of turning on the spot settles it - if the translation is
			// constant while yaw changes, that bone is static and the anchor is on the wrong node.
			// The bone's ORIENTATION, not its position. Logging the position was useless: a head
			// turns about its own pivot, so the pivot does not move and the numbers stayed constant
			// whether the bone was following the head or not. Its forward axis does move.
			Matrix4f pose = poseStack.last().pose();
			org.joml.Vector3f fwd = pose.transformDirection(new org.joml.Vector3f(0.0F, 0.0F, 1.0F));
			BetterArrowsMod.LOGGER.info("[stuck/draw] part='{}' boneFwd=({}, {}, {}) headYaw={} bodyYaw={}",
					entry.resolvedPartPath, fwd.x(), fwd.y(), fwd.z(), state.yRot, state.bodyRot);
		}

		poseStack.pushPose();
		poseStack.translate(offset.x, offset.y, offset.z);

		// Neutralize whatever scale is baked into the current pose (entity scale, a giant/baby
		// modifier, a slime's own per-instance size multiplier, ...) so the arrow stays a normal,
		// fixed visual size instead of ballooning up on a giant slime or a ghast.
		Vector3f accumulatedScale = new Vector3f();
		poseStack.last().pose().getScale(accumulatedScale);
		// Guard against a momentarily-zero (or non-finite) scale - a "shrinking" effect, some mob
		// death animations - which would otherwise inject Infinity/NaN into the pose matrix and
		// corrupt everything drawn after this point in the frame, not just the arrow.
		poseStack.scale(safeInverse(accumulatedScale.x), safeInverse(accumulatedScale.y), safeInverse(accumulatedScale.z));

		// Rotate the arrow's default facing axis (+X - the axis for which the old atan2-based
		// vanilla formula this replaced applied no extra rotation) directly onto the real flight
		// direction via a single quaternion, instead of two sequential Euler mulPose calls. The
		// old atan2(x,z)/atan2(y,dirXZ) approach degenerates (gimbal-lock style) whenever the
		// horizontal component of the direction gets small - steep up/down shots, or any mob
		// whose body cube is itself oriented near-horizontal (quadrupeds, phantoms) - which showed
		// up as arrows rendering backwards or sideways from their actual trajectory.
		Vector3f dirVec = new Vector3f((float) dir.x, (float) dir.y, (float) dir.z);
		if (dirVec.isFinite() && dirVec.lengthSquared() > 1.0E-12F) {
			dirVec.normalize();
		} else {
			dirVec.set(1.0F, 0.0F, 0.0F);
		}
		poseStack.mulPose(new Quaternionf().rotationTo(1.0F, 0.0F, 0.0F, dirVec.x(), dirVec.y(), dirVec.z()));

		collector.submitModel(
				arrowModel,
				arrowRenderState,
				poseStack,
				ARROW_TEXTURE,
				light,
				OverlayTexture.NO_OVERLAY,
				state.outlineColor,
				null);

		poseStack.popPose();
	}

	private static float safeInverse(float scale) {
		return Float.isFinite(scale) && scale != 0.0F ? 1.0F / scale : 1.0F;
	}

	private void resolve(StuckArrowClientCache.Entry entry, PoseStack poseStack, S state) {
		entry.resolveAttempts++;

		// The hit point was captured relative to the target's own position, so it is turned back
		// into a world point against wherever the target is NOW - see this class's javadoc.
		Vec3 entityPos = new Vec3(state.x, state.y, state.z);

		// Compensate for the target having turned (yBodyRot) between the real, server-side hit
		// and this resolve moment - golems/villagers snap to face an attacker almost immediately,
		// and resolving with today's (already-turned) pose against yesterday's hit point would
		// make the arrow land rotated away from where it actually hit. Rotating the offset (and
		// direction) by the yaw delta first is exactly equivalent to resolving against the
		// entity's pose AT hit time (the per-part chain below the root cancels out of that
		// derivation entirely - only the root's own yaw doesn't).
		float yawDeltaDegrees = Mth.wrapDegrees(entry.hitBodyYaw - state.bodyRot);
		float yawDeltaRadians = yawDeltaDegrees * ((float) Math.PI / 180.0F);
		Vec3 correctedWorldPos = entityPos.add(entry.hitOffset.yRot(yawDeltaRadians));
		Vec3 correctedDirection = entry.hitDirection.yRot(yawDeltaRadians);

		// The pipeline's camera for this frame, not the live one: see LevelRenderTracker.
		Vec3 cameraPos = LevelRenderTracker.entitySubmitCameraPos();
		Vec3 cameraRelativePos = correctedWorldPos.subtract(cameraPos);
		Vector4f targetPos = new Vector4f((float) cameraRelativePos.x, (float) cameraRelativePos.y, (float) cameraRelativePos.z, 1.0F);
		Vector4f targetDir = new Vector4f(
				(float) correctedDirection.x, (float) correctedDirection.y, (float) correctedDirection.z, 0.0F);
		if (!targetPos.isFinite() || !targetDir.isFinite()) {
			return;
		}

		Best best = new Best();
		searchCubes(getParentModel().root(), poseStack, "", targetPos, targetDir, best);

		if (best.partPath == null || best.distSq > MAX_RESOLVE_DIST_SQ) {
			if (BetterArrowsConfig.get().debugLogging) {
				// The invisible failure. An arrow that never resolves is never drawn, so this is
				// the only way to tell "the mod placed it badly" from "the mod gave up on it".
				BetterArrowsMod.LOGGER.info("[stuck] REJECTED part='{}' distSq={} (limit {})",
						best.partPath, best.distSq, MAX_RESOLVE_DIST_SQ);
			}
			return;
		}

		// Direction resolved against the winning cube's own pose too, so it lines up with the
		// bone's local axes the same way vanilla's own outward-direction formula expects.
		Matrix4f winnerInverse = new Matrix4f(best.poseMatrix).invert();
		Vector4f localDir = winnerInverse.transform(new Vector4f(targetDir));
		if (!localDir.isFinite()) {
			return;
		}
		Vec3 direction = new Vec3(localDir.x(), localDir.y(), localDir.z()).normalize();

		if (BetterArrowsConfig.get().debugLogging) {
			BetterArrowsMod.LOGGER.info("[stuck] part='{}' cube={} distSq={} offset={} attempts={}",
					best.partPath, best.cubeIndex, best.distSq, best.offsetBlocks, entry.resolveAttempts);
		}

		if (best.offsetBlocks == null || best.offsetBlocks.length() > MAX_LOCAL_OFFSET_BLOCKS) {
			return;
		}

		entry.resolvedPartPath = best.partPath;
		entry.resolvedCubeIndex = best.cubeIndex;
		entry.boneLocalOffset = best.offsetBlocks;
		entry.boneLocalDirection = direction;
	}

	// Same traversal as renderMatching (and as ModelPart#visit), but scoring each cube against the
	// hit point instead of drawing. Done by hand rather than through visit() so a single malformed
	// cube or non-invertible pose can be skipped instead of aborting the frame, and so both walks
	// provably agree on the (path, cubeIndex) naming a resolution is frozen against.
	private void searchCubes(ModelPart part, PoseStack poseStack, String path, Vector4f targetPos, Vector4f targetDir,
			Best best) {
		searchCubes(part, poseStack, path, targetPos, targetDir, best, "", new Matrix4f(poseStack.last().pose()));
	}

	/**
	 * @param anchorPath the deepest ancestor so far whose name is a VANILLA bone name, and
	 *                   {@code anchorPose} its transform. The arrow is anchored there rather than to
	 *                   the cube's own part, and that distinction is the whole point on a replaced
	 *                   model: Entity Model Features keeps Minecraft's own bones (`head`) and hangs
	 *                   its geometry underneath in nodes it names `EMF_head`. Minecraft applies the
	 *                   head's rotation to the BONE. Anchoring to the EMF node instead leaves the
	 *                   arrow at the mercy of whatever the resource pack does with that node - and
	 *                   Fresh Animations swaps its animation set when the entity is holding
	 *                   something, which is exactly the reported "the arrow stops following the head
	 *                   as soon as a shield is in hand, and follows again when it is put away".
	 *
	 *                   <p>On a vanilla model no name starts with {@code EMF_}, every part is its own
	 *                   anchor, and nothing about this changes.
	 */
	private void searchCubes(ModelPart part, PoseStack poseStack, String path, Vector4f targetPos, Vector4f targetDir,
			Best best, String anchorPath, Matrix4f anchorPose) {
		ModelPartAccessor accessor = (ModelPartAccessor) (Object) part;
		List<ModelPart.Cube> cubes = accessor.betterArrows$cubes();
		Map<String, ModelPart> children = accessor.betterArrows$children();
		if (cubes.isEmpty() && children.isEmpty()) {
			return;
		}

		poseStack.pushPose();
		part.translateAndRotate(poseStack);

		if (!isInjectedNode(path)) {
			anchorPath = path;
			anchorPose = new Matrix4f(poseStack.last().pose());
		}

		if (!cubes.isEmpty()) {
			Matrix4f inverse = new Matrix4f(poseStack.last().pose()).invert();
			if (inverse.isFinite()) {
				Vector4f localPx = inverse.transform(new Vector4f(targetPos))
						.mul(PIXELS_PER_BLOCK, PIXELS_PER_BLOCK, PIXELS_PER_BLOCK, 1.0F);
				if (localPx.isFinite()) {
					for (int cubeIndex = 0; cubeIndex < cubes.size(); cubeIndex++) {
						scoreCube(cubes.get(cubeIndex), path, cubeIndex, localPx, inverse, targetDir, poseStack, best,
								anchorPath, anchorPose);
					}
				}
			}
		}

		String childPath = path + "/";
		for (Map.Entry<String, ModelPart> child : children.entrySet()) {
			searchCubes(child.getValue(), poseStack, childPath + child.getKey(), targetPos, targetDir, best,
					anchorPath, anchorPose);
		}

		poseStack.popPose();
	}

	/** The last segment of a path, i.e. the part's own name. */
	private static String lastSegment(String path) {
		int slash = path.lastIndexOf('/');
		return slash < 0 ? path : path.substring(slash + 1);
	}

	/** A node a model-replacing mod injected under a vanilla bone, rather than a bone itself. */
	private static boolean isInjectedNode(String path) {
		return lastSegment(path).startsWith("EMF_");
	}

	private static void scoreCube(ModelPart.Cube cube, String path, int cubeIndex, Vector4f localPx, Matrix4f inverse,
			Vector4f targetDir, PoseStack poseStack, Best best, String anchorPath, Matrix4f anchorPose) {
		// Never Math.clamp here: it throws on min > max, and a cube built with a negative
		// dimension - which custom-entity-model resource packs and model-replacing mods do emit -
		// has exactly that. A malformed cube is skipped, not crashed on.
		if (!isUsable(cube)) {
			return;
		}

		float clampedX = clamp(localPx.x(), cube.minX, cube.maxX);
		float clampedY = clamp(localPx.y(), cube.minY, cube.maxY);
		float clampedZ = clamp(localPx.z(), cube.minZ, cube.maxZ);
		float dx = localPx.x() - clampedX;
		float dy = localPx.y() - clampedY;
		float dz = localPx.z() - clampedZ;
		float distSq = dx * dx + dy * dy + dz * dz;
		if (!Float.isFinite(distSq) || distSq > MAX_RESOLVE_DIST_SQ) {
			return;
		}
		// Compared on score, not on raw distance - see DEPTH_PENALTY. depth() counts the '/'
		// separators the traversal builds the path from, so root is 0 and /head/hat/EMF_headwear
		// is 3.
		float score = distSq + DEPTH_PENALTY * depth(path);
		if (score >= best.score) {
			return;
		}

		float finalX = clampedX;
		float finalY = clampedY;
		float finalZ = clampedZ;

		// Where the arrow's own path meets this cube, rather than the point on it nearest the hit.
		// Those are the same thing only when the hit already lies on the mesh, and on a model
		// replaced by a resource pack it almost never does: the drawn mesh is inset from the
		// hitbox, so a shot lands a third of a block clear of it and the nearest-point answer
		// drags the arrow sideways to whatever edge happens to be closest. A shot slightly above
		// the middle of the face came out on the chin - which is exactly what "it sits in the
		// hitbox, not in the head" looks like. Following the shot's real path lands it where it
		// went in.
		Vector4f dirLocal4 = inverse.transform(new Vector4f(targetDir));
		if (dirLocal4.isFinite()) {
			Vector3f onPath = distSq == 0.0F
					// Already inside: march BACKWARD to the face it crossed on the way in, so the
					// arrow is not buried in solid geometry (thick mobs, iron golems especially).
					? rayEntryFace(localPx.x(), localPx.y(), localPx.z(),
							dirLocal4.x(), dirLocal4.y(), dirLocal4.z(), cube)
					// Outside: march FORWARD to where it would have entered.
					: rayEnterFromOutside(localPx.x(), localPx.y(), localPx.z(),
							dirLocal4.x(), dirLocal4.y(), dirLocal4.z(), cube);
			if (onPath != null) {
				finalX = onPath.x;
				finalY = onPath.y;
				finalZ = onPath.z;
			}
		}

		// Re-expressed from the cube's own part into the ANCHOR bone's space: out through the cube
		// part's pose into world, back in through the anchor's. On a vanilla model the two are the
		// same part and this is the identity.
		Vector4f anchorLocal = new Vector4f(
				finalX / PIXELS_PER_BLOCK, finalY / PIXELS_PER_BLOCK, finalZ / PIXELS_PER_BLOCK, 1.0F);
		poseStack.last().pose().transform(anchorLocal);
		Matrix4f anchorInverse = new Matrix4f(anchorPose).invert();
		if (!anchorInverse.isFinite()) {
			return;
		}
		anchorInverse.transform(anchorLocal);
		if (!anchorLocal.isFinite()) {
			return;
		}

		best.score = score;
		best.distSq = distSq;
		best.partPath = anchorPath;
		best.cubeIndex = cubeIndex;
		best.offsetBlocks = new Vec3(anchorLocal.x(), anchorLocal.y(), anchorLocal.z());
		best.poseMatrix = new Matrix4f(anchorPose);
	}

	private static int depth(String path) {
		int levels = 0;
		for (int i = 0; i < path.length(); i++) {
			if (path.charAt(i) == '/') {
				levels++;
			}
		}
		return levels;
	}

	private static boolean isUsable(ModelPart.Cube cube) {
		return cube.minX <= cube.maxX && cube.minY <= cube.maxY && cube.minZ <= cube.maxZ
				&& Float.isFinite(cube.minX) && Float.isFinite(cube.maxX)
				&& Float.isFinite(cube.minY) && Float.isFinite(cube.maxY)
				&& Float.isFinite(cube.minZ) && Float.isFinite(cube.maxZ);
	}

	private static float clamp(float value, float min, float max) {
		if (value < min) {
			return min;
		}
		return value > max ? max : value;
	}

	/**
	 * Slab-method AABB raycast marching FORWARD from a point outside the cube, to where the arrow's
	 * path first meets it. Null if the path misses the cube entirely - the caller then keeps the
	 * plain nearest-point answer.
	 */
	private static Vector3f rayEnterFromOutside(float px, float py, float pz, float dx, float dy, float dz,
			ModelPart.Cube cube) {
		float tEntry = Float.NEGATIVE_INFINITY;
		float tExit = Float.POSITIVE_INFINITY;

		if (Math.abs(dx) > 1.0E-6F) {
			float t1 = (cube.minX - px) / dx;
			float t2 = (cube.maxX - px) / dx;
			tEntry = Math.max(tEntry, Math.min(t1, t2));
			tExit = Math.min(tExit, Math.max(t1, t2));
		} else if (px < cube.minX || px > cube.maxX) {
			return null;
		}

		if (Math.abs(dy) > 1.0E-6F) {
			float t1 = (cube.minY - py) / dy;
			float t2 = (cube.maxY - py) / dy;
			tEntry = Math.max(tEntry, Math.min(t1, t2));
			tExit = Math.min(tExit, Math.max(t1, t2));
		} else if (py < cube.minY || py > cube.maxY) {
			return null;
		}

		if (Math.abs(dz) > 1.0E-6F) {
			float t1 = (cube.minZ - pz) / dz;
			float t2 = (cube.maxZ - pz) / dz;
			tEntry = Math.max(tEntry, Math.min(t1, t2));
			tExit = Math.min(tExit, Math.max(t1, t2));
		} else if (pz < cube.minZ || pz > cube.maxZ) {
			return null;
		}

		if (tExit < 0.0F || tEntry > tExit || !Float.isFinite(tEntry)) {
			return null;
		}

		float t = Math.max(tEntry, 0.0F);
		return new Vector3f(px + dx * t, py + dy * t, pz + dz * t);
	}

	// Standard slab-method AABB raycast, marching backward from a point already inside the cube
	// along -direction, to find the face the arrow's real flight path crossed first (its entry
	// face). Returns null if the point can't reach a face this way (direction parallel to an
	// axis it's already outside on, or similar degenerate case) - caller falls back to the plain
	// clamped point in that case.
	private static Vector3f rayEntryFace(float px, float py, float pz, float dx, float dy, float dz, ModelPart.Cube cube) {
		float bx = -dx;
		float by = -dy;
		float bz = -dz;
		float tEntry = Float.NEGATIVE_INFINITY;
		float tExit = Float.POSITIVE_INFINITY;

		if (Math.abs(bx) > 1.0E-6F) {
			float t1 = (cube.minX - px) / bx;
			float t2 = (cube.maxX - px) / bx;
			tEntry = Math.max(tEntry, Math.min(t1, t2));
			tExit = Math.min(tExit, Math.max(t1, t2));
		} else if (px < cube.minX || px > cube.maxX) {
			return null;
		}

		if (Math.abs(by) > 1.0E-6F) {
			float t1 = (cube.minY - py) / by;
			float t2 = (cube.maxY - py) / by;
			tEntry = Math.max(tEntry, Math.min(t1, t2));
			tExit = Math.min(tExit, Math.max(t1, t2));
		} else if (py < cube.minY || py > cube.maxY) {
			return null;
		}

		if (Math.abs(bz) > 1.0E-6F) {
			float t1 = (cube.minZ - pz) / bz;
			float t2 = (cube.maxZ - pz) / bz;
			tEntry = Math.max(tEntry, Math.min(t1, t2));
			tExit = Math.min(tExit, Math.max(t1, t2));
		} else if (pz < cube.minZ || pz > cube.maxZ) {
			return null;
		}

		if (tExit < 0.0F || tEntry > tExit || !Float.isFinite(tExit)) {
			return null;
		}

		float t = Math.max(tExit, 0.0F);
		return new Vector3f(px + bx * t, py + by * t, pz + bz * t);
	}
}
