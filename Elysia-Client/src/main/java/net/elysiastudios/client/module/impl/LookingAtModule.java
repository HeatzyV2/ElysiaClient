package net.elysiastudios.client.module.impl;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class LookingAtModule extends TextHudModule {
    public LookingAtModule() {
        super("LookingAt", "Affiche le bloc que vous visez.", "LOOK");
    }

    @Override
    protected String getText() {
        if (mc.level == null || !(mc.hitResult instanceof BlockHitResult hit)) return null;
        BlockState state = mc.level.getBlockState(hit.getBlockPos());
        return "Block: " + state.getBlock().getName().getString();
    }
}
