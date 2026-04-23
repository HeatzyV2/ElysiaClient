package net.elysiastudios.client.module.impl;

import net.minecraft.world.entity.Entity;

public class MountSpeedModule extends TextHudModule {
    public MountSpeedModule() {
        super("MountSpeed", "Affiche la vitesse de votre monture.", "MSPD");
    }

    @Override
    protected String getText() {
        if (mc.player == null || !mc.player.isPassenger()) return null;
        Entity vehicle = mc.player.getVehicle();
        if (vehicle == null) return null;
        double dx = vehicle.getX() - vehicle.xOld;
        double dz = vehicle.getZ() - vehicle.zOld;
        return String.format("Mount %.1f b/s", Math.sqrt(dx * dx + dz * dz) * 20.0D);
    }
}
