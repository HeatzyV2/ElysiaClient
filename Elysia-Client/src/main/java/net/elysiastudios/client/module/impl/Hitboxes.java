package net.elysiastudios.client.module.impl;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;

public class Hitboxes extends TextHudModule {
    public Hitboxes() {
        super("EntityInfo", "Affiche les infos de l'entité visée.", "ENT");
    }

    @Override
    protected String getText() {
        if (!(mc.hitResult instanceof EntityHitResult entityHitResult)) {
            return null;
        }

        Entity entity = entityHitResult.getEntity();
        if (entity instanceof LivingEntity living) {
            return String.format("%s  HP %.1f/%.1f", living.getName().getString(), living.getHealth(), living.getMaxHealth());
        }
        return entity.getName().getString();
    }
}
