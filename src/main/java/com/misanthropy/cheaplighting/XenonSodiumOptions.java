package com.misanthropy.cheaplighting;

import com.misanthropy.cheaplighting.util.DynamicLightingPage;
import net.minecraftforge.common.MinecraftForge;
import org.embeddedt.embeddium.api.OptionGUIConstructionEvent;

public final class XenonSodiumOptions {
    private XenonSodiumOptions() {
    }

    public static void register() {
        MinecraftForge.EVENT_BUS.addListener(XenonSodiumOptions::onEmbeddiumPagesRegister);
    }

    private static void onEmbeddiumPagesRegister(OptionGUIConstructionEvent e) {
        e.getPages().add(new DynamicLightingPage());
    }
}
