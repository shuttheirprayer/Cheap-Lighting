package com.misanthropy.cheaplighting.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.misanthropy.cheaplighting.CheapLighting;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {
    @ModifyReturnValue(
            method = "getLightColor(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;)I",
            at = @At("TAIL")
    )
    private static int onGetLightColor(int original, BlockAndTintGetter level, BlockPos pos) {
        if (pos == CheapLighting.lastLitPos && original == CheapLighting.lastLitColor && RenderSystem.isOnRenderThread()) {
            return original;
        }
        if (CheapLighting.get().config.getDynamicLightsMode().isEnabled() && !level.getBlockState(pos).isSolidRender(level, pos)) {
            return CheapLighting.get().getLightmapWithDynamicLight(pos, original);
        }
        return original;
    }
}
