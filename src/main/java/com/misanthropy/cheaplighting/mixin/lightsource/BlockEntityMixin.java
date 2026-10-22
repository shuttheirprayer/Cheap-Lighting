package com.misanthropy.cheaplighting.mixin.lightsource;

import com.misanthropy.cheaplighting.CheapLighting;
import com.misanthropy.cheaplighting.DynamicLightSource;
import com.misanthropy.cheaplighting.api.DynamicLightHandlers;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockEntity.class)
public abstract class BlockEntityMixin implements DynamicLightSource {
	@Final
	@Shadow
	protected BlockPos worldPosition;

	@Shadow
	@Nullable
	protected Level level;

	@Shadow
	protected boolean remove;

	@Unique
	private int cheaplighting$luminance = 0;
	@Unique
	private int cheaplighting$lastLuminance = 0;
	@Unique
	private long cheaplighting$lastUpdate = 0;
	@Unique
	private final LongOpenHashSet cheaplighting$trackedLitChunkPos = new LongOpenHashSet();

	@Override
	public double cl$getDynamicLightX() {
		return this.worldPosition.getX() + 0.5;
	}

	@Override
	public double cl$getDynamicLightY() {
		return this.worldPosition.getY() + 0.5;
	}

	@Override
	public double cl$getDynamicLightZ() {
		return this.worldPosition.getZ() + 0.5;
	}

	@Override
	public Level cl$getDynamicLightLevel() {
		return this.level;
	}

	@Inject(method = "setRemoved", at = @At("TAIL"))
	private void onRemoved(CallbackInfo ci) {
		this.cl$setDynamicLightEnabled(false);
	}

	@Override
	public void cl$resetDynamicLight() {
		this.cheaplighting$lastLuminance = 0;
	}

	@Override
	public void cl$dynamicLightTick() {
		if (this.level == null || !this.level.isClientSide())
			return;
		if (!this.remove) {
			this.cheaplighting$luminance = DynamicLightHandlers.getLuminanceFrom((BlockEntity) (Object) this);
			CheapLighting.updateTracking(this);

			if (!this.cl$isDynamicLightEnabled()) {
				this.cheaplighting$lastLuminance = 0;
			}
		}
	}

	@Override
	public int cl$getLuminance() {
		return this.cheaplighting$luminance;
	}

	@Override
	public boolean cl$shouldUpdateDynamicLight() {
		var mode = CheapLighting.get().config.getDynamicLightsMode();
		if (!mode.isEnabled())
			return false;
		if (mode.hasDelay()) {
			long currentTime = System.currentTimeMillis();
			if (currentTime < this.cheaplighting$lastUpdate + mode.getDelay()) {
				return false;
			}

			this.cheaplighting$lastUpdate = currentTime;
		}
		return true;
	}

	@Override
	public boolean cheaplighting$updateDynamicLight(@NotNull LevelRenderer renderer) {
		if (!this.cl$shouldUpdateDynamicLight())
			return false;

		int luminance = this.cl$getLuminance();

		if (luminance != this.cheaplighting$lastLuminance) {
			this.cheaplighting$lastLuminance = luminance;

			if (this.cheaplighting$trackedLitChunkPos.isEmpty()) {
				var chunkPos = new BlockPos.MutableBlockPos(Math.floorDiv(this.worldPosition.getX(), 16),
						Mth.floorDiv(this.worldPosition.getY(), 16),
						Mth.floorDiv(this.worldPosition.getZ(), 16));

				CheapLighting.updateTrackedChunks(chunkPos, null, this.cheaplighting$trackedLitChunkPos);

				var directionX = (this.worldPosition.getX() & 15) >= 8 ? Direction.EAST : Direction.WEST;
				var directionY = (this.worldPosition.getY() & 15) >= 8 ? Direction.UP : Direction.DOWN;
				var directionZ = (this.worldPosition.getZ() & 15) >= 8 ? Direction.SOUTH : Direction.NORTH;

				for (int i = 0; i < 7; i++) {
					if (i % 4 == 0) {
						chunkPos.move(directionX);
					} else if (i % 4 == 1) {
						chunkPos.move(directionZ);
					} else if (i % 4 == 2) {
						chunkPos.move(directionX.getOpposite());
					} else {
						chunkPos.move(directionZ.getOpposite());
						chunkPos.move(directionY);
					}
					CheapLighting.updateTrackedChunks(chunkPos, null, this.cheaplighting$trackedLitChunkPos);
				}
			}

			this.cheaplighting$scheduleTrackedChunksRebuild(renderer);
			return true;
		}
		return false;
	}

	@Override
	public void cheaplighting$scheduleTrackedChunksRebuild(@NotNull LevelRenderer renderer) {
		if (this.level == Minecraft.getInstance().level)
			for (long pos : this.cheaplighting$trackedLitChunkPos) {
				CheapLighting.scheduleChunkRebuild(renderer, pos);
			}
	}
}
