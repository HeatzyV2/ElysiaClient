package net.elysiastudios.client.module.impl;

public class DistanceTravelledModule extends TextHudModule {
    private boolean initialized;
    private double lastX;
    private double lastZ;
    private double travelled;

    public DistanceTravelledModule() {
        super("DistanceTravelled", "Compteur de distance parcourue en session.", "DIST");
    }

    @Override
    protected String getText() {
        if (mc.player == null) return null;
        if (!initialized) {
            initialized = true;
            lastX = mc.player.getX();
            lastZ = mc.player.getZ();
        }

        double dx = mc.player.getX() - lastX;
        double dz = mc.player.getZ() - lastZ;
        double step = Math.sqrt(dx * dx + dz * dz);
        if (step < 20.0D) {
            travelled += step;
        }
        lastX = mc.player.getX();
        lastZ = mc.player.getZ();
        return String.format("Travel %.0f blocks", travelled);
    }
}
