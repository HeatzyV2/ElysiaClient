package net.elysiastudios.client.module.impl;

import net.minecraft.world.item.ItemStack;

public class HeldDurabilityModule extends TextHudModule {
    public HeldDurabilityModule() {
        super("HeldDurability", "Affiche la durabilité de l'item tenu.", "HDUR");
    }

    @Override
    protected String getText() {
        if (mc.player == null) return null;
        ItemStack stack = mc.player.getMainHandItem();
        if (stack.isEmpty() || !stack.isDamageableItem()) return "Held Dura N/A";
        int remaining = stack.getMaxDamage() - stack.getDamageValue();
        int percent = Math.round(remaining * 100.0F / stack.getMaxDamage());
        return "Held Dura " + percent + "%";
    }
}
