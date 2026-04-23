package net.elysiastudios.client.module.impl;

public class VelocityVectorModule extends TextHudModule {
    public VelocityVectorModule() {
        super("VelocityVector", "Affiche votre velocite X/Y/Z.", "VEL");
    }

    @Override
    protected String getText() {
        if (mc.player == null) return null;
        double vx = (mc.player.getX() - mc.player.xOld) * 20.0D;
        double vy = (mc.player.getY() - mc.player.yOld) * 20.0D;
        double vz = (mc.player.getZ() - mc.player.zOld) * 20.0D;
        return String.format("Vel %.1f / %.1f / %.1f", vx, vy, vz);
    }
}
