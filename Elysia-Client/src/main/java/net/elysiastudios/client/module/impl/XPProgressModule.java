package net.elysiastudios.client.module.impl;

public class XPProgressModule extends TextHudModule {
    public XPProgressModule() {
        super("XPProgress", "Affiche le niveau et la progression XP.", "XP");
    }

    @Override
    protected String getText() {
        if (mc.player == null) return null;
        int percent = Math.round(mc.player.experienceProgress * 100.0F);
        return "XP L" + mc.player.experienceLevel + "  " + percent + "%";
    }

    @Override
    protected int getAccentColor() {
        return 0xFF22C55E;
    }
}
