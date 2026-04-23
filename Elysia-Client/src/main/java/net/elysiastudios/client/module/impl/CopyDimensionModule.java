package net.elysiastudios.client.module.impl;

import net.elysiastudios.client.module.Category;

public class CopyDimensionModule extends InstantActionModule {
    public CopyDimensionModule() {
        super("CopyDimension", "Copie la dimension actuelle.", Category.UTILITY, "CDIM");
    }

    @Override
    protected void execute() {
        if (mc.level == null) {
            toast("Aucune dimension à copier");
            return;
        }
        String dimension = mc.level.dimension().toString();
        mc.keyboardHandler.setClipboard(dimension);
        toast("Dimension copiée");
    }
}
