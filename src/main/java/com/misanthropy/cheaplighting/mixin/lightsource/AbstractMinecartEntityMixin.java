package com.misanthropy.cheaplighting.mixin.lightsource;

import com.misanthropy.cheaplighting.CheapLighting;
import com.misanthropy.cheaplighting.DynamicLightSource;
import com.misanthropy.cheaplighting.api.DynamicLightHandlers;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractMinecart.class)
public abstract class AbstractMinecartEntityMixin extends Entity implements DynamicLightSource {
	@Shadow
	public abstract BlockState getDisplayBlockState();

	@Unique
	private int cheaplighting$luminance;

	public AbstractMinecartEntityMixin(EntityType<?> type, Level level) {
		super(type, level);
	}

	@Inject(method = "tick", at = @At("HEAD"))
	private void onTick(CallbackInfo ci) {
		if (this.level().isClientSide()) {
			if (this.isRemoved()) {
				this.cl$setDynamicLightEnabled(false);
			} else {
				if (!CheapLighting.get().config.getEntitiesLightSource().get() || !DynamicLightHandlers.canLightUp(this))
					this.cheaplighting$luminance = 0;
				else
					this.cl$dynamicLightTick();
				CheapLighting.updateTracking(this);
			}
		}
	}

	@Override
	public void cl$dynamicLightTick() {
		this.cheaplighting$luminance = Math.max(
				Math.max(
						this.isOnFire() ? 15 : 0,
                        this.getDisplayBlockState().getLightEmission()
				),
				DynamicLightHandlers.getLuminanceFrom(this)
		);
	}

	@Override
	public int cl$getLuminance() {
		return this.cheaplighting$luminance;
	}
}
