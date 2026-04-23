package net.elysiastudios.client.module.impl.smartfight;

public class ReachAnalyzer {
    public Result analyze(FightSession session, SmartFightSettings settings) {
        CombatProfile profile = CombatProfile.forMode(settings.combatMode);
        double averageDistance = AnalysisMath.averageDouble(session.getDistances());
        double deviation = AnalysisMath.stdDevDouble(session.getDistances(), averageDistance);
        double distanceWindow = Math.max(0.55D, Math.max(profile.idealDistance() - profile.closeDistance(), profile.farDistance() - profile.idealDistance()));
        float positioning = 1.0F - AnalysisMath.clamp((float) (Math.abs(averageDistance - profile.idealDistance()) / distanceWindow), 0.0F, 1.0F);
        float stability = 1.0F - AnalysisMath.clamp((float) (deviation / profile.unstableDeviation()), 0.0F, 1.0F);
        float score = AnalysisMath.clamp((positioning * 0.58F) + (stability * 0.42F), 0.0F, 1.0F);

        SmartFightStates.SpacingState state;
        String insight;
        if (session.getDistances().size() < 3) {
            state = SmartFightStates.SpacingState.OPTIMAL;
            insight = "Distance encore en lecture";
        } else if (averageDistance < profile.closeDistance()) {
            state = SmartFightStates.SpacingState.TOO_CLOSE;
            insight = switch (settings.combatMode) {
                case CRYSTAL_PVP -> "Trop collé pour respirer sur crystal";
                case MACE -> "Trop proche pour un lourd propre";
                case JAVA_COOLDOWN -> "Trop collé à la cible";
                case PVP_1_8 -> "Trop collé pour garder le strafe";
            };
        } else if (averageDistance > profile.farDistance() && session.getMisses() > 1) {
            state = SmartFightStates.SpacingState.TOO_FAR;
            insight = switch (settings.combatMode) {
                case CRYSTAL_PVP -> "Tu laisses trop d'air au target";
                case MACE -> "La portée utile n'est pas tenue";
                case JAVA_COOLDOWN -> "Engages trop longs";
                case PVP_1_8 -> "La portée 1.8 n'est pas tenue";
            };
        } else if (deviation > profile.unstableDeviation() * 0.82D) {
            state = SmartFightStates.SpacingState.UNSTABLE;
            insight = "Spacing irrégulier";
        } else {
            state = SmartFightStates.SpacingState.OPTIMAL;
            insight = switch (settings.combatMode) {
                case CRYSTAL_PVP -> "Distance utile pour reset et punition";
                case MACE -> "Fenêtre utile bien tenue";
                case JAVA_COOLDOWN -> "Zone d'impact propre";
                case PVP_1_8 -> "Zone de combo propre";
            };
        }

        return new Result(state, score, (float) deviation, averageDistance, insight);
    }

    public record Result(
        SmartFightStates.SpacingState state,
        float score,
        float variance,
        double averageDistance,
        String insight
    ) {
    }
}
