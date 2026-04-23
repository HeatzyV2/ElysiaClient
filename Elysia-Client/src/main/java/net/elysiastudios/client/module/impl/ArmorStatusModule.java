package net.elysiastudios.client.module.impl;

import net.elysiastudios.client.module.HudModule;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

public class ArmorStatusModule extends HudModule {
    public ArmorStatusModule() {
        super("ArmorStatus", "Affiche la durabilité de votre armure", 0, "🛡");
    }

    @Override
    public int getEditorWidth() {
        return getHudPadding() * 2 + 56;
    }

    @Override
    public int getEditorHeight() {
        return getHudPadding() * 2 + 72;
    }

    @Override
    public void render(GuiGraphics guiGraphics) {
        beginHudRender(guiGraphics);
        drawHudFrame(guiGraphics, 56 + getHudPadding() * 2, 72 + getHudPadding() * 2, 0xFFA855F7);

        EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        int itemY = getHudPadding();
        for (EquipmentSlot slot : slots) {
            ItemStack stack = mc.player != null ? mc.player.getItemBySlot(slot) : ItemStack.EMPTY;
            if (isPreviewRender() && stack.isEmpty()) {
                drawHudText(guiGraphics, slot.name().substring(0, 1), getHudPadding() + 4, itemY + 4, 0xFF94A3B8);
            } else if (!stack.isEmpty()) {
                guiGraphics.renderItem(stack, getHudPadding(), itemY);
            }

            if (!stack.isEmpty() && stack.isDamageableItem()) {
                int max = stack.getMaxDamage();
                int current = max - stack.getDamageValue();
                int percent = (int) ((current / (float) max) * 100);
                int color = percent > 50 ? 0xFF22C55E : percent > 25 ? 0xFFF59E0B : 0xFFEF4444;
                drawHudText(guiGraphics, percent + "%", getHudPadding() + 20, itemY + 5, color);
            } else if (isPreviewRender()) {
                drawHudText(guiGraphics, "100%", getHudPadding() + 20, itemY + 5, 0xFF22C55E);
            }
            itemY += 18;
        }

        endHudRender(guiGraphics);
    }
}
