package com.misanthropy.cheaplighting.mixin.lightsource;

import com.misanthropy.cheaplighting.CheapLighting;
import com.misanthropy.cheaplighting.DynamicLightSource;
import com.misanthropy.cheaplighting.ExplosiveLightingMode;
import com.misanthropy.cheaplighting.api.DynamicLightHandlers;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PrimedTnt.class)
public abstract class PrimedTntEntityMixin extends Entity implements DynamicLightSource {
	@Shadow
	public abstract int getFuse();

	@Unique
	private int startFuseTimer = 80;
	@Unique
	private int cheaplighting$luminance;

	public PrimedTntEntityMixin(EntityType<?> type, Level level) {
		super(type, level);
	}

	@Inject(method = "<init>(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/level/Level;)V", at = @At("TAIL"))
	private void onNew(EntityType<? extends PrimedTnt> type, Level level, CallbackInfo ci) {
		this.startFuseTimer = this.getFuse();
	}

	@Inject(method = "tick", at = @At("TAIL"))
	private void onTick(CallbackInfo ci) {
		if (this.level().isClientSide()) {
			if (!CheapLighting.get().config.getTntLightingMode().get().isEnabled())
				return;

			if (this.isRemoved()) {
				this.cl$setDynamicLightEnabled(false);
			} else {
				if (!CheapLighting.get().config.getEntitiesLightSource().get() || !DynamicLightHandlers.canLightUp(this))
					this.cl$resetDynamicLight();
				else
					this.cl$dynamicLightTick();
				CheapLighting.updateTracking(this);
			}
		}
	}

	@Override
	public void cl$dynamicLightTick() {
		if (this.isOnFire()) {
			this.cheaplighting$luminance = 15;
		} else {
			ExplosiveLightingMode lightingMode = CheapLighting.get().config.getTntLightingMode().get();
			if (lightingMode == ExplosiveLightingMode.FANCY) {
				var fuse = this.getFuse() / this.startFuseTimer;
				this.cheaplighting$luminance = (int) (-(fuse * fuse) * 10.0) + 10;
			} else {
				this.cheaplighting$luminance = 10;
			}
		}
	}

	@Override
	public int cl$getLuminance() {
		return this.cheaplighting$luminance;
	}
}
