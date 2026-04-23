package net.elysiastudios.client.module.impl;

import net.minecraft.world.item.Items;

public class ArrowCounterModule extends TextHudModule {
    public ArrowCounterModule() {
        super("ArrowCounter", "Compte les flèches dans l'inventaire.", "ARW");
    }

    @Override
    protected String getText() {
        if (mc.player == null) return null;
        int arrows = mc.player.getInventory().countItem(Items.ARROW) + mc.player.getInventory().countItem(Items.SPECTRAL_ARROW);
        return "Arrows " + arrows;
    }
}
