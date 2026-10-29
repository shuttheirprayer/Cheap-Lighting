package com.misanthropy.cheaplighting.mixin;

import com.misanthropy.cheaplighting.CheapLighting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftClientMixin {
	@Inject(method = "tick", at = @At("HEAD"))
	private void onTick(CallbackInfo ci) {
		CheapLighting.get().invalidateLightGrid();
	}

	@Inject(method = "updateLevelInEngines", at = @At("HEAD"))
	private void onUpdateLevelInEngines(ClientLevel level, CallbackInfo ci) {
		CheapLighting.get().clearLightSources();
	}
}
