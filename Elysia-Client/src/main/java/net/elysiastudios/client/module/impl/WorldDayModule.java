package net.elysiastudios.client.module.impl;

public class WorldDayModule extends TextHudModule {
    public WorldDayModule() {
        super("WorldDay", "Affiche le jour du monde.", "DAY");
    }

    @Override
    protected String getText() {
        if (mc.level == null) return null;
        long day = Math.floorDiv(mc.level.getDayTime(), 24000L) + 1L;
        return "World Day " + day;
    }
}
