package com.misanthropy.cheaplighting.mixin;

import com.misanthropy.cheaplighting.CheapLighting;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {
    @Inject(
            method = "getLightColor(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;)I",
            at = @At("TAIL"),
            cancellable = true
    )
    private static void onGetLightColor(BlockAndTintGetter level, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        if (CheapLighting.get().config.getDynamicLightsMode().isEnabled() && !level.getBlockState(pos).isSolidRender(level, pos)) {
            cir.setReturnValue(CheapLighting.get().getLightmapWithDynamicLight(pos, cir.getReturnValue()));
        }
    }
}
