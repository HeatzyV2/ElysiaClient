package net.elysiastudios.client.module.impl;

import net.elysiastudios.client.module.Category;
import net.elysiastudios.client.module.Module;
import net.elysiastudios.client.setting.BooleanModuleSetting;
import net.minecraft.world.effect.MobEffects;

public class AntiBlind extends Module {
    private boolean removeBlindness;
    private boolean removeDarkness;
    private boolean removeNausea;

    public AntiBlind() {
        super("AntiBlind", "Supprime les effets de cécité.", Category.RENDER, 0, "👁️");
        this.removeBlindness = true;
        this.removeDarkness = true;
        this.removeNausea = true;
        registerBlindSettings();
    }

    @Override
    public void onTick() {
        if (mc.player == null) {
            return;
        }
        if (removeBlindness && mc.player.hasEffect(MobEffects.BLINDNESS)) {
            mc.player.removeEffect(MobEffects.BLINDNESS);
        }
        if (removeDarkness && mc.player.hasEffect(MobEffects.DARKNESS)) {
            mc.player.removeEffect(MobEffects.DARKNESS);
        }
        if (removeNausea && mc.player.hasEffect(MobEffects.NAUSEA)) {
            mc.player.removeEffect(MobEffects.NAUSEA);
        }
    }

    private void registerBlindSettings() {
        addSetting(new BooleanModuleSetting(
            "anti_blindness",
            "Retirer Blindness",
            "Supprime l'effet Blindness côté client.",
            "Vision",
            true,
            () -> removeBlindness,
            value -> removeBlindness = value,
            true
        ));
        addSetting(new BooleanModuleSetting(
            "anti_darkness",
            "Retirer Darkness",
            "Supprime l'effet Darkness côté client.",
            "Vision",
            true,
            () -> removeDarkness,
            value -> removeDarkness = value,
            true
        ));
        addSetting(new BooleanModuleSetting(
            "anti_nausea",
            "Retirer Nausea",
            "Supprime l'effet Nausea côté client.",
            "Vision",
            true,
            () -> removeNausea,
            value -> removeNausea = value,
            true
        ));
    }
}
