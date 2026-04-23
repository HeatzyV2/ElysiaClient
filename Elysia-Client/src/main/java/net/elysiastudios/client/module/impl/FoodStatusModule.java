package net.elysiastudios.client.module.impl;

public class FoodStatusModule extends TextHudModule {
    public FoodStatusModule() {
        super("FoodStatus", "Affiche la faim et la saturation.", "FOOD");
    }

    @Override
    protected String getText() {
        if (mc.player == null) return null;
        return String.format("Food %d/20  Sat %.1f", mc.player.getFoodData().getFoodLevel(), mc.player.getFoodData().getSaturationLevel());
    }

    @Override
    protected int getAccentColor() {
        return 0xFFF97316;
    }
}
