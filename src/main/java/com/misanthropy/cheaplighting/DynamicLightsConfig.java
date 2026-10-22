package com.misanthropy.cheaplighting;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.Builder;
import net.minecraftforge.common.ForgeConfigSpec.EnumValue;

import java.util.HashMap;

public class DynamicLightsConfig {
	public static final  ForgeConfigSpec  SPECS;

	public static final EnumValue<DynamicLightsMode> DYNAMIC_LIGHTS_MODE;

	public static final BooleanValue ENTITIES_LIGHT_SOURCE;
	public static final BooleanValue SELF_LIGHT_SOURCE;
	public static final BooleanValue BLOCK_ENTITIES_LIGHT_SOURCE;
	public static final BooleanValue WATER_SENSITIVE_CHECK;

	public static final EnumValue<ExplosiveLightingMode> CREEPER_LIGHTING_MODE;
	public static final EnumValue<ExplosiveLightingMode> TNT_LIGHTING_MODE;

	public static final HashMap<String, Boolean> ENTITIES_SETTINGS;

	static {
		var BUILDER = new Builder();
		BUILDER.push("cheaplighting");

		DYNAMIC_LIGHTS_MODE = BUILDER
				.comment("Lighting mode")
				.defineEnum("mode", DynamicLightsMode.REALTIME);

		ENTITIES_LIGHT_SOURCE = BUILDER
				.comment("Enable entities light source.")
				.define("entities", false);

		SELF_LIGHT_SOURCE = BUILDER
				.comment("Enable first-person player light source.")
				.define("self", true);

		BLOCK_ENTITIES_LIGHT_SOURCE = BUILDER
				.comment("Enable block entities light source.")
				.define("block_entities", true);

		WATER_SENSITIVE_CHECK = BUILDER
				.comment("Enables water-sensitive light sources check. This means that water-sensitive items will not light up when submerged in water.")
				.define("water_sensitive_check", true);

		TNT_LIGHTING_MODE = BUILDER
				.comment("TNT lighting mode. May be off, simple or fancy.")
				.defineEnum("tnt", ExplosiveLightingMode.SIMPLE);

		CREEPER_LIGHTING_MODE = BUILDER
				.comment("Creeper lighting mode. May be off, simple or fancy.")
				.defineEnum("creeper", ExplosiveLightingMode.OFF);

		ENTITIES_SETTINGS = new HashMap<>();

		BUILDER.pop();
		SPECS = BUILDER.build();
	}

	public DynamicLightsMode getDynamicLightsMode() {
		return DYNAMIC_LIGHTS_MODE.get();
	}

	public BooleanValue getEntitiesLightSource() {
		return ENTITIES_LIGHT_SOURCE;
	}

	public BooleanValue getSelfLightSource() {
		return SELF_LIGHT_SOURCE;
	}

	public BooleanValue getBlockEntitiesLightSource() {
		return BLOCK_ENTITIES_LIGHT_SOURCE;
	}

	public BooleanValue getWaterSensitiveCheck() {
		return WATER_SENSITIVE_CHECK;
	}

	public EnumValue<ExplosiveLightingMode> getCreeperLightingMode() {
		return CREEPER_LIGHTING_MODE;
	}

	public EnumValue<ExplosiveLightingMode> getTntLightingMode() {
		return TNT_LIGHTING_MODE;
	}

	public HashMap<String, Boolean> getLightSettings() { return ENTITIES_SETTINGS; }
}
