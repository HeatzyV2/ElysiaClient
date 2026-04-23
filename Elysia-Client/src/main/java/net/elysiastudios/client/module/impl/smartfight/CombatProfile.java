package net.elysiastudios.client.module.impl.smartfight;

record CombatProfile(
    double idealIntervalMs,
    double fastIntervalMs,
    double passiveIntervalMs,
    double intervalToleranceMs,
    double intervalStabilityScale,
    float readyStrength,
    float strongStrength,
    double idealDistance,
    double closeDistance,
    double farDistance,
    double unstableDeviation,
    float exchangeScale,
    float crystalTempoWeight
) {
    static CombatProfile forMode(SmartFightSettings.CombatMode mode) {
        return switch (mode) {
            case CRYSTAL_PVP -> new CombatProfile(
                340.0D,
                185.0D,
                620.0D,
                200.0D,
                220.0D,
                0.52F,
                0.76F,
                3.05D,
                2.02D,
                3.58D,
                0.72D,
                8.5F,
                0.24F
            );
            case MACE -> new CombatProfile(
                900.0D,
                540.0D,
                1_250.0D,
                320.0D,
                300.0D,
                0.86F,
                0.96F,
                2.38D,
                1.58D,
                2.96D,
                0.56D,
                5.6F,
                0.0F
            );
            case JAVA_COOLDOWN -> new CombatProfile(
                600.0D,
                360.0D,
                900.0D,
                230.0D,
                240.0D,
                0.82F,
                0.94F,
                2.72D,
                1.84D,
                3.18D,
                0.52D,
                6.4F,
                0.0F
            );
            case PVP_1_8 -> new CombatProfile(
                115.0D,
                65.0D,
                360.0D,
                120.0D,
                150.0D,
                0.0F,
                0.0F,
                2.85D,
                1.75D,
                3.35D,
                0.62D,
                10.5F,
                0.0F
            );
        };
    }
}
