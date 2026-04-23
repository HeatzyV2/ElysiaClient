package net.elysiastudios.client.module.impl;

public class HealthStatusModule extends TextHudModule {
    public HealthStatusModule() {
        super("HealthStatus", "Affiche votre vie exacte.", "HP");
    }

    @Override
    protected String getText() {
        if (mc.player == null) return null;
        float health = mc.player.getHealth() + mc.player.getAbsorptionAmount();
        return String.format("HP %.1f / %.1f", health, mc.player.getMaxHealth());
    }

    @Override
    protected int getAccentColor() {
        return 0xFFEF4444;
    }
}
