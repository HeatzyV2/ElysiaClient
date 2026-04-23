package net.elysiastudios.client.module.impl;

public class FluidStatusModule extends TextHudModule {
    public FluidStatusModule() {
        super("FluidStatus", "Indique si vous êtes dans l'eau ou la lave.", "FLD");
    }

    @Override
    protected String getText() {
        if (mc.player == null) return null;
        if (mc.player.isInLava()) return "Fluid Lava";
        if (mc.player.isInWater()) return "Fluid Water";
        return "Fluid None";
    }

    @Override
    protected int getAccentColor() {
        if (mc.player != null && mc.player.isInLava()) return 0xFFEF4444;
        if (mc.player != null && mc.player.isInWater()) return 0xFF38BDF8;
        return 0xFF64748B;
    }
}
