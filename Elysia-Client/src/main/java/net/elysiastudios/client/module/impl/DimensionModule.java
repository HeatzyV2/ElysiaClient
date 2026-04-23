package net.elysiastudios.client.module.impl;

public class DimensionModule extends TextHudModule {
    public DimensionModule() {
        super("Dimension", "Affiche la dimension actuelle.", "DIM");
    }

    @Override
    protected String getText() {
        if (mc.level == null) return null;
        String dimension = mc.level.dimension().toString();
        int slash = dimension.lastIndexOf('/');
        if (slash >= 0) dimension = dimension.substring(slash + 1);
        int colon = dimension.lastIndexOf(':');
        if (colon >= 0) dimension = dimension.substring(colon + 1);
        return "Dimension " + dimension.replace('_', ' ');
    }
}
