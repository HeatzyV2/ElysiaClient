package net.elysiastudios.client.module.impl;

public class LightLevelModule extends TextHudModule {
    public LightLevelModule() {
        super("LightLevel", "Affiche le niveau de lumière local.", "LUX");
    }

    @Override
    protected String getText() {
        if (mc.player == null || mc.level == null) return null;
        int light = mc.level.getMaxLocalRawBrightness(mc.player.blockPosition());
        return "Light " + light + "/15";
    }

    @Override
    protected int getAccentColor() {
        return 0xFFFACC15;
    }
}
