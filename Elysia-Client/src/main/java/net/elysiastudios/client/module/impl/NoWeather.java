package net.elysiastudios.client.module.impl;

import net.elysiastudios.client.module.Category;
import net.elysiastudios.client.module.Module;
import net.elysiastudios.client.setting.FloatModuleSetting;

import java.util.Locale;

public class NoWeather extends Module {
    private float rainLevel;
    private float thunderLevel;

    public NoWeather() {
        super("NoWeather", "Supprime la pluie et la neige.", Category.RENDER, 0, "☀️");
        this.rainLevel = 0.0F;
        this.thunderLevel = 0.0F;
        registerWeatherSettings();
    }

    @Override
    public void onTick() {
        if (mc.level != null) {
            mc.level.setRainLevel(rainLevel);
            mc.level.setThunderLevel(thunderLevel);
        }
    }

    private void registerWeatherSettings() {
        addSetting(new FloatModuleSetting(
            "weather_rain_level",
            "Pluie",
            "Force le niveau de pluie côté client.",
            "Météo",
            true,
            () -> rainLevel,
            value -> rainLevel = value,
            0.0F,
            0.0F,
            1.0F,
            0.1F,
            value -> String.format(Locale.ROOT, "%.0f%%", value * 100.0F)
        ));
        addSetting(new FloatModuleSetting(
            "weather_thunder_level",
            "Tonnerre",
            "Force l'intensité de l'orage côté client.",
            "Météo",
            true,
            () -> thunderLevel,
            value -> thunderLevel = value,
            0.0F,
            0.0F,
            1.0F,
            0.1F,
            value -> String.format(Locale.ROOT, "%.0f%%", value * 100.0F)
        ));
    }
}
