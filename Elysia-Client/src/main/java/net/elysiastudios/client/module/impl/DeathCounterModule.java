package net.elysiastudios.client.module.impl;

public class DeathCounterModule extends TextHudModule {
    private boolean wasAlive = true;
    private int deaths;

    public DeathCounterModule() {
        super("DeathCounter", "Compte vos morts pendant la session.", "DEAD");
    }

    @Override
    protected String getText() {
        if (mc.player == null) return null;
        boolean alive = mc.player.isAlive();
        if (wasAlive && !alive) {
            deaths++;
        }
        wasAlive = alive;
        return "Deaths " + deaths;
    }

    @Override
    protected int getAccentColor() {
        return 0xFFEF4444;
    }
}
