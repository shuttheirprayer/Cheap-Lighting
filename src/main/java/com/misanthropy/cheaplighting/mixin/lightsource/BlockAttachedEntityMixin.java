package com.misanthropy.cheaplighting.mixin.lightsource;

import com.misanthropy.cheaplighting.CheapLighting;
import com.misanthropy.cheaplighting.DynamicLightSource;
import com.misanthropy.cheaplighting.api.DynamicLightHandlers;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(HangingEntity.class)
public abstract class BlockAttachedEntityMixin extends Entity implements DynamicLightSource {
	public BlockAttachedEntityMixin(EntityType<?> type, Level level) {
		super(type, level);
	}

	@Override
	public void tick() {
		if (this.level().isClientSide()) {
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
}
