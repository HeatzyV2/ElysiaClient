package net.elysiastudios.client.module.impl;

public class WorldTimeModule extends TextHudModule {
    public WorldTimeModule() {
        super("WorldTime", "Affiche l'heure Minecraft.", "WT");
    }

    @Override
    protected String getText() {
        if (mc.level == null) return null;
        long ticks = (mc.level.getDayTime() + 6000L) % 24000L;
        long hours = ticks / 1000L;
        long minutes = (ticks % 1000L) * 60L / 1000L;
        return String.format("MC Time %02d:%02d", hours, minutes);
    }
}
