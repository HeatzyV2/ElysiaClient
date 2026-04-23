package net.elysiastudios.client.module.impl;

import net.minecraft.world.entity.Entity;

public class MountStatusModule extends TextHudModule {
    public MountStatusModule() {
        super("MountStatus", "Affiche votre monture actuelle.", "MNT");
    }

    @Override
    protected String getText() {
        if (mc.player == null || !mc.player.isPassenger()) return "Aucune monture";
        Entity vehicle = mc.player.getVehicle();
        return "Mount " + (vehicle == null ? "Unknown" : vehicle.getName().getString());
    }
}
