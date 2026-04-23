package net.elysiastudios.client.module.impl;

public class TargetDistanceModule extends TextHudModule {
    public TargetDistanceModule() {
        super("TargetDistance", "Affiche la distance du point vise.", "RNG");
    }

    @Override
    protected String getText() {
        if (mc.player == null || mc.hitResult == null) return null;
        double distance = mc.player.getEyePosition().distanceTo(mc.hitResult.getLocation());
        return String.format("Target %.2f blocks", distance);
    }
}
