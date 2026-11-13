package com.misanthropy.cheaplighting.api.entity;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.misanthropy.cheaplighting.CheapLighting;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.minecraft.advancements.critereon.EntityFlagsPredicate;
import net.minecraft.advancements.critereon.MobEffectsPredicate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

public final class EntityLightSources {
	private static final Set<String> SUPPORTED_MATCH_KEYS = Set.of("type", "effects", "flags");
	private static List<Source> sources = List.of();

	private EntityLightSources() {
		throw new UnsupportedOperationException("EntityLightSources only contains static definitions.");
	}

	record Source(Predicate<EntityType<?>> type, MobEffectsPredicate effects, EntityFlagsPredicate flags, int luminance) {
		boolean matches(Entity entity) {
			return this.type.test(entity.getType()) && this.effects.matches(entity) && this.flags.matches(entity);
		}
	}

	public static void load(ResourceManager resourceManager) {
		var loaded = new ArrayList<Source>();
		resourceManager.listResources("dynamiclights/entity", path -> path.getPath().endsWith(".json")).forEach((id, resource) -> {
			try (var reader = resource.openAsReader()) {
				var source = parse(JsonParser.parseReader(reader).getAsJsonObject());
				if (source != null)
					loaded.add(source);
				else
					CheapLighting.get().warn("Skipped entity light source \"" + id + "\", it uses unsupported match or luminance fields.");
			} catch (IOException | RuntimeException e) {
				CheapLighting.get().warn("Failed to load entity light source \"" + id + "\": " + e.getMessage());
			}
		});
		sources = List.copyOf(loaded);
	}

	public static int getLuminance(Entity entity) {
		int luminance = 0;
		for (var source : sources)
			if (source.luminance > luminance && source.matches(entity))
				luminance = source.luminance;
		return luminance;
	}

	static @Nullable Source parse(JsonObject json) {
		var match = GsonHelper.getAsJsonObject(json, "match");
		var luminance = json.get("luminance");
		if (!SUPPORTED_MATCH_KEYS.containsAll(match.keySet()) || luminance == null || !luminance.isJsonPrimitive())
			return null;
		return new Source(parseType(match.get("type")), MobEffectsPredicate.fromJson(match.get("effects")),
				EntityFlagsPredicate.fromJson(match.get("flags")), luminance.getAsInt());
	}

	private static Predicate<EntityType<?>> parseType(@Nullable JsonElement element) {
		if (element == null)
			return type -> true;
		if (element.isJsonArray()) {
			Set<EntityType<?>> types = new ReferenceOpenHashSet<>();
			element.getAsJsonArray().forEach(id -> types.add(entityType(id.getAsString())));
			return types::contains;
		}
		var id = element.getAsString();
		if (id.startsWith("#")) {
			var tag = TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation(id.substring(1)));
			return type -> type.is(tag);
		}
		var single = entityType(id);
		return type -> type == single;
	}

	private static EntityType<?> entityType(String id) {
		return BuiltInRegistries.ENTITY_TYPE.getOptional(new ResourceLocation(id))
				.orElseThrow(() -> new JsonSyntaxException("Unknown entity type " + id));
	}
}
