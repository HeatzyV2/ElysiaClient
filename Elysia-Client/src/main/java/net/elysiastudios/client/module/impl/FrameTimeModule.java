package net.elysiastudios.client.module.impl;

public class FrameTimeModule extends TextHudModule {
    public FrameTimeModule() {
        super("FrameTime", "Affiche le temps moyen par image.", "MS");
    }

    @Override
    protected String getText() {
        int fps = Math.max(1, mc.getFps());
        return String.format("Frame %.2f ms", 1000.0D / fps);
    }
}
