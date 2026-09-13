package net.betterarrows.client.mixin;

import net.betterarrows.client.StuckArrowClientCache;
import net.betterarrows.client.state.StuckArrowsAccess;
import net.betterarrows.config.BetterArrowsConfig;
import net.minecraft.client.renderer.entity.layers.ArrowLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Stops vanilla and this mod from both drawing the same arrows on a player.
 *
 * <p>Vanilla's stuck-arrow layer exists for exactly one renderer - the player's - and scatters
 * {@code arrowCount} arrows over pseudo-random cubes. This mod draws the same arrows at their real
 * hit points, so without this the player wears every arrow twice: once where it actually hit, once
 * somewhere random. Vanilla is left to cover only the arrows this mod has no record of (shot
 * before the mod was installed, or by a server that doesn't have it), so nothing is ever lost.
 */
@Mixin(ArrowLayer.class)
public abstract class ArrowLayerMixin {
	// require = 0: if a future Minecraft renames this, the worst case should be a player wearing
	// their arrows twice, not a game that refuses to start.
	@Inject(method = "numStuck", at = @At("RETURN"), cancellable = true, require = 0)
	private void betterArrows$dontDoubleDraw(AvatarRenderState state, CallbackInfoReturnable<Integer> cir) {
		if (!BetterArrowsConfig.get().stuckArrows.enabled) {
			// Feature off - vanilla owns the arrows again, untouched.
			return;
		}

		// All or nothing, never a mixture - and never nothing at all.
		//
		// Suppressing vanilla by SUBTRACTING our count from its was tried and is wrong: whenever
		// vanilla's count runs ahead of what we recorded, the difference was handed back to vanilla
		// to draw, and those leftovers are placed by its pseudo-random per-frame formula rather than
		// pinned to a bone. They visibly swim around behind the model while ours stay welded to it,
		// and the two side by side read as "the arrows lag".
		//
		// But suppressing it UNCONDITIONALLY is wrong too, and that is the more damaging half. An
		// arrow we recorded but could not place on the model is invisible, so on a model this mod
		// cannot resolve against - a resource pack that replaces it, a renderer another mod swapped
		// out - the player wore NO arrows at all, with vanilla's fallback already switched off. That
		// is not a cosmetic regression, it is the mod looking dead.
		//
		// So: vanilla is silenced only once we have at least one arrow actually ready to draw. If we
		// have nothing to show, vanilla keeps its own, misplaced but present. The cost is a possible
		// single frame of vanilla's arrows before the first resolve lands, which is a far better
		// trade than a permanent blank.
		if (state instanceof StuckArrowsAccess access) {
			for (StuckArrowClientCache.Entry entry : access.betterArrows$getStuckArrows()) {
				if (entry.isResolved()) {
					cir.setReturnValue(0);
					return;
				}
			}
		}
	}
}