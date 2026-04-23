package net.elysiastudios.client.module.impl;

import net.elysiastudios.client.module.Category;
import net.elysiastudios.client.module.Module;

public class WeatherChanger extends Module {
    public WeatherChanger() {
        super("WeatherChanger", "Force un temps clair pour vous.", Category.RENDER, 0, "☁️");
    }

    @Override
    public void onTick() {
        if (mc.level != null) {
            mc.level.setRainLevel(0);
            mc.level.setThunderLevel(0);
        }
    }
}
