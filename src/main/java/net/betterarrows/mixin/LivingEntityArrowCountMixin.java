package net.betterarrows.mixin;

import net.betterarrows.BetterArrowsMod;
import net.betterarrows.StuckArrowRecord;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Keeps the persisted stuck-arrow backlog in step with vanilla's own arrow count, which decays one
 * arrow at a time over time ({@code LivingEntity#tick} -> {@code removeArrowTime}). Without this
 * the backlog only ever grows: a long-lived, repeatedly-shot mob would accumulate records forever
 * in its NBT, keep showing arrows vanilla has long since "healed" off, and hand every newly
 * tracking player an ever-larger burst of catch-up packets.
 *
 * <p>Trimming from the FRONT keeps the newest hits, which is what vanilla's decay means: the
 * oldest arrow works its way out first.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityArrowCountMixin {
	@Inject(method = "setArrowCount", at = @At("HEAD"))
	private void betterArrows$trimStuckArrows(int count, CallbackInfo ci) {
		LivingEntity self = (LivingEntity) (Object) this;
		if (self.level().isClientSide()) {
			return;
		}

		try {
			List<StuckArrowRecord> existing = self.getAttachedOrElse(BetterArrowsMod.STUCK_ARROWS, List.of());
			if (existing.isEmpty()) {
				return;
			}
			if (count <= 0) {
				self.removeAttached(BetterArrowsMod.STUCK_ARROWS);
				return;
			}
			if (existing.size() > count) {
				self.setAttached(BetterArrowsMod.STUCK_ARROWS,
						List.copyOf(existing.subList(existing.size() - count, existing.size())));
			}
		} catch (Exception e) {
			BetterArrowsMod.logOnce("Failed to trim the stuck-arrow backlog", e);
		}
	}
}
