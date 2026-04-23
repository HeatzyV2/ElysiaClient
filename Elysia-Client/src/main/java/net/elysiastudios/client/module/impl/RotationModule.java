package net.elysiastudios.client.module.impl;

public class RotationModule extends TextHudModule {
    public RotationModule() {
        super("Rotation", "Affiche yaw et pitch.", "ROT");
    }

    @Override
    protected String getText() {
        if (mc.player == null) return null;
        return String.format("Yaw %.1f  Pitch %.1f", mc.player.getYRot(), mc.player.getXRot());
    }
}
