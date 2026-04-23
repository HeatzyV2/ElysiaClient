package net.elysiastudios.client.module.impl;

import net.elysiastudios.client.module.Category;
import net.elysiastudios.client.module.Module;
import net.elysiastudios.client.module.ModuleManager;
import net.elysiastudios.client.setting.FloatModuleSetting;

import java.util.Locale;

public class LowFire extends Module {
    private float verticalOffset;

    public LowFire() {
        super("LowFire", "Réduit la hauteur du feu sur l'écran.", Category.RENDER, 0, "🔥");
        this.verticalOffset = 0.42F;
        registerLowFireSettings();
    }

    public static float getVerticalOffset() {
        LowFire module = ModuleManager.getInstance().getModule(LowFire.class);
        if (module == null || !module.isEnabled()) {
            return 0.0F;
        }
        return module.verticalOffset;
    }

    private void registerLowFireSettings() {
        addSetting(new FloatModuleSetting(
            "low_fire_offset",
            "Décalage vertical",
            "Décale le feu vers le bas pour dégager davantage la vue.",
            "Fire",
            true,
            () -> verticalOffset,
            value -> verticalOffset = value,
            0.42F,
            0.10F,
            0.80F,
            0.02F,
            value -> String.format(Locale.ROOT, "%.2f", value)
        ));
    }
}
