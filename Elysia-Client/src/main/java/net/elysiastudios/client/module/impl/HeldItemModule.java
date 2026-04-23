package net.elysiastudios.client.module.impl;

import net.minecraft.world.item.ItemStack;

public class HeldItemModule extends TextHudModule {
    public HeldItemModule() {
        super("HeldItem", "Affiche l'item tenu en main.", "ITEM");
    }

    @Override
    protected String getText() {
        if (mc.player == null) return null;
        ItemStack stack = mc.player.getMainHandItem();
        if (stack.isEmpty()) return "Main: Empty";
        return "Main: " + stack.getHoverName().getString() + " x" + stack.getCount();
    }
}
