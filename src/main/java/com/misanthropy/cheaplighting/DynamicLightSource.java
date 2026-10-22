package com.misanthropy.cheaplighting;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

public interface DynamicLightSource {
	double cl$getDynamicLightX();

	double cl$getDynamicLightY();

	double cl$getDynamicLightZ();

	Level cl$getDynamicLightLevel();

	default boolean cl$isDynamicLightEnabled() {
		return CheapLighting.get().config.getDynamicLightsMode().isEnabled() && CheapLighting.get().containsLightSource(this);
	}

	@ApiStatus.Internal
	default void cl$setDynamicLightEnabled(boolean enabled) {
		this.cl$resetDynamicLight();
		if (enabled)
			CheapLighting.get().addLightSource(this);
		else
			CheapLighting.get().removeLightSource(this);
	}

	void cl$resetDynamicLight();

	int cl$getLuminance();

	void cl$dynamicLightTick();

	boolean cl$shouldUpdateDynamicLight();

	boolean cheaplighting$updateDynamicLight(@NotNull LevelRenderer renderer);

	void cheaplighting$scheduleTrackedChunksRebuild(@NotNull LevelRenderer renderer);
}
