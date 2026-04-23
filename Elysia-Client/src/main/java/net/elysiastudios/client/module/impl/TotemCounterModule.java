package net.elysiastudios.client.module.impl;

import net.minecraft.world.item.Items;

public class TotemCounterModule extends TextHudModule {
    public TotemCounterModule() {
        super("TotemCounter", "Compte les totems dans l'inventaire.", "TOT");
    }

    @Override
    protected String getText() {
        if (mc.player == null) return null;
        return "Totems " + mc.player.getInventory().countItem(Items.TOTEM_OF_UNDYING);
    }

    @Override
    protected int getAccentColor() {
        return 0xFFFACC15;
    }
}
