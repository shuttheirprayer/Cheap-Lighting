package com.misanthropy.cheaplighting.api;

import com.misanthropy.cheaplighting.CheapLighting;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public interface DynamicLightHandler<T> {
	int getLuminance(T lightSource);

	default boolean isWaterSensitive(T lightSource) {
		return false;
	}

	static <T extends LivingEntity> @NotNull DynamicLightHandler<T> makeHandler(
			Function<T, Integer> luminance, Function<T, Boolean> waterSensitive
	) {
		return new DynamicLightHandler<>() {
			@Override
			public int getLuminance(T lightSource) {
				return luminance.apply(lightSource);
			}

			@Override
			public boolean isWaterSensitive(T lightSource) {
				return waterSensitive.apply(lightSource);
			}
		};
	}

	static <T extends LivingEntity> @NotNull DynamicLightHandler<T> makeLivingEntityHandler(@NotNull DynamicLightHandler<T> handler) {
		return entity -> {
			int luminance = 0;

			for (var equipped : entity.getAllSlots()) {
				luminance = Math.max(luminance, CheapLighting.getLuminanceFromItemStack(equipped, entity.isUnderWater()));
			}

			return Math.max(luminance, handler.getLuminance(entity));
		};
	}

	static <T extends Creeper> @NotNull DynamicLightHandler<T> makeCreeperEntityHandler(@Nullable DynamicLightHandler<T> handler) {
		return new DynamicLightHandler<>() {
			@Override
			public int getLuminance(T entity) {
				int luminance = 0;

				if (entity.getSwelling(0.f) > 0.001) {
					luminance = switch (CheapLighting.get().config.getCreeperLightingMode().get()) {
						case OFF -> 0;
						case SIMPLE -> 10;
						case FANCY -> (int) (entity.getSwelling(0.f) * 10.0);
					};
				}

				if (handler != null)
					luminance = Math.max(luminance, handler.getLuminance(entity));

				return luminance;
			}

			@Override
			public boolean isWaterSensitive(T lightSource) {
				return true;
			}
		};
	}
}
