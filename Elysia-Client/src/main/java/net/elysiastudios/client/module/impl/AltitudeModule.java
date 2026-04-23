package net.elysiastudios.client.module.impl;

public class AltitudeModule extends TextHudModule {
    public AltitudeModule() {
        super("Altitude", "Affiche votre hauteur actuelle.", "ALT");
    }

    @Override
    protected String getText() {
        if (mc.player == null) return null;
        return "Altitude Y " + (int) mc.player.getY();
    }
}
