package com.misanthropy.cheaplighting.mixin;

import com.misanthropy.cheaplighting.CheapLighting;
import com.misanthropy.cheaplighting.DynamicLightSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
public class EntityRendererMixin<T extends Entity> {
	@Inject(method = "getBlockLightLevel", at = @At("RETURN"), cancellable = true)
	private void onGetBlockLight(T entity, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
		if (!CheapLighting.get().config.getDynamicLightsMode().isEnabled())
			return;

		int vanilla = cir.getReturnValueI();
		int entityLuminance = ((DynamicLightSource) entity).cl$getLuminance();

		if (entityLuminance >= 15) {
			cir.setReturnValue(15);
			return;
		}

		int posLuminance = (int) CheapLighting.get().getDynamicLightLevel(pos);

		int max = Math.max(vanilla, Math.max(entityLuminance, posLuminance));
		if (max > vanilla)
			cir.setReturnValue(max);
	}
}
