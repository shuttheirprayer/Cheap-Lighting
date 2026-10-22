package com.misanthropy.cheaplighting.mixin.sodium;

import com.misanthropy.cheaplighting.CheapLighting;
import me.jellysquid.mods.sodium.client.model.light.data.QuadLightData;
import me.jellysquid.mods.sodium.client.model.quad.ModelQuadView;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = {
		"me.jellysquid.mods.sodium.client.model.light.flat.FlatLightPipeline",
		"me.jellysquid.mods.sodium.client.model.light.smooth.SmoothLightPipeline",
		"org.embeddedt.embeddium.render.chunk.light.ForgeLightPipeline"
}, remap = false)
public abstract class LightPipelineMixin {
	@Dynamic
	@Inject(method = "calculate", at = @At("RETURN"), require = 0, remap = false)
	private void cheaplighting$vertexLight(ModelQuadView quad, BlockPos pos, QuadLightData out, Direction cullFace, Direction lightFace, boolean shade, CallbackInfo ci) {
		CheapLighting lighting = CheapLighting.get();
		if (!lighting.config.getDynamicLightsMode().isEnabled())
			return;

		for (int i = 0; i < 4; i++) {
			double level = lighting.getDynamicLightLevel(pos.getX() + quad.getX(i), pos.getY() + quad.getY(i), pos.getZ() + quad.getZ(i));
			out.lm[i] = lighting.getLightmapWithDynamicLight(level, out.lm[i]);
		}
	}
}
