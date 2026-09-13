package net.betterarrows.client.mixin;

import net.betterarrows.client.StuckArrowClientCache;
import net.betterarrows.client.state.StuckArrowsAccess;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.List;

@Mixin(LivingEntityRenderState.class)
public abstract class LivingEntityRenderStateMixin implements StuckArrowsAccess {
	@Unique
	private List<StuckArrowClientCache.Entry> betterArrows$stuckArrows = List.of();

	@Unique
	private long betterArrows$renderPass;

	@Override
	public List<StuckArrowClientCache.Entry> betterArrows$getStuckArrows() {
		// Null-tolerant on purpose: a render state built through a path where the merged field
		// initialiser never ran would otherwise turn a cosmetic mod into a rendering crash.
		return this.betterArrows$stuckArrows == null ? List.of() : this.betterArrows$stuckArrows;
	}

	@Override
	public void betterArrows$setStuckArrows(List<StuckArrowClientCache.Entry> arrows) {
		this.betterArrows$stuckArrows = arrows;
	}

	@Override
	public long betterArrows$getRenderPass() {
		return this.betterArrows$renderPass;
	}

	@Override
	public void betterArrows$setRenderPass(long pass) {
		this.betterArrows$renderPass = pass;
	}
}
