package net.elysiastudios.client.module.impl;

public class LastDeathModule extends TextHudModule {
    private boolean wasAlive = true;
    private String lastDeath = "No death";

    public LastDeathModule() {
        super("LastDeath", "Mémorise les dernières coordonnées de mort.", "RIP");
    }

    @Override
    protected String getText() {
        if (mc.player == null) return null;
        boolean alive = mc.player.isAlive();
        if (wasAlive && !alive) {
            lastDeath = String.format("%.0f %.0f %.0f", mc.player.getX(), mc.player.getY(), mc.player.getZ());
        }
        wasAlive = alive;
        return "Last Death: " + lastDeath;
    }

    @Override
    protected int getAccentColor() {
        return 0xFFEF4444;
    }
}
