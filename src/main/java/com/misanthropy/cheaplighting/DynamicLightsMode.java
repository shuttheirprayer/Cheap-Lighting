package com.misanthropy.cheaplighting;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Optional;

public enum DynamicLightsMode {
	OFF(0, ChatFormatting.RED, "OFF"),
	SLOW(500, ChatFormatting.YELLOW, "SLOW"),
	FAST(250, ChatFormatting.GOLD, "REGULAR"),
	REALTIME(0, ChatFormatting.GREEN, "REALTIME");

	private final int delay;
	private final Component translatedText;

	DynamicLightsMode(int delay, @NotNull ChatFormatting formatting, @NotNull String translatedText) {
		this.delay = delay;
		this.translatedText = Component.literal(translatedText).copy().withStyle(formatting);
	}

	public boolean isEnabled() {
		return this != OFF;
	}

	public boolean hasDelay() {
		return this.delay != 0;
	}

	public int getDelay() {
		return this.delay;
	}

	public DynamicLightsMode next() {
		DynamicLightsMode[] v = values();
		if (v.length == this.ordinal() + 1)
			return v[0];
		return v[this.ordinal() + 1];
	}

	public @NotNull Component getTranslatedText() {
		return this.translatedText;
	}

	public static @NotNull Optional<DynamicLightsMode> byId(@NotNull String id) {
		return Arrays.stream(values()).filter(mode -> mode.getName().equalsIgnoreCase(id)).findFirst();
	}

	public @NotNull String getName() {
		return this.name().toLowerCase();
	}
}
