package com.misanthropy.cheaplighting.mixin.lightsource;

import com.misanthropy.cheaplighting.CheapLighting;
import com.misanthropy.cheaplighting.DynamicLightSource;
import com.misanthropy.cheaplighting.api.DynamicLightHandlers;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityMixin implements DynamicLightSource {
	@Shadow
	public Level level;

	@Shadow
	public abstract double getX();

	@Shadow
	public abstract double getEyeY();

	@Shadow
	public abstract double getZ();

	@Shadow
	public abstract double getY();

	@Shadow
	public abstract boolean isOnFire();

	@Shadow
	public abstract EntityType<?> getType();

	@Shadow
	public abstract BlockPos getOnPos();

	@Shadow
	public abstract boolean isRemoved();

	@Shadow
	public abstract ChunkPos chunkPosition();

	@Unique
	protected int cheaplighting$luminance = 0;
	@Unique
	private int cheaplighting$lastLuminance = 0;
	@Unique
	private long cheaplighting$lastUpdate = 0;
	@Unique
	private double cheaplighting$prevX;
	@Unique
	private double cheaplighting$prevY;
	@Unique
	private double cheaplighting$prevZ;
	@Unique
	private LongOpenHashSet cheaplighting$trackedLitChunkPos;

	@Inject(method = "tick", at = @At("TAIL"))
	public void onTick(CallbackInfo ci) {
		if (this.level.isClientSide()) {
			if (this.isRemoved()) {
				this.cl$setDynamicLightEnabled(false);
			} else {
				this.cl$dynamicLightTick();
				if ((!CheapLighting.get().config.getEntitiesLightSource().get() && this.getType() != EntityType.PLAYER)
						|| !DynamicLightHandlers.canLightUp((Entity) (Object) this))
					this.cheaplighting$luminance = 0;
				CheapLighting.updateTracking(this);
			}
		}
	}

	@Inject(method = "remove", at = @At("TAIL"))
	public void onRemove(CallbackInfo ci) {
		if (this.level.isClientSide())
			this.cl$setDynamicLightEnabled(false);
	}

	@Override
	public double cl$getDynamicLightX() {
		return this.getX();
	}

	@Override
	public double cl$getDynamicLightY() {
		return this.getEyeY();
	}

	@Override
	public double cl$getDynamicLightZ() {
		return this.getZ();
	}

	@Override
	public Level cl$getDynamicLightLevel() {
		return this.level;
	}

	@Override
	public void cl$resetDynamicLight() {
		this.cheaplighting$lastLuminance = 0;
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
	public void cl$dynamicLightTick() {
		this.cheaplighting$luminance = this.isOnFire() ? 15 : 0;

		int luminance = DynamicLightHandlers.getLuminanceFrom((Entity) (Object) this);
		if (luminance > this.cheaplighting$luminance)
			this.cheaplighting$luminance = luminance;
	}

	@Override
	public int cl$getLuminance() {
		return this.cheaplighting$luminance;
	}

	@Override
	public boolean cheaplighting$updateDynamicLight(@NotNull LevelRenderer renderer) {
		if (!this.cl$shouldUpdateDynamicLight())
			return false;
		double deltaX = this.getX() - this.cheaplighting$prevX;
		double deltaY = this.getY() - this.cheaplighting$prevY;
		double deltaZ = this.getZ() - this.cheaplighting$prevZ;

		int luminance = this.cl$getLuminance();

		if (Math.abs(deltaX) > 0.1D || Math.abs(deltaY) > 0.1D || Math.abs(deltaZ) > 0.1D || luminance != this.cheaplighting$lastLuminance) {
			this.cheaplighting$prevX = this.getX();
			this.cheaplighting$prevY = this.getY();
			this.cheaplighting$prevZ = this.getZ();
			this.cheaplighting$lastLuminance = luminance;

			var newPos = new LongOpenHashSet();

			if (luminance > 0) {
				var entityChunkPos = this.chunkPosition();
				var chunkPos = new BlockPos.MutableBlockPos(entityChunkPos.x, SectionPos.blockToSectionCoord(this.getEyeY()), entityChunkPos.z);

				CheapLighting.scheduleChunkRebuild(renderer, chunkPos);
				CheapLighting.updateTrackedChunks(chunkPos, this.cheaplighting$trackedLitChunkPos, newPos);

				var directionX = (this.getOnPos().getX() & 15) >= 8 ? Direction.EAST : Direction.WEST;
				var directionY = ((int) Mth.floor(this.getEyeY()) & 15) >= 8 ? Direction.UP : Direction.DOWN;
				var directionZ = (this.getOnPos().getZ() & 15) >= 8 ? Direction.SOUTH : Direction.NORTH;

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
					CheapLighting.scheduleChunkRebuild(renderer, chunkPos);
					CheapLighting.updateTrackedChunks(chunkPos, this.cheaplighting$trackedLitChunkPos, newPos);
				}
			}

			this.cheaplighting$scheduleTrackedChunksRebuild(renderer);

			this.cheaplighting$trackedLitChunkPos = newPos;
			return true;
		}
		return false;
	}

	@Override
	public void cheaplighting$scheduleTrackedChunksRebuild(@NotNull LevelRenderer renderer) {
		if (this.cheaplighting$trackedLitChunkPos != null && Minecraft.getInstance().level == this.level)
			for (long pos : this.cheaplighting$trackedLitChunkPos) {
				CheapLighting.scheduleChunkRebuild(renderer, pos);
			}
	}
}
