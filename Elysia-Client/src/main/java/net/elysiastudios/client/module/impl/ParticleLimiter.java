package net.elysiastudios.client.module.impl;

import net.elysiastudios.client.module.Category;
import net.elysiastudios.client.module.Module;
import net.elysiastudios.client.module.ModuleManager;
import net.elysiastudios.client.setting.BooleanModuleSetting;
import net.elysiastudios.client.setting.EnumModuleSetting;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;

public class ParticleLimiter extends Module {
    private static int particleCounter;

    private LimitStrength strength;
    private boolean preserveCombatParticles;
    private boolean preserveWeatherParticles;

    public ParticleLimiter() {
        super("ParticleLimiter", "Réduit fortement les particules décoratives tout en gardant les importantes.", Category.OPTIMIZATION, 0, "PRT");
        this.strength = LimitStrength.AGGRESSIVE;
        this.preserveCombatParticles = true;
        this.preserveWeatherParticles = false;
        registerParticleSettings();
    }

    public static boolean shouldDrop(ParticleOptions options) {
        ParticleLimiter module = ModuleManager.getInstance().getModule(ParticleLimiter.class);
        if (module == null || !module.isEnabled() || options == null) {
            return false;
        }

        ParticleType<?> type = options.getType();
        if (module.preserveCombatParticles && isCombatParticle(type)) {
            return false;
        }
        if (module.preserveWeatherParticles && type == ParticleTypes.RAIN) {
            return false;
        }

        particleCounter = (particleCounter + 1) % module.strength.getModulo();
        return particleCounter != 0;
    }

    private static boolean isCombatParticle(ParticleType<?> type) {
        return type == ParticleTypes.EXPLOSION_EMITTER
            || type == ParticleTypes.EXPLOSION
            || type == ParticleTypes.FLASH
            || type == ParticleTypes.DAMAGE_INDICATOR
            || type == ParticleTypes.CRIT
            || type == ParticleTypes.ENCHANTED_HIT
            || type == ParticleTypes.SWEEP_ATTACK
            || type == ParticleTypes.TOTEM_OF_UNDYING;
    }

    private void registerParticleSettings() {
        addSetting(new EnumModuleSetting<>(
            "particle_strength",
            "Intensité",
            "Choisit combien de particules décoratives sont filtrées.",
            "Particules",
            true,
            () -> strength,
            value -> strength = value,
            LimitStrength.AGGRESSIVE,
            LimitStrength.values(),
            LimitStrength::getLabel
        ));
        addSetting(new BooleanModuleSetting(
            "particle_preserve_combat",
            "Préserver combat",
            "Garde les particules importantes pour le PvP.",
            "Particules",
            true,
            () -> preserveCombatParticles,
            value -> preserveCombatParticles = value,
            true
        ));
        addSetting(new BooleanModuleSetting(
            "particle_preserve_weather",
            "Préserver météo",
            "Laisse passer les particules de pluie.",
            "Particules",
            true,
            () -> preserveWeatherParticles,
            value -> preserveWeatherParticles = value,
            false
        ));
    }

    private enum LimitStrength {
        LIGHT("Léger", 2),
        NORMAL("Normal", 4),
        AGGRESSIVE("Agressif", 8);

        private final String label;
        private final int modulo;

        LimitStrength(String label, int modulo) {
            this.label = label;
            this.modulo = modulo;
        }

        public String getLabel() {
            return label;
        }

        public int getModulo() {
            return modulo;
        }
    }
}
