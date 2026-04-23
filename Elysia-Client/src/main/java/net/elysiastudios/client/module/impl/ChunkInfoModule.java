package net.elysiastudios.client.module.impl;

import net.minecraft.core.BlockPos;

public class ChunkInfoModule extends TextHudModule {
    public ChunkInfoModule() {
        super("ChunkInfo", "Affiche le chunk et la position locale.", "CH");
    }

    @Override
    protected String getText() {
        if (mc.player == null) return null;
        BlockPos pos = mc.player.blockPosition();
        return "Chunk " + (pos.getX() >> 4) + ", " + (pos.getZ() >> 4) + "  Local " + (pos.getX() & 15) + ", " + (pos.getZ() & 15);
    }
}
