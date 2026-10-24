package com.misanthropy.cheaplighting.mixin;

import com.misanthropy.cheaplighting.DynamicLightSource;
import com.misanthropy.cheaplighting.api.DynamicLightHandlers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.world.level.chunk.LevelChunk$BoundTickingBlockEntity")
public abstract class BoundTickingBlockEntityMixin {
	@Shadow
	@Final
	private BlockEntity blockEntity;

	@Inject(method = "tick", at = @At("HEAD"))
	private void cheaplighting$dynamicLightTick(CallbackInfo ci) {
		BlockEntity entity = this.blockEntity;
		if (DynamicLightHandlers.getDynamicLightHandler(entity.getType()) == null)
			return;
		Level level = entity.getLevel();
		if (level != null && level.isClientSide())
			((DynamicLightSource) entity).cl$dynamicLightTick();
	}
}
