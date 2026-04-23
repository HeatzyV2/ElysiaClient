package net.elysiastudios.client.module.impl;

public class HotbarSlotModule extends TextHudModule {
    public HotbarSlotModule() {
        super("HotbarSlot", "Affiche le slot de hotbar sélectionné.", "SLOT");
    }

    @Override
    protected String getText() {
        if (mc.player == null) return null;
        return "Hotbar Slot " + (mc.player.getInventory().getSelectedSlot() + 1);
    }
}
