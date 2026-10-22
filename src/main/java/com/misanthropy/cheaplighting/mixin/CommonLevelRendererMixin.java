package com.misanthropy.cheaplighting.mixin;

import com.misanthropy.cheaplighting.CheapLighting;
import com.misanthropy.cheaplighting.accessor.WorldRendererAccessor;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = LevelRenderer.class, priority = 900)
public abstract class CommonLevelRendererMixin implements WorldRendererAccessor {
	@Invoker("setSectionDirty")
	@Override
	public abstract void cheaplighting$scheduleChunkRebuild(int x, int y, int z, boolean important);

	@Inject(
			method = "getLightColor(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)I",
			at = @At("TAIL"),
			cancellable = true
	)
	private static void onGetLightmapCoordinates(BlockAndTintGetter level, BlockState state, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
		if (CheapLighting.get().config.getDynamicLightsMode().isEnabled() && !state.isSolidRender(level, pos))
			cir.setReturnValue(CheapLighting.get().getLightmapWithDynamicLight(pos, cir.getReturnValue()));
	}
}
