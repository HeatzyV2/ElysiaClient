package net.elysiastudios.client.module.impl;

public class AirStatusModule extends TextHudModule {
    public AirStatusModule() {
        super("AirStatus", "Affiche l'oxygène restant sous l'eau.", "AIR");
    }

    @Override
    protected String getText() {
        if (mc.player == null) return null;
        int max = mc.player.getMaxAirSupply();
        int air = mc.player.getAirSupply();
        if (air >= max) return "Air OK";
        return "Air " + Math.round(air * 100.0F / max) + "%";
    }

    @Override
    protected int getAccentColor() {
        return 0xFF06B6D4;
    }
}
