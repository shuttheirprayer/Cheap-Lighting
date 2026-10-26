package com.misanthropy.cheaplighting.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.blaze3d.systems.RenderSystem;
import com.llamalad7.mixinextras.sugar.Local;
import com.misanthropy.cheaplighting.CheapLighting;
import com.misanthropy.cheaplighting.accessor.WorldRendererAccessor;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = LevelRenderer.class, priority = 900)
public abstract class CommonLevelRendererMixin implements WorldRendererAccessor {
	@Invoker("setSectionDirty")
	@Override
	public abstract void cheaplighting$scheduleChunkRebuild(int x, int y, int z, boolean important);

	@ModifyReturnValue(
			method = "getLightColor(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)I",
			at = @At("TAIL")
	)
	private static int onGetLightmapCoordinates(int original, @Local(argsOnly = true) BlockAndTintGetter level, @Local(argsOnly = true) BlockState state, @Local(argsOnly = true) BlockPos pos) {
		CheapLighting lighting = CheapLighting.get();
		int result = original;
		if (lighting.getLightSourcesCount() != 0 && lighting.config.getDynamicLightsMode().isEnabled() && !state.isSolidRender(level, pos))
			result = lighting.getLightmapWithDynamicLight(pos, original);
		if (RenderSystem.isOnRenderThread()) {
			CheapLighting.lastLitPos = pos;
			CheapLighting.lastLitColor = result;
		}
		return result;
	}
}
