package net.elysiastudios.client.module.impl;

public class InventorySlotsModule extends TextHudModule {
    public InventorySlotsModule() {
        super("InventorySlots", "Affiche le nombre de slots libres.", "SLOT");
    }

    @Override
    protected String getText() {
        if (mc.player == null) return null;
        int free = 0;
        for (int i = 0; i < mc.player.getInventory().getContainerSize(); i++) {
            if (mc.player.getInventory().getItem(i).isEmpty()) {
                free++;
            }
        }
        return "Free Slots " + free;
    }
}
