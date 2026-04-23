package net.elysiastudios.client.module.impl;

import net.elysiastudios.client.module.Category;

public class CopyPlayerStatsModule extends InstantActionModule {
    public CopyPlayerStatsModule() {
        super("CopyPlayerStats", "Copie un résumé joueur utile.", Category.UTILITY, "CSTAT");
    }

    @Override
    protected void execute() {
        if (mc.player == null || mc.level == null) {
            toast("Aucun joueur à copier");
            return;
        }
        String stats = String.format("XYZ %.1f %.1f %.1f | HP %.1f | Food %d | Dim %s",
            mc.player.getX(),
            mc.player.getY(),
            mc.player.getZ(),
            mc.player.getHealth(),
            mc.player.getFoodData().getFoodLevel(),
            mc.level.dimension());
        mc.keyboardHandler.setClipboard(stats);
        toast("Stats joueur copiées");
    }
}
