package net.elysiastudios.client.module.impl;

public class VerticalSpeedModule extends TextHudModule {
    public VerticalSpeedModule() {
        super("VerticalSpeed", "Affiche votre vitesse verticale.", "VY");
    }

    @Override
    protected String getText() {
        if (mc.player == null) return null;
        double speed = (mc.player.getY() - mc.player.yOld) * 20.0D;
        return String.format("Vertical %.2f b/s", speed);
    }
}
