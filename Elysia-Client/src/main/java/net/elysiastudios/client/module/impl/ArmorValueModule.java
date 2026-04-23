package net.elysiastudios.client.module.impl;

public class ArmorValueModule extends TextHudModule {
    public ArmorValueModule() {
        super("ArmorValue", "Affiche les points d'armure.", "ARM");
    }

    @Override
    protected String getText() {
        if (mc.player == null) return null;
        return "Armor " + mc.player.getArmorValue();
    }

    @Override
    protected int getAccentColor() {
        return 0xFF94A3B8;
    }
}
