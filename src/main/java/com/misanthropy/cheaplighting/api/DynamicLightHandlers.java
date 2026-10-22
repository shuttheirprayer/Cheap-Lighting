package com.misanthropy.cheaplighting.api;

import com.misanthropy.cheaplighting.CheapLighting;
import com.misanthropy.cheaplighting.accessor.DynamicLightHandlerHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.jetbrains.annotations.Nullable;

public final class DynamicLightHandlers {
	private DynamicLightHandlers() {
		throw new UnsupportedOperationException("DynamicLightHandlers only contains static definitions.");
	}

	public static void registerDefaultHandlers() {
		registerDynamicLightHandler(EntityType.BLAZE, DynamicLightHandler.makeHandler(blaze -> 10, blaze -> true));
		registerDynamicLightHandler(EntityType.CREEPER, DynamicLightHandler.makeCreeperEntityHandler(null));
		registerDynamicLightHandler(EntityType.ENDERMAN, entity -> {
			int luminance = 0;
			if (entity.getCarriedBlock() != null)
				luminance = entity.getCarriedBlock().getLightEmission();
			return luminance;
		});
		registerDynamicLightHandler(EntityType.ITEM,
				entity -> CheapLighting.getLuminanceFromItemStack(entity.getItem(), entity.isUnderWater()));
		registerDynamicLightHandler(EntityType.ITEM_FRAME, entity -> {
			var world = entity.level();
			return CheapLighting.getLuminanceFromItemStack(entity.getItem(), !world.getFluidState(entity.blockPosition()).isEmpty());
		});
		registerDynamicLightHandler(EntityType.GLOW_ITEM_FRAME, entity -> {
			var world = entity.level();
			return Math.max(14, CheapLighting.getLuminanceFromItemStack(entity.getItem(),
					!world.getFluidState(entity.blockPosition()).isEmpty()));
		});
		registerDynamicLightHandler(EntityType.MAGMA_CUBE, entity -> (entity.squish > 0.6) ? 11 : 8);
		registerDynamicLightHandler(EntityType.SPECTRAL_ARROW, entity -> 8);
		registerDynamicLightHandler(EntityType.GLOW_SQUID,
				entity -> (int) Mth.clampedLerp(0.f, 12.f, 1.f - entity.getDarkTicksRemaining() / 10.f)
		);
	}

	@SuppressWarnings("unchecked")
	public static <T extends Entity> void registerDynamicLightHandler(EntityType<T> type, DynamicLightHandler<T> handler) {
		register((DynamicLightHandlerHolder<T>) type, handler);
	}

	@SuppressWarnings("unchecked")
	public static <T extends BlockEntity> void registerDynamicLightHandler(BlockEntityType<T> type, DynamicLightHandler<T> handler) {
		register((DynamicLightHandlerHolder<T>) type, handler);
	}

	private static <T> void register(DynamicLightHandlerHolder<T> holder, DynamicLightHandler<T> handler) {
		var registeredHandler = holder.cheaplighting$getDynamicLightHandler();
		if (registeredHandler != null) {
			DynamicLightHandler<T> newHandler = entity -> Math.max(registeredHandler.getLuminance(entity), handler.getLuminance(entity));
			holder.cheaplighting$setDynamicLightHandler(newHandler);
		} else {
			holder.cheaplighting$setDynamicLightHandler(handler);
		}
	}

	public static <T extends Entity> @Nullable DynamicLightHandler<T> getDynamicLightHandler(EntityType<T> type) {
		return DynamicLightHandlerHolder.cast(type).cheaplighting$getDynamicLightHandler();
	}

	public static <T extends BlockEntity> @Nullable DynamicLightHandler<T> getDynamicLightHandler(BlockEntityType<T> type) {
		return DynamicLightHandlerHolder.cast(type).cheaplighting$getDynamicLightHandler();
	}

	public static <T extends Entity> boolean canLightUp(T entity) {
		if (entity == Minecraft.getInstance().player && !CheapLighting.get().config.getSelfLightSource().get())
			return false;

		var setting = DynamicLightHandlerHolder.cast(entity.getType()).cheaplighting$getSetting();
		return !setting;
	}

	public static <T extends BlockEntity> boolean canLightUp(T entity) {
		var setting = DynamicLightHandlerHolder.cast(entity.getType()).cheaplighting$getSetting();
		return !setting;
	}

	@SuppressWarnings("unchecked")
	public static <T extends Entity> int getLuminanceFrom(T entity) {
		if (!CheapLighting.get().config.getEntitiesLightSource().get())
			return 0;
		if (entity == Minecraft.getInstance().player && !CheapLighting.get().config.getSelfLightSource().get())
			return 0;
		var handler = (DynamicLightHandler<T>) getDynamicLightHandler(entity.getType());
		if (handler == null)
			return 0;
		if (!canLightUp(entity))
			return 0;
		if (handler.isWaterSensitive(entity)
				&& !entity.level().getFluidState(BlockPos.containing(entity.getX(), entity.getEyeY(), entity.getZ())).isEmpty())
			return 0;
		return handler.getLuminance(entity);
	}

	@SuppressWarnings("unchecked")
	public static <T extends BlockEntity> int getLuminanceFrom(T entity) {
		if (!CheapLighting.get().config.getBlockEntitiesLightSource().get())
			return 0;
		DynamicLightHandler<T> handler = (DynamicLightHandler<T>) getDynamicLightHandler(entity.getType());
		if (handler == null)
			return 0;
		if (!canLightUp(entity))
			return 0;
		if (handler.isWaterSensitive(entity) && entity.getLevel() != null && !entity.getLevel().getFluidState(entity.getBlockPos()).isEmpty())
			return 0;
		return handler.getLuminance(entity);
	}
}
