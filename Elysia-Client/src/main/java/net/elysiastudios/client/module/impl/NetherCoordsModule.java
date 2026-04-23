package net.elysiastudios.client.module.impl;

public class NetherCoordsModule extends TextHudModule {
    public NetherCoordsModule() {
        super("NetherCoords", "Convertit vos coordonnées Nether/Overworld.", "8X");
    }

    @Override
    protected String getText() {
        if (mc.player == null || mc.level == null) return null;
        boolean nether = mc.level.dimension().toString().contains("the_nether");
        double factor = nether ? 8.0D : 0.125D;
        String target = nether ? "Overworld" : "Nether";
        return String.format("%s X %.0f Z %.0f", target, mc.player.getX() * factor, mc.player.getZ() * factor);
    }
}
