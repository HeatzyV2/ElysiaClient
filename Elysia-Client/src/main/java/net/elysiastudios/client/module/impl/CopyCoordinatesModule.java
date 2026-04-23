package net.elysiastudios.client.module.impl;

import net.elysiastudios.client.module.Category;

public class CopyCoordinatesModule extends InstantActionModule {
    public CopyCoordinatesModule() {
        super("CopyCoordinates", "Copie vos coordonnées dans le presse-papiers.", Category.UTILITY, "COPY");
    }

    @Override
    protected void execute() {
        if (mc.player == null) {
            toast("Aucune position à copier");
            return;
        }
        String coords = String.format("%.1f %.1f %.1f", mc.player.getX(), mc.player.getY(), mc.player.getZ());
        mc.keyboardHandler.setClipboard(coords);
        toast("Coordonnées copiées : " + coords);
    }
}
