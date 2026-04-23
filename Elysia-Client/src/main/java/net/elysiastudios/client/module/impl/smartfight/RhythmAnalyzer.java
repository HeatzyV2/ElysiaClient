package net.elysiastudios.client.module.impl.smartfight;

public class RhythmAnalyzer {
    public Result analyze(FightSession session, SmartFightSettings settings, long now) {
        CombatProfile profile = CombatProfile.forMode(settings.combatMode);
        double averageInterval = AnalysisMath.averageLong(session.getActionIntervals());
        double intervalStdDev = AnalysisMath.stdDevLong(session.getActionIntervals(), averageInterval);
        float regularity = session.getActionIntervals().size() < 2
            ? 0.64F
            : 1.0F - AnalysisMath.clamp((float) (intervalStdDev / profile.intervalStabilityScale()), 0.0F, 0.88F);
        float cadenceFit = session.getActionIntervals().isEmpty()
            ? 0.62F
            : 1.0F - AnalysisMath.clamp((float) (Math.abs(averageInterval - profile.idealIntervalMs()) / profile.intervalToleranceMs()), 0.0F, 1.0F);
        int outgoing = session.getRecentOutgoingHits(now, 2_800L);
        int incoming = session.getRecentIncomingHits(now, 2_800L);
        int crystalActions = settings.combatMode == SmartFightSettings.CombatMode.CRYSTAL_PVP ? session.getRecentCrystalActions(now, 2_800L) : 0;
        float exchangeLoad = AnalysisMath.clamp((outgoing + incoming + (crystalActions * profile.crystalTempoWeight())) / profile.exchangeScale(), 0.0F, 1.0F);

        SmartFightStates.RhythmState state;
        boolean recovering = false;
        String insight;
        if (exchangeLoad > 0.34F && (regularity < 0.30F || cadenceFit < 0.22F)) {
            state = SmartFightStates.RhythmState.DESYNCED;
            insight = "Tempo cassé";
        } else if (outgoing >= incoming + 2 && cadenceFit > 0.46F && regularity > 0.44F) {
            state = SmartFightStates.RhythmState.AGGRESSIVE;
            insight = settings.combatMode == SmartFightSettings.CombatMode.CRYSTAL_PVP
                ? "Cadence offensive bien posée"
                : settings.combatMode == SmartFightSettings.CombatMode.PVP_1_8 ? "CPS dominant" : "Cadence dominante";
        } else if (incoming >= outgoing + 2 && averageInterval > profile.passiveIntervalMs()) {
            state = SmartFightStates.RhythmState.PASSIVE;
            insight = "Rythme subi";
        } else if (regularity < 0.45F || cadenceFit < 0.30F) {
            state = SmartFightStates.RhythmState.UNSTABLE;
            insight = "Cadence irrégulière";
        } else {
            state = SmartFightStates.RhythmState.STABLE;
            insight = "Tempo sous contrôle";
        }

        if (session.getRhythmState() == SmartFightStates.RhythmState.PASSIVE
            || session.getRhythmState() == SmartFightStates.RhythmState.DESYNCED
            || session.getRhythmState() == SmartFightStates.RhythmState.UNSTABLE) {
            recovering = outgoing >= incoming && regularity > 0.50F && cadenceFit > 0.34F;
        }

        float score = AnalysisMath.clamp((regularity * 0.52F) + (cadenceFit * 0.28F) + (exchangeLoad * 0.20F), 0.0F, 1.0F);
        return new Result(state, score, regularity, recovering, insight);
    }

    public record Result(
        SmartFightStates.RhythmState state,
        float score,
        float regularity,
        boolean recovering,
        String insight
    ) {
    }
}
