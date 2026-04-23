package net.elysiastudios.client.module.impl;

import net.elysiastudios.client.module.Category;
import net.elysiastudios.client.module.Module;
import net.elysiastudios.client.setting.BooleanModuleSetting;
import net.elysiastudios.client.setting.IntModuleSetting;
import net.minecraft.network.chat.Component;

public class MemoryOptimizer extends Module {
    private long lastOptimization;
    private int triggerPercent;
    private int cooldownSeconds;
    private boolean showFeedback;

    public MemoryOptimizer() {
        super("MemoryOptimizer", "Nettoie la mémoire uniquement quand l'utilisation devient haute.", Category.OPTIMIZATION, 0, "RAM");
        this.triggerPercent = 72;
        this.cooldownSeconds = 120;
        this.showFeedback = false;
        registerMemorySettings();
    }

    @Override
    public void onTick() {
        long now = System.currentTimeMillis();
        Runtime runtime = Runtime.getRuntime();
        long used = runtime.totalMemory() - runtime.freeMemory();
        long max = runtime.maxMemory();
        boolean memoryHigh = max > 0L && used > (long) (max * (triggerPercent / 100.0D));

        if (memoryHigh && now - lastOptimization >= cooldownSeconds * 1000L) {
            lastOptimization = now;
            System.gc();
            if (showFeedback && mc.gui != null) {
                mc.gui.setOverlayMessage(Component.literal("Nettoyage mémoire lancé"), false);
            }
        }
    }

    private void registerMemorySettings() {
        addSetting(new IntModuleSetting(
            "memory_trigger_percent",
            "Seuil d'usage",
            "Pourcentage de RAM utilisé avant nettoyage.",
            "Mémoire",
            true,
            () -> triggerPercent,
            value -> triggerPercent = value,
            72,
            45,
            95,
            1,
            value -> value + "%"
        ));
        addSetting(new IntModuleSetting(
            "memory_cooldown",
            "Cooldown",
            "Temps minimum entre deux optimisations mémoire.",
            "Mémoire",
            true,
            () -> cooldownSeconds,
            value -> cooldownSeconds = value,
            120,
            15,
            600,
            15,
            value -> value + " s"
        ));
        addSetting(new BooleanModuleSetting(
            "memory_feedback",
            "Retour visuel",
            "Affiche un message quand un nettoyage est lancé.",
            "Mémoire",
            true,
            () -> showFeedback,
            value -> showFeedback = value,
            false
        ));
    }
}
