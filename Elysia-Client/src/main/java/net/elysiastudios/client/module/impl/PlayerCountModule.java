package net.elysiastudios.client.module.impl;

public class PlayerCountModule extends TextHudModule {
    public PlayerCountModule() {
        super("PlayerCount", "Affiche le nombre de joueurs connectes.", "PLY");
    }

    @Override
    protected String getText() {
        if (mc.getConnection() == null) return "Players 1";
        return "Players " + mc.getConnection().getOnlinePlayers().size();
    }
}
