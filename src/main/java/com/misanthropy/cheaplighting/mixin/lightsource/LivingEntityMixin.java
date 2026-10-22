package com.misanthropy.cheaplighting.mixin.lightsource;

import com.misanthropy.cheaplighting.CheapLighting;
import com.misanthropy.cheaplighting.DynamicLightSource;
import com.misanthropy.cheaplighting.api.DynamicLightHandlers;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity implements DynamicLightSource {
	@Unique
	protected int cheaplighting$luminance;

	public LivingEntityMixin(EntityType<?> type, Level level) {
		super(type, level);
	}

	@Override
	public void cl$dynamicLightTick() {
		if (!CheapLighting.get().config.getEntitiesLightSource().get() || !DynamicLightHandlers.canLightUp(this)) {
			this.cheaplighting$luminance = 0;
			return;
		}

		if (this.isOnFire() || this.isCurrentlyGlowing()) {
			this.cheaplighting$luminance = 15;
		} else {
			this.cheaplighting$luminance = CheapLighting.getLivingEntityLuminanceFromItems((LivingEntity) (Object) this);
		}

		int luminance = DynamicLightHandlers.getLuminanceFrom(this);
		if (luminance > this.cheaplighting$luminance)
			this.cheaplighting$luminance = luminance;
	}

	@Override
	public int cl$getLuminance() {
		return this.cheaplighting$luminance;
	}
}
