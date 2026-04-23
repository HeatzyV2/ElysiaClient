package net.elysiastudios.client.module.impl;

import net.elysiastudios.client.module.Category;
import net.elysiastudios.client.module.Module;
import net.elysiastudios.client.module.ModuleManager;
import net.elysiastudios.client.setting.BooleanModuleSetting;
import net.elysiastudios.client.setting.EnumModuleSetting;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import org.lwjgl.glfw.GLFW;

public class Fullbright extends Module {
    private BrightnessMode mode;
    private boolean hideNightVisionParticles;

    public Fullbright() {
        super("Fullbright", "Vision nocturne infinie.", Category.RENDER, GLFW.GLFW_KEY_F);
        this.mode = BrightnessMode.HYBRID;
        this.hideNightVisionParticles = true;
        registerFullbrightSettings();
    }

    @Override
    public void onEnable() {
        syncNightVision();
    }

    @Override
    public void onTick() {
        syncNightVision();
    }

    @Override
    public void onDisable() {
        if (mc.player != null) {
            mc.player.removeEffect(MobEffects.NIGHT_VISION);
        }
    }

    public static boolean shouldForceLightmap() {
        Fullbright module = ModuleManager.getInstance().getModule(Fullbright.class);
        return module != null && module.isEnabled() && module.mode != BrightnessMode.NIGHT_VISION_ONLY;
    }

    private void syncNightVision() {
        if (mc.player == null) {
            return;
        }

        if (mode == BrightnessMode.LIGHTMAP_ONLY) {
            mc.player.removeEffect(MobEffects.NIGHT_VISION);
            return;
        }

        if (!mc.player.hasEffect(MobEffects.NIGHT_VISION)) {
            mc.player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 220, 0, true, !hideNightVisionParticles));
        }
    }

    private void registerFullbrightSettings() {
        addSetting(new EnumModuleSetting<>(
            "fullbright_mode",
            "Mode",
            "Choisit entre lumière forcée, vision nocturne ou hybride.",
            "Fullbright",
            true,
            () -> mode,
            value -> mode = value,
            BrightnessMode.HYBRID,
            BrightnessMode.values(),
            BrightnessMode::getLabel
        ));
        addSetting(new BooleanModuleSetting(
            "fullbright_hide_particles",
            "Masquer les particules",
            "Cache les particules de vision nocturne si le mode les utilise.",
            "Fullbright",
            true,
            () -> hideNightVisionParticles,
            value -> hideNightVisionParticles = value,
            true
        ));
    }

    private enum BrightnessMode {
        HYBRID("Hybride"),
        LIGHTMAP_ONLY("Lumière forcée"),
        NIGHT_VISION_ONLY("Vision nocturne");

        private final String label;

        BrightnessMode(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }
}
