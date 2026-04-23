package net.elysiastudios.client.module.impl;

public class SneakStatusModule extends TextHudModule {
    public SneakStatusModule() {
        super("SneakStatus", "Affiche si vous êtes accroupi.", "SNK");
    }

    @Override
    protected String getText() {
        if (mc.player == null) return null;
        return mc.player.isCrouching() ? "Sneak ON" : "Sneak OFF";
    }
}
