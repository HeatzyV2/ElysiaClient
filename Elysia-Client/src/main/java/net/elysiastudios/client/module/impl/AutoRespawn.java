package net.elysiastudios.client.module.impl;

import net.elysiastudios.client.module.Category;
import net.elysiastudios.client.module.Module;
import net.elysiastudios.client.setting.BooleanModuleSetting;
import net.elysiastudios.client.setting.IntModuleSetting;

public class AutoRespawn extends Module {
    private int delayTicks;
    private boolean singleplayerOnly;
    private int deathTicks;

    public AutoRespawn() {
        super("AutoRespawn", "Réapparaît automatiquement après la mort.", Category.UTILITY, 0, "↻");
        this.delayTicks = 0;
        this.singleplayerOnly = false;
        this.deathTicks = 0;
        registerRespawnSettings();
    }

    @Override
    public void onTick() {
        if (mc.player == null) {
            return;
        }
        if (singleplayerOnly && !mc.hasSingleplayerServer()) {
            return;
        }

        if (!mc.player.isAlive()) {
            deathTicks++;
            if (deathTicks >= delayTicks) {
                mc.player.respawn();
                deathTicks = 0;
            }
        } else {
            deathTicks = 0;
        }
    }

    private void registerRespawnSettings() {
        addSetting(new IntModuleSetting(
            "respawn_delay_ticks",
            "Délai",
            "Attend un certain nombre de ticks avant de réapparaître.",
            "Respawn",
            true,
            () -> delayTicks,
            value -> delayTicks = value,
            0,
            0,
            100,
            5,
            value -> value + " ticks"
        ));
        addSetting(new BooleanModuleSetting(
            "respawn_singleplayer_only",
            "Solo uniquement",
            "N'agit qu'en monde solo.",
            "Respawn",
            true,
            () -> singleplayerOnly,
            value -> singleplayerOnly = value,
            false
        ));
    }
}
