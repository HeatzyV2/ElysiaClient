package net.elysiastudios.client.module.impl.smartfight;

import static net.elysiastudios.client.module.impl.smartfight.SmartFightStates.FightInterpretation;
import static net.elysiastudios.client.module.impl.smartfight.SmartFightStates.PressureState;
import static net.elysiastudios.client.module.impl.smartfight.SmartFightStates.RhythmState;
import static net.elysiastudios.client.module.impl.smartfight.SmartFightStates.SpacingState;
import static net.elysiastudios.client.module.impl.smartfight.SmartFightStates.TimingState;

public record FightSummary(
    String title,
    String diagnosis,
    FightInterpretation interpretation,
    TimingState timingState,
    SpacingState spacingState,
    RhythmState rhythmState,
    PressureState pressureState,
    float accuracy,
    float stabilityScore,
    double averageDistance,
    long durationMillis,
    long expiresAt
) {
    public static FightSummary preview(long now) {
        return new FightSummary(
            "Résumé SmartFight",
            "Combat propre et stable",
            FightInterpretation.CONTROL_STABLE,
            TimingState.GOOD,
            SpacingState.OPTIMAL,
            RhythmState.STABLE,
            PressureState.APPLYING_PRESSURE,
            0.74F,
            0.79F,
            2.61D,
            17_000L,
            now + 6_000L
        );
    }

    public boolean isVisible(long now) {
        return now <= expiresAt;
    }
}
