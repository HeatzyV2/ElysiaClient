package net.elysiastudios.client.module.impl;

import net.elysiastudios.client.module.Category;

public class CopyNetherCoordinatesModule extends InstantActionModule {
    public CopyNetherCoordinatesModule() {
        super("CopyNetherCoords", "Copie la conversion Nether/Overworld.", Category.UTILITY, "8X");
    }

    @Override
    protected void execute() {
        if (mc.player == null || mc.level == null) {
            toast("Aucune position à convertir");
            return;
        }
        boolean nether = mc.level.dimension().toString().contains("the_nether");
        double factor = nether ? 8.0D : 0.125D;
        String target = nether ? "Overworld" : "Nether";
        String coords = String.format("%s: %.1f %.1f", target, mc.player.getX() * factor, mc.player.getZ() * factor);
        mc.keyboardHandler.setClipboard(coords);
        toast("Conversion copiée : " + coords);
    }
}
