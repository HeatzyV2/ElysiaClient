package net.elysiastudios.client.module.impl;

import net.elysiastudios.client.module.Category;
import net.elysiastudios.client.module.Module;
import net.elysiastudios.client.setting.BooleanModuleSetting;
import net.elysiastudios.client.setting.IntModuleSetting;
import net.minecraft.network.chat.Component;

public class HealthAlert extends Module {
    private boolean alerted;
    private long lastAlertTime;
    private int healthThreshold;
    private int cooldownSeconds;
    private boolean showOverlayMessage;

    public HealthAlert() {
        super("HealthAlert", "Affiche une alerte claire quand les points de vie sont bas.", Category.COMBAT, 0, "HP");
        this.healthThreshold = 6;
        this.cooldownSeconds = 8;
        this.showOverlayMessage = true;
        registerAlertSettings();
    }

    @Override
    public void onTick() {
        if (mc.player == null || !showOverlayMessage) {
            return;
        }

        boolean lowHealth = mc.player.getHealth() <= healthThreshold;
        if (lowHealth) {
            long now = System.currentTimeMillis();
            if (!alerted || now - lastAlertTime >= cooldownSeconds * 1000L) {
                mc.gui.setOverlayMessage(Component.literal("Vie faible - pense à te soigner"), false);
                alerted = true;
                lastAlertTime = now;
            }
        } else {
            alerted = false;
        }
    }

    private void registerAlertSettings() {
        addSetting(new IntModuleSetting(
            "health_threshold",
            "Seuil de vie",
            "Définit à partir de combien de points de vie l'alerte se déclenche.",
            "Alerte",
            true,
            () -> healthThreshold,
            value -> healthThreshold = value,
            6,
            2,
            20,
            1,
            value -> value + " HP"
        ));
        addSetting(new IntModuleSetting(
            "health_cooldown",
            "Cooldown",
            "Temps minimum entre deux alertes visuelles.",
            "Alerte",
            true,
            () -> cooldownSeconds,
            value -> cooldownSeconds = value,
            8,
            2,
            30,
            1,
            value -> value + " s"
        ));
        addSetting(new BooleanModuleSetting(
            "health_overlay",
            "Message écran",
            "Affiche un message compact au centre de l'écran.",
            "Alerte",
            true,
            () -> showOverlayMessage,
            value -> showOverlayMessage = value,
            true
        ));
    }
}
