package com.misanthropy.cheaplighting.compat;

import com.misanthropy.cheaplighting.CheapLighting;
import net.minecraft.world.entity.player.Player;
import org.com.dynamiclantern.WaistItemCache;
import org.com.dynamiclantern.WaistItemRules;

public final class DynamicLanternCompat {
	private DynamicLanternCompat() {
	}

	public static int getWaistLuminance(Player player, boolean submergedInFluid) {
		int luminance = 0;
		for (var cached : WaistItemCache.getVisibleWaistItemsOrRefresh(player)) {
			var stack = cached.stack();
			int itemLuminance = CheapLighting.getLuminanceFromItemStack(stack, submergedInFluid);
			if (itemLuminance == 0 && WaistItemRules.isShaderLightItem(stack))
				itemLuminance = CheapLighting.getLuminanceFromItemStack(WaistItemRules.shaderLightStack(stack), submergedInFluid);
			luminance = Math.max(luminance, itemLuminance);
		}
		return luminance;
	}
}
