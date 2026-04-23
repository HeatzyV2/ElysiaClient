package net.elysiastudios.client.compat;

import net.fabricmc.loader.api.FabricLoader;

public final class EssentialCompat {
    private static final boolean ESSENTIAL_LOADED =
        FabricLoader.getInstance().isModLoaded("essential") ||
        FabricLoader.getInstance().isModLoaded("essential-container");

    private EssentialCompat() {
    }

    public static boolean isEssentialLoaded() {
        return ESSENTIAL_LOADED;
    }
}
