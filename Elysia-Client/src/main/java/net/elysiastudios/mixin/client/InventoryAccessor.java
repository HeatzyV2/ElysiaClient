package net.elysiastudios.mixin.client;

import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Inventory.class)
public interface InventoryAccessor {
    @Accessor("selected")
    void setSelectedSlot(int selected);
    
    @Accessor("selected")
    int getSelectedSlot();
}
