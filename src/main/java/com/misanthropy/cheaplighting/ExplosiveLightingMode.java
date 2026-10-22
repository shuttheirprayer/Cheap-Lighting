package com.misanthropy.cheaplighting;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Optional;

public enum ExplosiveLightingMode {
	OFF(ChatFormatting.RED, "OFF"),
	SIMPLE(ChatFormatting.YELLOW, "SIMPLE"),
	FANCY(ChatFormatting.GREEN, "FANCY");

	private final Component translatedText;

	ExplosiveLightingMode(@NotNull ChatFormatting formatting, @NotNull String translatedText) {
		this.translatedText = Component.literal(translatedText).copy().withStyle(formatting);
	}

	public boolean isEnabled() {
		return this != OFF;
	}

	public ExplosiveLightingMode next() {
		ExplosiveLightingMode[] v = values();
		if (v.length == this.ordinal() + 1)
			return v[0];
		return v[this.ordinal() + 1];
	}

	public @NotNull Component getTranslatedText() {
		return this.translatedText;
	}

	public static @NotNull Optional<ExplosiveLightingMode> byId(@NotNull String id) {
		return Arrays.stream(values()).filter(mode -> mode.getName().equalsIgnoreCase(id)).findFirst();
	}

	public @NotNull String getName() {
		return this.name().toLowerCase();
	}
}
