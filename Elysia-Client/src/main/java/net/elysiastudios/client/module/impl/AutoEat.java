package net.elysiastudios.client.module.impl;

import net.elysiastudios.client.module.Category;
import net.elysiastudios.client.module.Module;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;

public class AutoEat extends Module {
    public AutoEat() {
        super("AutoEat", "Mange automatiquement quand vous avez faim.", Category.UTILITY, 0, "🍎");
    }

    @Override
    public void onTick() {
        if (mc.player == null) return;

        if (mc.player.getFoodData().needsFood()) {
            for (int i = 0; i < 9; i++) {
                ItemStack stack = mc.player.getInventory().getItem(i);
                if (stack.has(DataComponents.FOOD)) {
                    ((net.elysiastudios.mixin.client.InventoryAccessor)mc.player.getInventory()).setSelectedSlot(i);
                    mc.options.keyUse.setDown(true);
                    return;
                }
            }
        } else {
            // On ne reset pas forcément ici car ça pourrait stopper d'autres actions
        }
    }
}
