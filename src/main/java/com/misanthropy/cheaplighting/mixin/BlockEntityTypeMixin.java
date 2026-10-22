package com.misanthropy.cheaplighting.mixin;

import com.misanthropy.cheaplighting.CheapLighting;
import com.misanthropy.cheaplighting.accessor.DynamicLightHandlerHolder;
import com.misanthropy.cheaplighting.api.DynamicLightHandler;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(BlockEntityType.class)
public class BlockEntityTypeMixin<T extends BlockEntity> implements DynamicLightHandlerHolder<T> {
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
			var self = (BlockEntityType<?>) (Object) this;
			var id = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(self);
			if (id == null) {
				return false;
			}

			var path = "light_sources.settings.block_entities." + id.getNamespace() + '.' + id.getPath().replace('/', '.');

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
		var self = (BlockEntityType<?>) (Object) this;
		var id = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(self);
		if (id == null) {
			return Component.empty();
		}
		return Component.literal(id.getNamespace() + ':' + id.getPath());
	}
}
