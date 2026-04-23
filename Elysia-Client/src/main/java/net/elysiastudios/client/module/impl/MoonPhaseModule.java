package net.elysiastudios.client.module.impl;

public class MoonPhaseModule extends TextHudModule {
    private static final String[] PHASES = {
        "Full Moon", "Waning Gibbous", "Last Quarter", "Waning Crescent",
        "New Moon", "Waxing Crescent", "First Quarter", "Waxing Gibbous"
    };

    public MoonPhaseModule() {
        super("MoonPhase", "Affiche la phase de lune estimée.", "MOON");
    }

    @Override
    protected String getText() {
        if (mc.level == null) return null;
        int phase = (int) ((mc.level.getDayTime() / 24000L) % PHASES.length);
        return "Moon " + PHASES[phase];
    }
}
