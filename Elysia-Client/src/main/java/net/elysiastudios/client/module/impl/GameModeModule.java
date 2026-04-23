package net.elysiastudios.client.module.impl;

public class GameModeModule extends TextHudModule {
    public GameModeModule() {
        super("GameMode", "Affiche votre mode de jeu local.", "MODE");
    }

    @Override
    protected String getText() {
        if (mc.player == null) return null;
        if (mc.player.isSpectator()) return "Mode Spectator";
        if (mc.player.isCreative()) return "Mode Creative";
        return "Mode Survival";
    }
}
