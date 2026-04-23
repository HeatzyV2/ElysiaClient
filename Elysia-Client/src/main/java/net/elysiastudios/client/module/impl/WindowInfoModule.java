package net.elysiastudios.client.module.impl;

public class WindowInfoModule extends TextHudModule {
    public WindowInfoModule() {
        super("WindowInfo", "Affiche la résolution GUI.", "WIN");
    }

    @Override
    protected String getText() {
        return mc.getWindow().getGuiScaledWidth() + "x" + mc.getWindow().getGuiScaledHeight() + " GUI";
    }
}
