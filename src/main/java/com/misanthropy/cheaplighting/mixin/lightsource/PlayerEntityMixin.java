package com.misanthropy.cheaplighting.mixin.lightsource;

import com.misanthropy.cheaplighting.CheapLighting;
import com.misanthropy.cheaplighting.DynamicLightSource;
import com.misanthropy.cheaplighting.api.DynamicLightHandlers;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Player.class)
public abstract class PlayerEntityMixin extends LivingEntity implements DynamicLightSource {
	@Shadow
	public abstract boolean isSpectator();

	@Unique
	protected int cheaplighting$luminance;
	@Unique
	private Level cheaplighting$lastWorld;

	protected PlayerEntityMixin(EntityType<? extends LivingEntity> type, Level level) {
		super(type, level);
	}

	@Override
	public void cl$dynamicLightTick() {
		if (!DynamicLightHandlers.canLightUp(this)) {
			this.cheaplighting$luminance = 0;
			return;
		}

		if (this.isOnFire() || this.isCurrentlyGlowing()) {
			this.cheaplighting$luminance = 15;
		} else {
			this.cheaplighting$luminance = Math.max(
					DynamicLightHandlers.getLuminanceFrom(this),
					CheapLighting.getLivingEntityLuminanceFromItems(this)
			);
		}

		if (this.isSpectator())
			this.cheaplighting$luminance = 0;

		if (this.cheaplighting$lastWorld != this.level()) {
			this.cheaplighting$lastWorld = this.level();
			this.cheaplighting$luminance = 0;
		}
	}

	@Override
	public int cl$getLuminance() {
		return this.cheaplighting$luminance;
	}
}
