package net.elysiastudios.client.module.impl;

import net.elysiastudios.client.module.Category;
import net.elysiastudios.client.module.Module;
import net.elysiastudios.client.module.ModuleManager;
import net.elysiastudios.client.setting.BooleanModuleSetting;
import net.elysiastudios.client.setting.IntModuleSetting;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public class EntityCullingLite extends Module {
    private int distanceBlocks;
    private boolean preserveNamedEntities;

    public EntityCullingLite() {
        super("EntityCullingLite", "Coupe le rendu des entités non-joueurs trop loin pour gagner des FPS.", Category.OPTIMIZATION, 0, "ENT");
        this.distanceBlocks = 64;
        this.preserveNamedEntities = true;
        registerCullingSettings();
    }

    public static boolean shouldSkip(Entity entity, double cameraX, double cameraY, double cameraZ) {
        EntityCullingLite module = ModuleManager.getInstance().getModule(EntityCullingLite.class);
        if (module == null || !module.isEnabled() || entity == null || entity instanceof Player) {
            return false;
        }
        if (module.preserveNamedEntities && entity.hasCustomName()) {
            return false;
        }

        double maxDistanceSqr = module.distanceBlocks * (double) module.distanceBlocks;
        return entity.distanceToSqr(cameraX, cameraY, cameraZ) > maxDistanceSqr;
    }

    private void registerCullingSettings() {
        addSetting(new IntModuleSetting(
            "culling_distance",
            "Distance max",
            "Distance à partir de laquelle les entités sont masquées.",
            "Culling",
            true,
            () -> distanceBlocks,
            value -> distanceBlocks = value,
            64,
            16,
            160,
            8,
            value -> value + " blocs"
        ));
        addSetting(new BooleanModuleSetting(
            "culling_preserve_named",
            "Préserver les noms",
            "Garde les entités avec un nom personnalisé.",
            "Culling",
            true,
            () -> preserveNamedEntities,
            value -> preserveNamedEntities = value,
            true
        ));
    }
}
