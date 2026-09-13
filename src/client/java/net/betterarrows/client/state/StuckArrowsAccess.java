package net.betterarrows.client.state;

import net.betterarrows.client.StuckArrowClientCache;

import java.util.List;

/**
 * Duck interface mixed into {@code LivingEntityRenderState}, mirroring the pattern used by
 * "Arrow In The Knee" (AITK) to carry per-entity arrow data from the live entity into the
 * per-frame render-state snapshot that render layers actually see.
 *
 * <p>Lives outside the mixin package deliberately: Mixin forbids direct references to classes
 * inside a config's declared "package" (it assumes everything there is a mixin, not a plain
 * type other code can cast to).
 */
public interface StuckArrowsAccess {
	List<StuckArrowClientCache.Entry> betterArrows$getStuckArrows();

	void betterArrows$setStuckArrows(List<StuckArrowClientCache.Entry> arrows);

	/**
	 * A token that changes every time this render state is re-extracted from its entity, i.e.
	 * once per entity per render pass. The render layer refuses to draw the same arrow twice for
	 * the same token, which is what stops some resource packs (custom-entity-model animation
	 * packs) from double-drawing every arrow, while still letting a legitimately separate pass
	 * (a shader mod's shadow pass, say) draw them again - something a plain per-frame counter
	 * could not distinguish.
	 */
	long betterArrows$getRenderPass();

	void betterArrows$setRenderPass(long pass);
}
