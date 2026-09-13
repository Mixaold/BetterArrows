package net.betterarrows.client.mixin;

import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;
import java.util.Map;

/**
 * Exposes {@code ModelPart}'s private cube/child lists so
 * {@link net.betterarrows.client.render.StuckArrowRenderLayer} can walk the model tree itself
 * with an early exit once every stuck arrow on the entity has been found, instead of always
 * paying for a full traversal via the public {@code ModelPart#visit} (which has no way to stop
 * partway through).
 */
@Mixin(ModelPart.class)
public interface ModelPartAccessor {
	@Accessor("cubes")
	List<ModelPart.Cube> betterArrows$cubes();

	@Accessor("children")
	Map<String, ModelPart> betterArrows$children();
}
