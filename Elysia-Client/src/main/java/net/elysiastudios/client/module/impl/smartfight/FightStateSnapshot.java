package net.elysiastudios.client.module.impl.smartfight;

import static net.elysiastudios.client.module.impl.smartfight.SmartFightStates.FightInterpretation;
import static net.elysiastudios.client.module.impl.smartfight.SmartFightStates.OpponentPattern;
import static net.elysiastudios.client.module.impl.smartfight.SmartFightStates.PressureState;
import static net.elysiastudios.client.module.impl.smartfight.SmartFightStates.RhythmState;
import static net.elysiastudios.client.module.impl.smartfight.SmartFightStates.SpacingState;
import static net.elysiastudios.client.module.impl.smartfight.SmartFightStates.TimingState;

public record FightStateSnapshot(
    boolean active,
    FightInterpretation interpretation,
    String headline,
    String subline,
    TimingState timingState,
    SpacingState spacingState,
    RhythmState rhythmState,
    PressureState pressureState,
    OpponentPattern opponentPattern,
    float controlScore,
    float stabilityScore,
    float confidence,
    float accuracy,
    double averageDistance,
    int swings,
    int hits,
    int misses,
    long durationMillis,
    long lastUpdateTime
) {
    public static FightStateSnapshot idle(long now) {
        return new FightStateSnapshot(
            false,
            FightInterpretation.STABLE_EXCHANGES,
            "Veille premium",
            "Analyse prête dès qu'un échange commence",
            TimingState.SOLID,
            SpacingState.OPTIMAL,
            RhythmState.STABLE,
            PressureState.NEUTRAL,
            OpponentPattern.UNKNOWN,
            0.58F,
            0.72F,
            0.42F,
            0.0F,
            2.7D,
            0,
            0,
            0,
            0L,
            now
        );
    }

    public static FightStateSnapshot preview(long now) {
        return new FightStateSnapshot(
            true,
            FightInterpretation.CONTROL_STABLE,
            "Contrôle stable",
            "Pression propre, lecture cohérente",
            TimingState.GOOD,
            SpacingState.OPTIMAL,
            RhythmState.STABLE,
            PressureState.APPLYING_PRESSURE,
            OpponentPattern.AGGRESSIVE,
            0.86F,
            0.78F,
            0.81F,
            0.73F,
            2.63D,
            14,
            10,
            4,
            14_000L,
            now
        );
    }

    public int accentColor() {
        return interpretation.getColor();
    }
}
