package net.elysiastudios.client.module.impl;

import net.elysiastudios.client.module.Category;
import net.elysiastudios.client.module.Module;
import net.elysiastudios.client.module.ModuleManager;
import net.elysiastudios.client.setting.BooleanModuleSetting;
import net.minecraft.world.phys.EntityHitResult;

public class NoHurtCam extends Module {
    private boolean onlyFirstPerson;
    private boolean onlyWhenTargetingEntity;

    public NoHurtCam() {
        super("NoHurtCam", "Réduit les secousses de caméra quand vous êtes touché.", Category.COMBAT, 0, "🎥");
        this.onlyFirstPerson = true;
        this.onlyWhenTargetingEntity = false;
        registerHurtCamSettings();
    }

    public static boolean shouldSuppressHurtCam() {
        NoHurtCam module = ModuleManager.getInstance().getModule(NoHurtCam.class);
        if (module == null || !module.isEnabled() || mc == null || mc.player == null) {
            return false;
        }
        if (module.onlyFirstPerson && !mc.options.getCameraType().isFirstPerson()) {
            return false;
        }
        return !module.onlyWhenTargetingEntity || mc.hitResult instanceof EntityHitResult;
    }

    private void registerHurtCamSettings() {
        addSetting(new BooleanModuleSetting(
            "hurtcam_first_person",
            "Seulement en vue joueur",
            "Ne supprime l'effet que si la caméra est en première personne.",
            "Caméra",
            true,
            () -> onlyFirstPerson,
            value -> onlyFirstPerson = value,
            true
        ));
        addSetting(new BooleanModuleSetting(
            "hurtcam_target_only",
            "Seulement sur cible",
            "N'agit que lorsque le viseur pointe une entité.",
            "Caméra",
            true,
            () -> onlyWhenTargetingEntity,
            value -> onlyWhenTargetingEntity = value,
            false
        ));
    }
}
