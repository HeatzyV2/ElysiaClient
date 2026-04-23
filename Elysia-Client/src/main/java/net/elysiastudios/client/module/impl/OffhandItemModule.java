package net.elysiastudios.client.module.impl;

import net.minecraft.world.item.ItemStack;

public class OffhandItemModule extends TextHudModule {
    public OffhandItemModule() {
        super("OffhandItem", "Affiche l'item en main secondaire.", "OFF");
    }

    @Override
    protected String getText() {
        if (mc.player == null) return null;
        ItemStack stack = mc.player.getOffhandItem();
        if (stack.isEmpty()) return "Offhand Empty";
        return "Offhand " + stack.getHoverName().getString() + " x" + stack.getCount();
    }
}
