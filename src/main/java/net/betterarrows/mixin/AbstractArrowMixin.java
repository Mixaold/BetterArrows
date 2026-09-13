package net.betterarrows.mixin;

import net.betterarrows.BetterArrowsMod;
import net.betterarrows.StuckArrowRecord;
import net.betterarrows.config.BetterArrowsConfig;
import net.betterarrows.network.ArrowStuckPayload;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Captures the exact {@link EntityHitResult} position vanilla computes on hit and immediately
 * discards - see the projectile collision research: {@code onHitEntity} is the only place this
 * ever exists, server-side, for a single method call. We only observe it here, we don't compute
 * anything new, so this adds no recurring server tick cost.
 *
 * <p>The capture is deliberately split across HEAD and RETURN and commits only if the target's
 * arrow count actually went UP, because that - and nothing else - is vanilla's own definition of
 * "this arrow stuck". Recording at HEAD instead is wrong in several common cases where vanilla
 * bounces the arrow off and sticks nothing:
 *
 * <ul>
 *   <li>invulnerability frames - a follow-up arrow landing within a second of the last one does no
 *       damage and is deflected. This is the big one: with rapid fire, most arrows legitimately do
 *       not stick, and recording them anyway produced a record for the arrow that bounced while
 *       the client-side clamp against {@code getArrowCount()} then dropped the OLDEST record - the
 *       one that had really stuck. Visible as "some arrows show, some don't".</li>
 *   <li>piercing arrows - vanilla never increments the count and never discards the arrow.</li>
 *   <li>endermen - vanilla returns right after damage, before touching the count.</li>
 *   <li>anything else that makes {@code hurtOrSimulate} return false (invulnerable targets, a
 *       protected player, a mod cancelling the damage).</li>
 * </ul>
 *
 * <p>Reading the count on both sides covers all of them at once, and keeps covering whatever
 * vanilla adds later, instead of re-listing vanilla's conditions here and drifting out of sync.
 */
@Mixin(AbstractArrow.class)
public abstract class AbstractArrowMixin {
	/**
	 * Pull the anchor back a little along the direction the arrow came from, so at least part of
	 * the shaft clears the surface instead of the whole arrow burying itself - many mobs' hitbox
	 * is looser than their visual mesh (pandas especially), so the literal collision point can
	 * land well inside the model.
	 */
	@Unique
	private static final double BETTER_ARROWS$ANCHOR_PULLBACK = 0.2;

	/** Target's arrow count as it was before vanilla handled this hit, or -1 if not applicable. */
	@Unique
	private int betterArrows$arrowCountBeforeHit = -1;

	/**
	 * Stops an arrow bouncing off a mob that is still in its invulnerability frames.
	 *
	 * <p>Vanilla's rule (LivingEntity, ~line 1210): for a second of real time after a hit, damage
	 * that is no greater than the last hit is refused outright. {@code hurtOrSimulate} then returns
	 * false, and the arrow is deflected back at the shooter instead of sticking - which reads as a
	 * glitch, because the shot visibly connected. Bow damage varies with draw strength, so whether
	 * a follow-up arrow sticks or pings off looks purely random.
	 *
	 * <p>Reporting the hit as landed makes vanilla take its normal "stuck" path: the arrow is
	 * consumed, counted and drawn. No damage is added - vanilla already decided this shot does
	 * none, and that decision is left alone. Only the bounce goes away.
	 *
	 * <p>Narrow on purpose. This fires ONLY for invulnerability frames, so a shot genuinely
	 * refused for another reason - a blocked shield, a creative-mode player, a mod cancelling the
	 * damage - still deflects exactly as it always did.
	 */
	@WrapOperation(
			method = "onHitEntity",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/entity/Entity;hurtOrSimulate(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
	private boolean betterArrows$noInvulnerabilityBounce(
			Entity target, DamageSource source, float damage, Operation<Boolean> original) {
		boolean hurt = original.call(target, source, damage);
		if (hurt || !BetterArrowsConfig.get().stickThroughInvulnerability.enabled) {
			return hurt;
		}
		if (!(target instanceof LivingEntity living) || target.level().isClientSide()) {
			return false;
		}
		// invulnerableTime > 10 is exactly vanilla's own test for "still cooling down from the
		// last hit"; anything else that refused the damage is none of our business.
		if (living.invulnerableTime <= 10 || living.getItemBlockingWith() != null) {
			return false;
		}
		return true;
	}

	@Inject(method = "onHitEntity", at = @At("HEAD"))
	private void betterArrows$noteArrowCountBeforeHit(EntityHitResult hitResult, CallbackInfo ci) {
		this.betterArrows$arrowCountBeforeHit = -1;
		AbstractArrow self = (AbstractArrow) (Object) this;
		if (self.level().isClientSide()) {
			return;
		}
		if (hitResult.getEntity() instanceof LivingEntity target) {
			this.betterArrows$arrowCountBeforeHit = target.getArrowCount();
		}
	}

	@Inject(method = "onHitEntity", at = @At("RETURN"))
	private void betterArrows$captureHitPoint(EntityHitResult hitResult, CallbackInfo ci) {
		int before = this.betterArrows$arrowCountBeforeHit;
		this.betterArrows$arrowCountBeforeHit = -1;
		if (before < 0) {
			return;
		}
		if (!(hitResult.getEntity() instanceof LivingEntity target)) {
			return;
		}
		if (target.getArrowCount() <= before) {
			// Vanilla did not stick this arrow - see the class javadoc. Nothing to draw.
			return;
		}

		// This mod is purely cosmetic: nothing it can get wrong is worth taking a server tick
		// (and with it the world) down for, so a failure here degrades to "no fancy arrow".
		// Wound particles moved out to the standalone "Simple Mob Particles" mod - it derives them
		// from the generic AFTER_DAMAGE/AFTER_DEATH events (which fire for this hit too, since an
		// arrow's damage still runs through hurtOrSimulate), so nothing needs to be called from
		// here any more, and this mod works with or without that one installed.
		AbstractArrow self = (AbstractArrow) (Object) this;
		if (!BetterArrowsConfig.get().stuckArrows.enabled) {
			return;
		}
		try {
			betterArrows$record(self, target, hitResult);
		} catch (Exception e) {
			BetterArrowsMod.logOnce("Failed to capture a stuck-arrow hit point", e);
		}
	}

	@Unique
	private static void betterArrows$record(AbstractArrow self, LivingEntity target, EntityHitResult hitResult) {
		// Unchanged since HEAD: on the path where vanilla sticks the arrow it never touches the
		// arrow's motion, and knockback moves the target's velocity, not its position.
		Vec3 direction = self.getDeltaMovement().normalize();
		if (direction.lengthSqr() < 1.0E-8) {
			// Vec3#normalize returns ZERO for a degenerate vector - without a real flight
			// direction there is nothing to orient the arrow along, so leave it to vanilla.
			return;
		}

		// Stored RELATIVE to the target, never as an absolute world point: the target keeps moving
		// after the hit and a client may only resolve this much later (rejoin, re-entering render
		// distance). Resolving an absolute point against a position the entity has since walked
		// away from puts the arrow nowhere near the real wound.
		Vec3 hitOffset = hitResult.getLocation()
				.subtract(direction.scale(BETTER_ARROWS$ANCHOR_PULLBACK))
				.subtract(target.position());
		if (!betterArrows$isFinite(hitOffset) || !betterArrows$isFinite(direction)) {
			return;
		}

		long hitId = ThreadLocalRandom.current().nextLong();
		float bodyYaw = target.yBodyRot;

		ArrowStuckPayload payload = new ArrowStuckPayload(hitId, target.getId(), hitOffset, direction, bodyYaw);
		for (ServerPlayer player : PlayerLookup.tracking(target)) {
			// Guarded: a server running this mod can perfectly well be joined by a client that
			// doesn't have it (or has it disabled). Pushing a payload whose channel the client
			// never registered is at best wasted bandwidth and at worst a disconnect.
			if (ServerPlayNetworking.canSend(player, ArrowStuckPayload.TYPE)) {
				ServerPlayNetworking.send(player, payload);
			}
		}

		// Remembered so a player who starts tracking this target LATER (rejoining, walking back
		// into range) gets caught up too - see BetterArrowsMod's START_TRACKING listener.
		// Copy-then-set, not in-place mutation: a list decoded from persisted NBT via the
		// attachment's Codec comes back immutable (java.util.ImmutableCollections), so .add()-ing
		// directly onto it throws UnsupportedOperationException the moment an entity that already
		// had stuck arrows before a save/reload gets hit again.
		List<StuckArrowRecord> existing = target.getAttachedOrElse(BetterArrowsMod.STUCK_ARROWS, List.of());
		// Hard cap, oldest dropped first. Without it a long-lived, heavily-shot entity grows this
		// list (and the NBT it is saved into, and the packet burst every player joining pays for)
		// without bound. LivingEntityArrowCountMixin normally trims well below this as vanilla's
		// own arrow count decays; this is the backstop for anything that bypasses that.
		int keepFrom = Math.max(0, existing.size() + 1 - BetterArrowsMod.MAX_STUCK_ARROWS);
		List<StuckArrowRecord> updated = new ArrayList<>(existing.subList(keepFrom, existing.size()));
		updated.add(new StuckArrowRecord(hitId, hitOffset, direction, bodyYaw));
		target.setAttached(BetterArrowsMod.STUCK_ARROWS, List.copyOf(updated));
	}

	@Unique
	private static boolean betterArrows$isFinite(Vec3 v) {
		return Double.isFinite(v.x) && Double.isFinite(v.y) && Double.isFinite(v.z);
	}
}
