package net.elysiastudios.client.module.impl;

public class WeatherStatusModule extends TextHudModule {
    public WeatherStatusModule() {
        super("WeatherStatus", "Affiche la météo locale.", "MÉTÉO");
    }

    @Override
    protected String getText() {
        if (mc.level == null) return null;
        if (mc.level.isThundering()) return "Weather Thunder";
        if (mc.level.isRaining()) return "Weather Rain";
        return "Weather Clear";
    }

    @Override
    protected int getAccentColor() {
        return 0xFF38BDF8;
    }
}
