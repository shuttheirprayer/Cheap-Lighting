package com.misanthropy.cheaplighting.mixin;

import com.misanthropy.cheaplighting.CheapLighting;
import com.misanthropy.cheaplighting.accessor.DynamicLightHandlerHolder;
import com.misanthropy.cheaplighting.api.DynamicLightHandler;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(EntityType.class)
public abstract class EntityTypeMixin<T extends Entity> implements DynamicLightHandlerHolder<T> {
	@Shadow
	public abstract Component getDescription();

	@Unique
	private DynamicLightHandler<T> cheaplighting$lightHandler;
	@Unique
	private Boolean cheaplighting$setting;

	@Override
	public @Nullable DynamicLightHandler<T> cheaplighting$getDynamicLightHandler() {
		return this.cheaplighting$lightHandler;
	}

	@Override
	public void cheaplighting$setDynamicLightHandler(DynamicLightHandler<T> handler) {
		this.cheaplighting$lightHandler = handler;
	}

	@Override
	public boolean cheaplighting$getSetting() {
		if (this.cheaplighting$setting == null) {
			var self = (EntityType<?>) (Object) this;
			var id = BuiltInRegistries.ENTITY_TYPE.getKey(self);
			if (id.getNamespace().equals("minecraft") && id.getPath().equals("pig") && self != EntityType.PIG) {
				return false;
			}

			var path = "light_sources.settings.entities." + id.getNamespace() + '.' + id.getPath().replace('/', '.');

			var config = CheapLighting.get().config.getLightSettings();
			if (!config.containsKey(path)) {
				this.cheaplighting$setting = false;
				return false;
			}

			this.cheaplighting$setting = config.getOrDefault(path, false);
		}

		return this.cheaplighting$setting;
	}

	@Override
	public Component cheaplighting$getName() {
		var name = this.getDescription();
		if (name == null) {
			return Component.translatable("cheaplighting.dummy");
		}
		return name;
	}
}
