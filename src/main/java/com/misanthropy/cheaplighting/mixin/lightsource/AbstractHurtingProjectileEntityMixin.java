package com.misanthropy.cheaplighting.mixin.lightsource;

import com.misanthropy.cheaplighting.CheapLighting;
import com.misanthropy.cheaplighting.DynamicLightSource;
import com.misanthropy.cheaplighting.api.DynamicLightHandlers;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(AbstractHurtingProjectile.class)
public abstract class AbstractHurtingProjectileEntityMixin extends Entity implements DynamicLightSource {
	public AbstractHurtingProjectileEntityMixin(EntityType<?> type, Level level) {
		super(type, level);
	}

	@Override
	public void cl$dynamicLightTick() {
		if (!this.cl$isDynamicLightEnabled())
			this.cl$setDynamicLightEnabled(true);
	}

	@Override
	public int cl$getLuminance() {
		if (CheapLighting.get().config.getEntitiesLightSource().get() && DynamicLightHandlers.canLightUp(this))
			return 14;
		return 0;
	}
}
