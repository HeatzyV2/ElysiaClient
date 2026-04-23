package net.elysiastudios.client.module.impl;

public class FpsAverageModule extends TextHudModule {
    private float averageFps;

    public FpsAverageModule() {
        super("FpsAverage", "Affiche une moyenne FPS lisse.", "AVG");
    }

    @Override
    protected String getText() {
        int fps = mc.getFps();
        if (averageFps <= 0.0F) {
            averageFps = fps;
        }
        averageFps += (fps - averageFps) * 0.05F;
        return "Avg FPS " + Math.round(averageFps);
    }

    @Override
    protected int getAccentColor() {
        return averageFps >= 60.0F ? 0xFF22C55E : 0xFFF59E0B;
    }
}
