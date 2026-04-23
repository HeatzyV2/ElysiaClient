package net.elysiastudios.client.module.impl;

import net.elysiastudios.client.module.Category;
import net.elysiastudios.client.module.Module;
import net.elysiastudios.client.setting.BooleanModuleSetting;

public class Sneak extends Module {
    private boolean onlyOnGround;
    private boolean onlyWhenStill;
    private boolean keepInScreens;

    public Sneak() {
        super("Sneak", "Reste accroupi automatiquement.", Category.MOVEMENT, 0, "👣");
        this.onlyOnGround = false;
        this.onlyWhenStill = false;
        this.keepInScreens = false;
        registerSneakSettings();
    }

    @Override
    public void onTick() {
        if (mc.player == null) {
            return;
        }

        boolean shouldSneak = (!onlyOnGround || mc.player.onGround())
            && (!onlyWhenStill || mc.player.getDeltaMovement().horizontalDistanceSqr() < 0.0004D)
            && (keepInScreens || mc.screen == null);

        mc.options.keyShift.setDown(shouldSneak);
    }

    @Override
    public void onDisable() {
        if (mc.player != null) {
            mc.options.keyShift.setDown(false);
        }
    }

    private void registerSneakSettings() {
        addSetting(new BooleanModuleSetting(
            "sneak_only_ground",
            "Seulement au sol",
            "N'active le sneak automatique qu'au sol.",
            "Sneak",
            true,
            () -> onlyOnGround,
            value -> onlyOnGround = value,
            false
        ));
        addSetting(new BooleanModuleSetting(
            "sneak_only_still",
            "Seulement à l'arrêt",
            "N'active le sneak auto que quand le joueur bouge très peu.",
            "Sneak",
            true,
            () -> onlyWhenStill,
            value -> onlyWhenStill = value,
            false
        ));
        addSetting(new BooleanModuleSetting(
            "sneak_keep_screens",
            "Garder dans les menus",
            "Maintient le sneak même si un écran est ouvert.",
            "Sneak",
            true,
            () -> keepInScreens,
            value -> keepInScreens = value,
            false
        ));
    }
}
