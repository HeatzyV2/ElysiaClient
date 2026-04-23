package net.elysiastudios.client.module.impl.smartfight;

public class TimingAnalyzer {
    public Result analyze(FightSession session, SmartFightSettings settings) {
        CombatProfile profile = CombatProfile.forMode(settings.combatMode);
        float accuracy = session.getAccuracy();
        boolean ignoresCooldown = settings.combatMode == SmartFightSettings.CombatMode.PVP_1_8;
        boolean hasSwingStrength = !session.getSwingStrengths().isEmpty();
        float neutralStrength = settings.combatMode == SmartFightSettings.CombatMode.CRYSTAL_PVP ? 0.74F : 0.62F;
        float averageStrength = ignoresCooldown ? 1.0F : hasSwingStrength ? AnalysisMath.averageFloat(session.getSwingStrengths()) : neutralStrength;
        float readyRatio = ignoresCooldown ? 1.0F : hasSwingStrength ? AnalysisMath.ratioAtLeast(session.getSwingStrengths(), profile.readyStrength()) : neutralStrength;
        float strongRatio = ignoresCooldown ? 1.0F : hasSwingStrength ? AnalysisMath.ratioAtLeast(session.getSwingStrengths(), profile.strongStrength()) : Math.max(0.48F, neutralStrength - 0.08F);
        double averageInterval = AnalysisMath.averageLong(session.getActionIntervals());
        double intervalStdDev = AnalysisMath.stdDevLong(session.getActionIntervals(), averageInterval);
        float consistency = session.getActionIntervals().size() < 2
            ? 0.62F
            : 1.0F - AnalysisMath.clamp((float) (intervalStdDev / profile.intervalStabilityScale()), 0.0F, 0.88F);
        float cadenceFit = session.getActionIntervals().isEmpty()
            ? 0.62F
            : 1.0F - AnalysisMath.clamp((float) (Math.abs(averageInterval - profile.idealIntervalMs()) / profile.intervalToleranceMs()), 0.0F, 1.0F);
        float spamRatio = AnalysisMath.ratioBelow(session.getActionIntervals(), profile.fastIntervalMs());
        float wasteRatio = session.getTotalSwings() <= 0 ? 0.0F : (float) session.getMisses() / (float) session.getTotalSwings();
        float discipline = ignoresCooldown
            ? AnalysisMath.clamp(
                (cadenceFit * 0.48F)
                    + (consistency * 0.28F)
                    + ((1.0F - spamRatio) * 0.24F),
                0.0F,
                1.0F
            )
            : AnalysisMath.clamp(
                (readyRatio * 0.52F)
                    + (strongRatio * 0.12F)
                    + (cadenceFit * 0.18F)
                    + ((1.0F - spamRatio) * 0.18F),
                0.0F,
                1.0F
            );
        float score = ignoresCooldown
            ? AnalysisMath.clamp(
                (discipline * 0.52F)
                    + (consistency * 0.22F)
                    + ((1.0F - wasteRatio) * 0.14F)
                    + (accuracy * 0.12F),
                0.0F,
                1.0F
            )
            : AnalysisMath.clamp(
                (discipline * 0.48F)
                    + (consistency * 0.24F)
                    + ((1.0F - wasteRatio) * 0.16F)
                    + (accuracy * 0.12F),
                0.0F,
                1.0F
            );

        SmartFightStates.TimingState state;
        String insight;
        if (session.getTotalSwings() < 3 && session.getCrystalActions() < 2) {
            state = SmartFightStates.TimingState.SOLID;
            insight = "Lecture en montée";
        } else if (ignoresCooldown && spamRatio > 0.55F && wasteRatio > 0.44F) {
            state = SmartFightStates.TimingState.WASTEFUL;
            insight = "CPS trop brouillon";
        } else if (!ignoresCooldown && spamRatio > 0.50F && readyRatio < 0.50F) {
            state = SmartFightStates.TimingState.WASTEFUL;
            insight = switch (settings.combatMode) {
                case CRYSTAL_PVP -> "Tempo trop brouillon même pour crystal";
                case MACE -> "Swings trop pressés pour la mace";
                case JAVA_COOLDOWN -> "Swings hors cooldown trop fréquents";
                case PVP_1_8 -> "CPS trop brouillon";
            };
        } else if (!ignoresCooldown && (readyRatio < 0.42F || averageStrength < profile.readyStrength() - 0.08F)) {
            state = SmartFightStates.TimingState.FORCED;
            insight = switch (settings.combatMode) {
                case CRYSTAL_PVP -> "Les entrées melee manquent de propreté";
                case MACE -> "Il faut laisser respirer le swing";
                case JAVA_COOLDOWN -> "Timing trop pressé pour le cooldown";
                case PVP_1_8 -> "Cadence trop forcée";
            };
        } else if (wasteRatio > 0.58F) {
            state = SmartFightStates.TimingState.WASTEFUL;
            insight = "Trop d'actions non converties";
        } else if (score > 0.82F && (ignoresCooldown || readyRatio > 0.70F) && spamRatio < (ignoresCooldown ? 0.42F : 0.24F)) {
            state = SmartFightStates.TimingState.GOOD;
            insight = switch (settings.combatMode) {
                case CRYSTAL_PVP -> "Fenêtres propres entre les resets";
                case MACE -> "Ouvertures lourdes bien exploitées";
                case JAVA_COOLDOWN -> "Cooldown bien exploité";
                case PVP_1_8 -> "Combo 1.8 bien cadencé";
            };
        } else if (score > 0.66F) {
            state = SmartFightStates.TimingState.SOLID;
            insight = "Cadence globalement propre";
        } else {
            state = SmartFightStates.TimingState.SLIPPING;
            insight = "Timing qui glisse";
        }

        return new Result(state, score, consistency, averageStrength, wasteRatio, readyRatio, spamRatio, discipline, insight);
    }

    public record Result(
        SmartFightStates.TimingState state,
        float score,
        float consistency,
        float averageStrength,
        float wasteRatio,
        float readyRatio,
        float spamRatio,
        float discipline,
        String insight
    ) {
    }
}
