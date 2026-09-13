package net.betterarrows.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.betterarrows.BetterArrowsMod;
import net.betterarrows.config.BetterArrowsConfig;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Everything that happens the moment an arrow meets a block: the sound of the material it actually
 * hit, crumble particles carrying that block's real texture, and the glancing-angle ricochet.
 *
 * <p>All three live in one mixin because all three hook {@code onHitBlock} and the ricochet
 * decision has to be made before either of the others: a bounce is not a landing, so it gets its
 * own quieter sound and never runs the landing particles.
 */
@Mixin(AbstractArrow.class)
public abstract class AbstractArrowImpactMixin {
	/** Sentinel for "this mod is not touching the impact sound, let vanilla be". */
	@Unique
	private static final float BETTER_ARROWS$SOUND_UNTOUCHED = -1.0F;

	/** How far out of the surface a bouncing arrow is nudged, so it starts the next tick clear of it. */
	@Unique
	private static final double BETTER_ARROWS$BOUNCE_CLEARANCE = 0.05;

	@Unique
	private float betterArrows$impactSoundVolume = BETTER_ARROWS$SOUND_UNTOUCHED;

	@Unique
	private int betterArrows$bounces;

	@Unique
	private int betterArrows$slides;

	@Unique
	private int betterArrows$lastSlideTick = Integer.MIN_VALUE;

	/**
	 * How each material answers an arrow, as two separate numbers - because it is two separate
	 * questions, and one factor cannot honestly answer both:
	 *
	 * <ul>
	 *   <li><b>[0] angle</b> scales the glancing-angle window: how shallow a shot has to be before
	 *       it skips at all. Stone takes a hit well off the perpendicular; loose ground needs the
	 *       arrow to be travelling almost along the face.</li>
	 *   <li><b>[1] energy</b> scales the speed that survives the skip - the material's own share of
	 *       that answer, not the whole of it. How glancing the hit was is charged separately, the same
	 *       way for every bounce this mod makes, in {@code betterArrows$bounceOff}; the two multiply.
	 *       So this number asks only "how much does this stuff swallow", at a fixed angle. Loose
	 *       ground sits high for a reason the angle factor cannot express on its own: it is not that
	 *       sand is springy, it is that the only shots sand ever lets bounce are the ones that never
	 *       really entered it - and across a window a few degrees wide the cosine barely moves.</li>
	 * </ul>
	 *
	 * <p>Keyed on the block's own {@code SoundType}, so it covers modded blocks for free: almost all
	 * of them reuse one of vanilla's sound types, and anything that doesn't falls back to a middling
	 * default. An angle factor of 0 means the arrow always buries itself.
	 */
	@Unique
	private static final Map<SoundType, float[]> BETTER_ARROWS$BOUNCE = betterArrows$buildBounceTable();

	@Unique
	private static final float[] BETTER_ARROWS$DEFAULT_BOUNCE = { 0.5F, 0.5F };

	@Unique
	private static final int BETTER_ARROWS$ANGLE = 0;

	@Unique
	private static final int BETTER_ARROWS$ENERGY = 1;

	@Unique
	private static Map<SoundType, float[]> betterArrows$buildBounceTable() {
		Map<SoundType, float[]> map = new IdentityHashMap<>();
		// Hard and ringing - an arrow skips off these readily, and leaves fast.
		betterArrows$bounceGroup(map, 1.0F, 1.0F,
				SoundType.STONE, SoundType.METAL, SoundType.IRON, SoundType.GLASS, SoundType.ANVIL,
				SoundType.NETHERITE_BLOCK, SoundType.ANCIENT_DEBRIS, SoundType.LODESTONE, SoundType.CHAIN,
				SoundType.COPPER, SoundType.COPPER_BULB, SoundType.COPPER_GRATE, SoundType.LANTERN,
				SoundType.AMETHYST, SoundType.AMETHYST_CLUSTER, SoundType.CALCITE, SoundType.TUFF,
				SoundType.TUFF_BRICKS, SoundType.POLISHED_TUFF, SoundType.DEEPSLATE, SoundType.DEEPSLATE_BRICKS,
				SoundType.DEEPSLATE_TILES, SoundType.POLISHED_DEEPSLATE, SoundType.BASALT, SoundType.NETHER_BRICKS,
				SoundType.NETHERRACK, SoundType.BONE_BLOCK, SoundType.DRIPSTONE_BLOCK, SoundType.GILDED_BLACKSTONE,
				SoundType.NETHER_ORE, SoundType.NETHER_GOLD_ORE, SoundType.SPAWNER, SoundType.VAULT,
				SoundType.TRIAL_SPAWNER, SoundType.HEAVY_CORE);
		// Wood gives a little - it bites, but a shallow enough shot still skips.
		betterArrows$bounceGroup(map, 0.55F, 0.55F,
				SoundType.WOOD, SoundType.BAMBOO_WOOD, SoundType.NETHER_WOOD, SoundType.CHERRY_WOOD,
				SoundType.BAMBOO, SoundType.LADDER, SoundType.SCAFFOLDING, SoundType.STEM,
				SoundType.HANGING_SIGN, SoundType.CHISELED_BOOKSHELF, SoundType.SHELF, SoundType.PACKED_MUD,
				SoundType.MUD_BRICKS, SoundType.RESIN_BRICKS);
		// Loose ground - sand, gravel, dirt. Anything arriving with even a little downward bite is
		// swallowed, so the window is a sliver: you have to be shooting nearly flat ALONG the surface
		// for this to trigger at all. What does skip keeps most of its speed, though, precisely
		// because at that angle the arrow hardly presses into the ground - it clips the top of it.
		betterArrows$bounceGroup(map, 0.25F, 0.80F,
				SoundType.SAND, SoundType.SUSPICIOUS_SAND, SoundType.GRAVEL, SoundType.SUSPICIOUS_GRAVEL,
				SoundType.GRASS, SoundType.ROOTED_DIRT);
		// Soft and swallowing - the arrow simply sticks, at any angle. Snow is here deliberately: it
		// is a thin top layer over whatever it is sitting on, and bouncing off it reads as the arrow
		// missing the ground entirely.
		betterArrows$bounceGroup(map, 0.0F, 0.0F,
				SoundType.SNOW, SoundType.POWDER_SNOW, SoundType.WOOL, SoundType.WET_GRASS,
				SoundType.MUD, SoundType.MUDDY_MANGROVE_ROOTS, SoundType.SOUL_SAND,
				SoundType.SOUL_SOIL, SoundType.SLIME_BLOCK, SoundType.HONEY_BLOCK, SoundType.SPONGE,
				SoundType.WET_SPONGE, SoundType.COBWEB, SoundType.MOSS, SoundType.MOSS_CARPET);
		return map;
	}

	/** One array per group rather than per material: it is read-only, so the whole group can share it. */
	@Unique
	private static void betterArrows$bounceGroup(
			Map<SoundType, float[]> map, float angle, float energy, SoundType... types) {
		float[] pair = { angle, energy };
		for (SoundType type : types) {
			map.put(type, pair);
		}
	}

	@Inject(method = "onHitBlock", at = @At("HEAD"), cancellable = true)
	private void betterArrows$onImpact(BlockHitResult hitResult, CallbackInfo ci) {
		AbstractArrow self = (AbstractArrow) (Object) this;
		this.betterArrows$impactSoundVolume = BETTER_ARROWS$SOUND_UNTOUCHED;

		// Cosmetic and physics-flavour work only: a failure here must never cost a server tick.
		try {
			BlockState state = self.level().getBlockState(hitResult.getBlockPos());

			// Ricochet is decided server-side only and relies on needsSync to correct the client,
			// exactly as vanilla's own ProjectileDeflection does. Deciding it on both sides instead
			// would desync the moment a client's config disagreed with the server's.
			// Slide is tried first. Ice reports its sound type as GLASS, and GLASS is listed as hard
			// and ringing, so without this the ricochet below happily pings arrows off a frozen lake.
			if (!self.level().isClientSide() && betterArrows$trySlide(self, hitResult, state)) {
				ci.cancel();
				return;
			}

			// Ricochet is never allowed on a block arrows are meant to SLIDE on. If the slide above
			// declined - it ran out of contacts, or the arrow was too slow to be worth sliding - the
			// answer is that the arrow stops there, not that it springs off. Falling through to the
			// bouncer was the reason arrows pinged strangely and a long way off ice: ice reports its
			// sound type as GLASS, and GLASS is rated as hard and ringing, bouncing at full strength.
			if (!self.level().isClientSide() && !state.is(BetterArrowsMod.ARROWS_SLIDE_ON)
					&& betterArrows$tryRicochet(self, hitResult, state)) {
				ci.cancel();
				return;
			}

			betterArrows$chooseImpactSound(self, state);
		} catch (Exception e) {
			BetterArrowsMod.logOnce("Failed to handle an arrow's block impact", e);
		}
	}

	/**
	 * Swaps in the sound of the material actually hit. Deliberately NOT gated on the logical side:
	 * we only change WHICH sound vanilla plays, never when or how many times, so whatever vanilla
	 * does about client/server here keeps doing it.
	 */
	@Unique
	private void betterArrows$chooseImpactSound(AbstractArrow self, BlockState state) {
		BetterArrowsConfig.SurfaceSounds config = BetterArrowsConfig.get().surfaceSounds;
		if (!config.enabled) {
			return;
		}
		if (state.is(BetterArrowsMod.NO_IMPACT_SOUND)) {
			// A bell is already ringing, amethyst is already chiming. Adding a material knock on
			// top of that is noise, so this mod says nothing at all here.
			this.betterArrows$impactSoundVolume = 0.0F;
			return;
		}
		self.setSoundEvent(state.getSoundType().getStepSound());
		this.betterArrows$impactSoundVolume = config.volume;
	}

	/**
	 * Vanilla plays the landing sound at full volume. The material sounds are meant to be a detail
	 * you notice standing next to the arrow, not an announcement, so the volume is replaced here -
	 * and dropped entirely for blocks that make their own noise.
	 */
	@WrapOperation(
			method = "onHitBlock",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/entity/projectile/arrow/AbstractArrow;playSound(Lnet/minecraft/sounds/SoundEvent;FF)V"))
	private void betterArrows$quieterImpactSound(
			AbstractArrow self, SoundEvent sound, float volume, float pitch, Operation<Void> original) {
		float chosen = this.betterArrows$impactSoundVolume;
		if (chosen == BETTER_ARROWS$SOUND_UNTOUCHED) {
			original.call(self, sound, volume, pitch);
		} else if (chosen > 0.0F) {
			original.call(self, sound, chosen, pitch);
		}
		// chosen == 0: this block speaks for itself, stay quiet.
	}

	@Inject(method = "onHitBlock", at = @At("RETURN"))
	private void betterArrows$landingParticles(BlockHitResult hitResult, CallbackInfo ci) {
		AbstractArrow self = (AbstractArrow) (Object) this;
		if (!(self.level() instanceof ServerLevel level)) {
			return;
		}
		try {
			BlockState state = level.getBlockState(hitResult.getBlockPos());
			betterArrows$spawnBlockParticles(level, state, hitResult, 1.0F);
		} catch (Exception e) {
			BetterArrowsMod.logOnce("Failed to spawn arrow impact particles", e);
		}
	}

	/**
	 * Crumble particles carrying the real texture of the block that was hit - vanilla's own
	 * {@code ParticleTypes.BLOCK} already does exactly that, no custom particle needed.
	 * Scaled by how hard the arrow arrived so a nearly-spent arrow does not spray debris.
	 */
	@Unique
	private static void betterArrows$spawnBlockParticles(
			ServerLevel level, BlockState state, BlockHitResult hitResult, float intensity) {
		BetterArrowsConfig.ImpactParticles.Block config = BetterArrowsConfig.get().impactParticles.block;
		if (!config.enabled || state.isAir()) {
			return;
		}
		int count = Math.round(config.count * intensity);
		if (count <= 0) {
			return;
		}

		Vec3 at = hitResult.getLocation();
		// Nudged out along the face so the particles are not born inside the block, where they
		// would be invisible.
		Vec3 normal = hitResult.getDirection().getUnitVec3();
		Vec3 spawn = at.add(normal.scale(0.1));
		level.sendParticles(
				new BlockParticleOption(ParticleTypes.BLOCK, state),
				spawn.x, spawn.y, spawn.z,
				count,
				normal.x * 0.1, normal.y * 0.1, normal.z * 0.1,
				0.05);
	}

	/**
	 * An arrow that arrives shallow on ice keeps going ALONG it instead of springing off.
	 *
	 * <p>The whole trick is that only the component INTO the surface is removed; the component
	 * along it survives, minus a little. Gravity then presses the arrow back down onto the ice on
	 * the next tick, it grazes again, and loses a little more - so the slide and its decay both
	 * fall out of running this same method once per tick. There is no tick hook and no sliding
	 * state to keep: an arrow that leaves the edge of the ice simply stops hitting a block and
	 * flies on, and one that meets a wall hits a different block and sticks in it.
	 *
	 * <p>Sound and particles are deliberately gated on the speed INTO the surface, which is large
	 * on the shot that starts the slide and almost nothing (one tick of gravity) on every graze
	 * after it. Without that gate a sliding arrow machine-guns the ice sound twenty times a second.
	 */
	@Unique
	private boolean betterArrows$trySlide(AbstractArrow self, BlockHitResult hitResult, BlockState state) {
		BetterArrowsConfig.IceSlide config = BetterArrowsConfig.get().iceSlide;
		if (!config.enabled || !state.is(BetterArrowsMod.ARROWS_SLIDE_ON)) {
			return false;
		}
		if (this.betterArrows$slides >= config.maxSlides) {
			// Backstop only - speedKept should have stopped it long ago. Let it stick.
			return false;
		}

		Vec3 velocity = self.getDeltaMovement();
		double speed = velocity.length();
		if (!Double.isFinite(speed) || speed <= 0.0) {
			return false;
		}

		Vec3 normal = hitResult.getDirection().getUnitVec3();
		double alongNormal = velocity.dot(normal);
		if (alongNormal >= 0.0) {
			// Travelling away from the face it supposedly hit - nothing to slide along.
			return false;
		}

		double angleFromSurfaceDegrees = betterArrows$angleFromSurface(alongNormal, speed);
		// Underwater there is no skating: the water is already dragging the arrow to a halt, and a
		// spent arrow skidding along a submerged block reached places nothing should have. But the
		// rule that an arrow cannot bury itself in ice still holds down there - gating the whole
		// method on water is what let ricocheted arrows lodge in submerged ice again - so a wet hit
		// falls through to the same "thrown off, never embedded" path a steep one takes.
		if (angleFromSurfaceDegrees > config.maxAngleDegrees || self.isInWater()) {
			// Too steep to skate - but an arrow still cannot bury itself in ice, so it is thrown off
			// rather than allowed to stick. Weakly: each bounce sheds most of the speed, and once
			// there is nothing left the arrow is handed back to vanilla and simply lies on the
			// surface. That last step is what keeps this from becoming an arrow that jitters against
			// the ice forever and never despawns, since vanilla only ages arrows that have landed.
			if (speed < config.minSpeed) {
				if (hitResult.getDirection() == Direction.UP) {
					// Spent, on a surface it can rest on. Let vanilla land it: an arrow lying flat on
					// a frozen lake looks right, and it is what stops this becoming an arrow that
					// nudges at the ice forever and never despawns - vanilla only ages ones that
					// have landed.
					return false;
				}
				// Spent against a wall or the underside of ice. Nothing to rest on and nothing to
				// embed in, so it is eased off the face and left to fall until it finds something
				// that is not ice.
				self.setPos(hitResult.getLocation().add(normal.scale(BETTER_ARROWS$BOUNCE_CLEARANCE)));
				self.setDeltaMovement(normal.scale(0.02).add(0.0, -0.05, 0.0));
				self.needsSync = true;
				this.betterArrows$slides++;
				return true;
			}
			state.onProjectileHit(self.level(), state, hitResult, self);
			if (!self.isAlive()) {
				return true;
			}
			self.setPos(hitResult.getLocation().add(normal.scale(BETTER_ARROWS$BOUNCE_CLEARANCE)));
			// hardBounceKept is the ceiling and nothing more: the angle is charged inside bounceOff,
			// which is why an arrow fired straight down into ice at point blank no longer comes back
			// up at a third of its speed like a rubber ball. A square-on impact is exactly the case
			// with nothing left to bounce with - all of it went into the hit.
			self.setDeltaMovement(betterArrows$bounceOff(
					velocity, normal, alongNormal, angleFromSurfaceDegrees, config.hardBounceKept));
			self.needsSync = true;
			this.betterArrows$slides++;
			betterArrows$playBounceEffects(self, state, hitResult, (float) (speed / 3.0));
			return true;
		}

		// Drop the component into the surface entirely. This is the one line that makes it a slide
		// rather than a bounce - a reflection would negate it instead of discarding it.
		// Only PART of the component into the surface is cancelled - see normalKept. Cancelling all
		// of it is what made an arrow leave the edge of a frozen lake gliding dead flat.
		//
		// Friction is charged per TICK, not per contact, and that distinction is not academic. This
		// method only runs when the arrow actually touches the ice, and it does not touch it every
		// tick: damping the vertical speed leaves it smaller than the clearance the arrow is lifted
		// by, so the next tick it does not reach the block at all and contact happens roughly every
		// OTHER tick. Charging once per contact therefore took about half the speed it was meant to,
		// and an arrow left a ten-block run of ice still travelling fast enough to clear ten blocks
		// of water and land on the far side. Raising it to the power of the ticks elapsed makes the
		// loss what the setting says it is, whatever the contact cadence turns out to be.
		int elapsed = Mth.clamp(self.tickCount - this.betterArrows$lastSlideTick, 1, 5);
		double kept = Math.pow(config.speedKept, elapsed);
		Vec3 along = velocity.subtract(normal.scale(alongNormal * (1.0 - config.normalKept))).scale(kept);
		if (along.length() < config.minSpeed) {
			// Spent. Let vanilla stick it where it lies, so it can still be picked up.
			return false;
		}

		// Vanilla's block reactions live in onHitBlock, which this cancels - run them by hand or a
		// shot across a frozen target would silently fail to score.
		state.onProjectileHit(self.level(), state, hitResult, self);
		if (!self.isAlive()) {
			return true;
		}

		self.setPos(hitResult.getLocation().add(normal.scale(BETTER_ARROWS$BOUNCE_CLEARANCE)));
		self.setDeltaMovement(along);
		// Same signal vanilla's own ProjectileDeflection raises: without it the client keeps
		// predicting the arrow into the ice and the slide reads as a stutter.
		self.needsSync = true;
		this.betterArrows$slides++;
		this.betterArrows$lastSlideTick = self.tickCount;

		// Only the arrival is loud - see the javadoc.
		if (-alongNormal > config.minSpeed) {
			betterArrows$playBounceEffects(self, state, hitResult, (float) (speed / 3.0));
		}
		return true;
	}

	/**
	 * A glancing hit skips off the surface instead of planting itself. Steep hits are left to
	 * vanilla, which is what keeps the mod feeling like Minecraft: shooting a wall in front of you
	 * still sticks the arrow exactly where it always did.
	 *
	 * @return true if the arrow bounced and vanilla's landing must be skipped
	 */
	@Unique
	private boolean betterArrows$tryRicochet(AbstractArrow self, BlockHitResult hitResult, BlockState state) {
		BetterArrowsConfig.Ricochet config = BetterArrowsConfig.get().ricochet;
		if (!config.enabled || this.betterArrows$bounces >= config.maxBounces) {
			return false;
		}

		Vec3 velocity = self.getDeltaMovement();
		double speed = velocity.length();
		if (!(speed >= config.minSpeed) || !Double.isFinite(speed)) {
			// A nearly-spent arrow skittering off a wall reads as a glitch, not as physics.
			return false;
		}

		// What the arrow hit matters as much as how it hit: sand swallows an arrow that stone
		// would throw straight back off.
		float[] bounce = BETTER_ARROWS$BOUNCE.getOrDefault(state.getSoundType(), BETTER_ARROWS$DEFAULT_BOUNCE);
		if (bounce[BETTER_ARROWS$ANGLE] <= 0.0F) {
			return false;
		}

		// Leaves answer to the same sound type as dirt and grass, so the loose-ground window would
		// otherwise let a flat shot skip off a tree canopy. An arrow standing in leaves is what
		// everyone expects to see, so leaves keep vanilla's answer.
		if (state.is(BlockTags.LEAVES)) {
			return false;
		}

		Direction face = hitResult.getDirection();
		Vec3 normal = face.getUnitVec3();
		double alongNormal = velocity.dot(normal);
		if (alongNormal >= 0.0) {
			// Travelling away from the face it supposedly hit - nothing sensible to reflect.
			return false;
		}

		double angleFromSurfaceDegrees = betterArrows$angleFromSurface(alongNormal, speed);
		if (angleFromSurfaceDegrees > config.maxGlancingAngleDegrees * bounce[BETTER_ARROWS$ANGLE]) {
			return false;
		}

		// Vanilla's block reactions live in onHitBlock, which we are about to cancel - so run them
		// by hand first, or a glancing shot would silently fail to ring a bell, light a candle or
		// score a target.
		state.onProjectileHit(self.level(), state, hitResult, self);
		if (!self.isAlive()) {
			// The block consumed the arrow (a decorated pot shattering, say). Nothing left to bounce.
			return true;
		}

		// The config's factor times the material's share, with the angle charged on top of both
		// inside bounceOff: a skip taken at the very edge of the window leaves slower than one that
		// arrived flat along the face.
		Vec3 reflected = betterArrows$bounceOff(velocity, normal, alongNormal, angleFromSurfaceDegrees,
				config.energyRetained * bounce[BETTER_ARROWS$ENERGY]);
		self.setPos(hitResult.getLocation().add(normal.scale(BETTER_ARROWS$BOUNCE_CLEARANCE)));
		self.setDeltaMovement(reflected);
		// Same signal vanilla's own ProjectileDeflection raises: push the corrected flight to
		// clients instead of letting them keep predicting the arrow into the wall.
		self.needsSync = true;
		this.betterArrows$bounces++;

		betterArrows$playBounceEffects(self, state, hitResult, (float) (speed / 3.0));
		return true;
	}

	/**
	 * The angle the arrow arrived at, measured from the SURFACE rather than from its normal: 0 is
	 * travelling flat along the face, 90 is straight into it.
	 *
	 * <p>One method rather than the same two lines in both callers, because the convention is the
	 * part that matters here. The slide window, the ricochet window and the speed kept through a
	 * bounce all read this number, and a branch that quietly measured it from the normal instead
	 * would still produce plausible-looking angles while meaning the opposite.
	 */
	@Unique
	private static double betterArrows$angleFromSurface(double alongNormal, double speed) {
		// 90 - acos(x) is asin(x), so this is just the incidence cosine read as an angle.
		return Math.toDegrees(Math.asin(Math.min(-alongNormal / speed, 1.0)));
	}

	/**
	 * Mirrors the flight in the surface and hands back what is left of its speed.
	 *
	 * <p>Both ways an arrow can be thrown off a block come through here - the glancing ricochet and
	 * the hard bounce off ice - so that they answer the same question the same way. They did not
	 * always: the ricochet scaled the whole reflection by a flat factor, so a shot arriving at 1
	 * degree and one arriving at 34 both left stone with the same 35% of their speed, while the ice
	 * branch already charged for the angle. Ice was the one in the right, and it is the one the
	 * ricochet was brought round to. The energy of a hit goes INTO the block in proportion to how
	 * square-on it was, so what is still available to bounce is what was travelling along the face:
	 * {@code cos(angle from the surface)} is 1 for a graze and 0 for a perpendicular strike.
	 *
	 * <p>{@code kept} carries the rest of the answer - the config's factor, times the material's own
	 * share where there is a table to consult. Deliberately NOT folded into the cosine: angle and
	 * material are different questions and they multiply. The angle decides how much energy is
	 * offered to the block, the material decides how much of it comes back. Replacing the material
	 * factor with the cosine instead - the other way these two branches could have been squared up -
	 * would have left every surface handing back roughly the same speed, since a ricochet is only
	 * ever allowed in a window where the cosine runs from 1.00 down to 0.82.
	 */
	@Unique
	private static Vec3 betterArrows$bounceOff(
			Vec3 velocity, Vec3 normal, double alongNormal, double angleFromSurfaceDegrees, double kept) {
		double glancing = Math.cos(Math.toRadians(angleFromSurfaceDegrees));
		return velocity.subtract(normal.scale(2.0 * alongNormal)).scale(kept * glancing);
	}

	@Unique
	private static void betterArrows$playBounceEffects(
			AbstractArrow self, BlockState state, BlockHitResult hitResult, float intensity) {
		BetterArrowsConfig.SurfaceSounds soundConfig = BetterArrowsConfig.get().surfaceSounds;
		if (soundConfig.enabled && !state.is(BetterArrowsMod.NO_IMPACT_SOUND)) {
			// Higher and thinner than a landing: a skip off the surface, not a thud into it.
			self.playSound(state.getSoundType().getStepSound(), soundConfig.volume * 0.8F, 1.5F);
		}
		if (self.level() instanceof ServerLevel level) {
			betterArrows$spawnBlockParticles(level, state, hitResult, Math.min(intensity, 1.0F) * 0.5F);
		}
	}
}
