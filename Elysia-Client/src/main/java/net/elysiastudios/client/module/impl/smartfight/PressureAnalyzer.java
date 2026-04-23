package net.elysiastudios.client.module.impl.smartfight;

public class PressureAnalyzer {
    public Result analyze(FightSession session, SmartFightSettings settings, long now) {
        int outgoing = session.getRecentOutgoingHits(now, 3_000L);
        int incoming = session.getRecentIncomingHits(now, 3_000L);
        int swings = session.getRecentSwings(now, 3_000L);
        int crystalActions = settings.combatMode == SmartFightSettings.CombatMode.CRYSTAL_PVP ? session.getRecentCrystalActions(now, 3_000L) : 0;

        float crystalTempo = AnalysisMath.clamp(crystalActions / 5.0F, 0.0F, 1.0F);
        float momentum = AnalysisMath.clamp(0.5F + (((outgoing + (crystalActions * 0.35F)) - incoming) / 4.5F), 0.0F, 1.0F);
        float conversion = swings <= 0 ? session.getAccuracy() : AnalysisMath.clamp((float) outgoing / (float) Math.max(1, swings), 0.0F, 1.0F);
        float score = AnalysisMath.clamp((momentum * 0.62F) + (conversion * 0.26F) + (crystalTempo * 0.12F), 0.0F, 1.0F);

        SmartFightStates.PressureState state;
        String insight;
        if (outgoing >= incoming + 2 && conversion > 0.34F) {
            state = SmartFightStates.PressureState.APPLYING_PRESSURE;
            insight = settings.combatMode == SmartFightSettings.CombatMode.CRYSTAL_PVP ? "Pression tenue sans forcer" : "Pression continue";
        } else if (incoming >= outgoing + 2 && conversion < 0.58F) {
            state = SmartFightStates.PressureState.UNDER_PRESSURE;
            insight = "Adversaire imposant";
        } else if (session.getPressureState() == SmartFightStates.PressureState.UNDER_PRESSURE && outgoing >= incoming && (conversion > 0.36F || crystalActions >= 2)) {
            state = SmartFightStates.PressureState.RECOVERING;
            insight = "Reprise du contrôle";
        } else {
            state = SmartFightStates.PressureState.NEUTRAL;
            insight = "Échange équilibré";
        }

        return new Result(state, score, momentum, insight);
    }

    public record Result(
        SmartFightStates.PressureState state,
        float score,
        float momentum,
        String insight
    ) {
    }
}
