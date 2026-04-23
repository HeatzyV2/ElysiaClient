package net.elysiastudios.client.module.impl;

public class SprintStatusModule extends TextHudModule {
    public SprintStatusModule() {
        super("SprintStatus", "Affiche si vous sprintez.", "SPR");
    }

    @Override
    protected String getText() {
        if (mc.player == null) return null;
        return mc.player.isSprinting() ? "Sprint ON" : "Sprint OFF";
    }
}
