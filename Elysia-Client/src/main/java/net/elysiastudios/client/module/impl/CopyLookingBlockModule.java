package net.elysiastudios.client.module.impl;

import net.elysiastudios.client.module.Category;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;

public class CopyLookingBlockModule extends InstantActionModule {
    public CopyLookingBlockModule() {
        super("CopyLookingBlock", "Copie les coordonnées du bloc visé.", Category.UTILITY, "CBLK");
    }

    @Override
    protected void execute() {
        if (!(mc.hitResult instanceof BlockHitResult hit)) {
            toast("Aucun bloc visé");
            return;
        }
        BlockPos pos = hit.getBlockPos();
        String coords = pos.getX() + " " + pos.getY() + " " + pos.getZ();
        mc.keyboardHandler.setClipboard(coords);
        toast("Bloc copié : " + coords);
    }
}
