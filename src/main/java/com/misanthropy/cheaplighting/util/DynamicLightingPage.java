package com.misanthropy.cheaplighting.util;

import com.google.common.collect.ImmutableList;
import com.misanthropy.cheaplighting.DynamicLightsConfig;
import com.misanthropy.cheaplighting.DynamicLightsMode;
import com.misanthropy.cheaplighting.ExplosiveLightingMode;
import me.jellysquid.mods.sodium.client.gui.options.Option;
import me.jellysquid.mods.sodium.client.gui.options.OptionGroup;
import me.jellysquid.mods.sodium.client.gui.options.OptionImpl;
import me.jellysquid.mods.sodium.client.gui.options.OptionPage;
import me.jellysquid.mods.sodium.client.gui.options.control.CyclingControl;
import me.jellysquid.mods.sodium.client.gui.options.control.TickBoxControl;
import me.jellysquid.mods.sodium.client.gui.options.storage.SodiumOptionsStorage;
import net.minecraft.network.chat.Component;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.Arrays;
import java.util.function.Function;

public class DynamicLightingPage extends OptionPage {
    public static final SodiumOptionsStorage mixinsOptionsStorage = new SodiumOptionsStorage();

    public DynamicLightingPage() {
        super(Component.translatable("sodium.dynamiclights.options.page"), create());
    }

    private static ImmutableList<OptionGroup> create() {
        return ImmutableList.of(
                OptionGroup.createBuilder()
                        .add(cycling("mode", DynamicLightsMode.class, DynamicLightsConfig.DYNAMIC_LIGHTS_MODE, DynamicLightsMode::getTranslatedText))
                        .build(),
                OptionGroup.createBuilder()
                        .add(tickBox("self", DynamicLightsConfig.SELF_LIGHT_SOURCE))
                        .add(tickBox("entities", DynamicLightsConfig.ENTITIES_LIGHT_SOURCE))
                        .add(tickBox("blockentities", DynamicLightsConfig.BLOCK_ENTITIES_LIGHT_SOURCE))
                        .add(tickBox("underwater", DynamicLightsConfig.WATER_SENSITIVE_CHECK))
                        .build(),
                OptionGroup.createBuilder()
                        .add(cycling("tnt", ExplosiveLightingMode.class, DynamicLightsConfig.TNT_LIGHTING_MODE, ExplosiveLightingMode::getTranslatedText))
                        .add(cycling("creeper", ExplosiveLightingMode.class, DynamicLightsConfig.CREEPER_LIGHTING_MODE, ExplosiveLightingMode::getTranslatedText))
                        .build()
        );
    }

    private static Option<?> tickBox(String key, ForgeConfigSpec.BooleanValue value) {
        return OptionImpl.createBuilder(boolean.class, mixinsOptionsStorage)
                .setName(Component.translatable("sodium.dynamiclights.options." + key))
                .setTooltip(Component.translatable("sodium.dynamiclights.options." + key + ".desc"))
                .setControl(TickBoxControl::new)
                .setBinding((options, v) -> {
                            value.set(v);
                            DynamicLightsConfig.SPECS.save();
                        },
                        options -> value.get())
                .build();
    }

    private static <E extends Enum<E>> Option<?> cycling(String key, Class<E> type, ForgeConfigSpec.EnumValue<E> value, Function<E, Component> label) {
        return OptionImpl.createBuilder(type, mixinsOptionsStorage)
                .setName(Component.translatable("sodium.dynamiclights.options." + key))
                .setTooltip(Component.translatable("sodium.dynamiclights.options." + key + ".desc"))
                .setControl(option -> new CyclingControl<>(option, type,
                        Arrays.stream(type.getEnumConstants()).map(label).toArray(Component[]::new)))
                .setBinding((options, v) -> {
                            value.set(v);
                            DynamicLightsConfig.SPECS.save();
                        },
                        options -> value.get())
                .build();
    }
}
