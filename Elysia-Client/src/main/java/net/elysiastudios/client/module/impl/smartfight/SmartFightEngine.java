package net.elysiastudios.client.module.impl.smartfight;

public class SmartFightEngine {
    private final TimingAnalyzer timingAnalyzer = new TimingAnalyzer();
    private final ReachAnalyzer reachAnalyzer = new ReachAnalyzer();
    private final RhythmAnalyzer rhythmAnalyzer = new RhythmAnalyzer();
    private final PressureAnalyzer pressureAnalyzer = new PressureAnalyzer();
    private final PatternAnalyzer patternAnalyzer = new PatternAnalyzer();

    public FightStateSnapshot analyze(FightSession session, SmartFightSettings settings, long now) {
        TimingAnalyzer.Result timing = settings.timingAssist.enabled
            ? timingAnalyzer.analyze(session, settings)
            : new TimingAnalyzer.Result(SmartFightStates.TimingState.SOLID, 0.66F, 0.66F, 0.85F, 0.18F, 0.70F, 0.18F, 0.70F, "Timing discret");
        ReachAnalyzer.Result reach = settings.reachSense.enabled
            ? reachAnalyzer.analyze(session, settings)
            : new ReachAnalyzer.Result(SmartFightStates.SpacingState.OPTIMAL, 0.67F, 0.20F, 2.60D, "Spacing masqué");
        RhythmAnalyzer.Result rhythm = settings.rhythmTracker.enabled
            ? rhythmAnalyzer.analyze(session, settings, now)
            : new RhythmAnalyzer.Result(SmartFightStates.RhythmState.STABLE, 0.65F, 0.65F, false, "Tempo neutre");
        PressureAnalyzer.Result pressure = settings.pressureAnalyzer.enabled
            ? pressureAnalyzer.analyze(session, settings, now)
            : new PressureAnalyzer.Result(SmartFightStates.PressureState.NEUTRAL, 0.50F, 0.50F, "Pression neutre");
        PatternAnalyzer.Result pattern = settings.opponentPattern.enabled
            ? patternAnalyzer.analyze(session, now)
            : new PatternAnalyzer.Result(SmartFightStates.OpponentPattern.UNKNOWN, 0.22F, "Lecture discrète");

        float accuracy = session.getAccuracy();
        float controlScore = AnalysisMath.clamp(
            (timing.score() * 0.24F)
                + (timing.readyRatio() * 0.10F)
                + (reach.score() * 0.18F)
                + (rhythm.score() * 0.18F)
                + (pressure.score() * 0.20F)
                + (accuracy * 0.10F),
            0.0F,
            1.0F
        );
        float stabilityScore = AnalysisMath.clamp(
            (timing.discipline() * 0.38F)
                + (timing.consistency() * 0.16F)
                + (AnalysisMath.clamp(1.0F - reach.variance(), 0.0F, 1.0F) * 0.18F)
                + (rhythm.regularity() * 0.16F)
                + ((1.0F - timing.wasteRatio()) * 0.07F)
                + ((1.0F - timing.spamRatio()) * 0.05F),
            0.0F,
            1.0F
        );
        float confidence = AnalysisMath.clamp(
            0.22F + ((session.getTotalSwings() + session.getIncomingHits() + session.getDistances().size() + session.getCrystalActions()) / 22.0F),
            0.22F,
            0.94F
        );

        SmartFightStates.FightInterpretation interpretation = chooseInterpretation(session, settings, timing, reach, rhythm, pressure, accuracy, stabilityScore);
        FightStateSnapshot previous = session.getDisplaySnapshot();
        if (previous != null && settings.animations) {
            if (interpretation != previous.interpretation() && now - session.getLastInterpretationChange() < 900L && confidence < 0.76F) {
                interpretation = previous.interpretation();
            } else if (interpretation != previous.interpretation()) {
                session.setLastInterpretationChange(now);
            }

            controlScore = AnalysisMath.lerp(previous.controlScore(), controlScore, 0.18F);
            stabilityScore = AnalysisMath.lerp(previous.stabilityScore(), stabilityScore, 0.16F);
            confidence = AnalysisMath.lerp(previous.confidence(), confidence, 0.24F);
            accuracy = AnalysisMath.lerp(previous.accuracy(), accuracy, 0.22F);
        }

        FightStateSnapshot snapshot = new FightStateSnapshot(
            true,
            interpretation,
            interpretation.getLabel(),
            buildSubline(interpretation, timing, reach, rhythm, pressure, pattern),
            timing.state(),
            reach.state(),
            rhythm.state(),
            pressure.state(),
            pattern.pattern(),
            controlScore,
            stabilityScore,
            confidence,
            accuracy,
            settings.reachSense.showDistanceState ? reach.averageDistance() : 0.0D,
            session.getTotalSwings(),
            session.getSuccessfulHits(),
            session.getMisses(),
            session.getDurationMillis(now),
            now
        );

        session.applySnapshot(snapshot);
        return snapshot;
    }

    public FightSummary buildSummary(FightSession session, FightStateSnapshot snapshot, SmartFightSettings settings, long now) {
        String diagnosis = buildSummaryDiagnosis(snapshot);
        return new FightSummary(
            "Résumé SmartFight",
            diagnosis,
            snapshot.interpretation(),
            snapshot.timingState(),
            snapshot.spacingState(),
            snapshot.rhythmState(),
            snapshot.pressureState(),
            snapshot.accuracy(),
            snapshot.stabilityScore(),
            snapshot.averageDistance(),
            snapshot.durationMillis(),
            now + (settings.fightSummary.durationSeconds * 1_000L)
        );
    }

    private SmartFightStates.FightInterpretation chooseInterpretation(
        FightSession session,
        SmartFightSettings settings,
        TimingAnalyzer.Result timing,
        ReachAnalyzer.Result reach,
        RhythmAnalyzer.Result rhythm,
        PressureAnalyzer.Result pressure,
        float accuracy,
        float stabilityScore
    ) {
        if ((timing.state() == SmartFightStates.TimingState.FORCED || timing.state() == SmartFightStates.TimingState.WASTEFUL)
            && (accuracy < 0.55F || timing.readyRatio() < 0.50F || (settings.combatMode != SmartFightSettings.CombatMode.CRYSTAL_PVP && timing.spamRatio() > 0.45F))) {
            return SmartFightStates.FightInterpretation.OVERCOMMITTING;
        }
        if (reach.state() == SmartFightStates.SpacingState.UNSTABLE
            || reach.state() == SmartFightStates.SpacingState.TOO_CLOSE
            || reach.state() == SmartFightStates.SpacingState.TOO_FAR) {
            return SmartFightStates.FightInterpretation.POOR_SPACING;
        }
        if (pressure.state() == SmartFightStates.PressureState.UNDER_PRESSURE
            && (rhythm.state() == SmartFightStates.RhythmState.DESYNCED || rhythm.state() == SmartFightStates.RhythmState.PASSIVE)) {
            return SmartFightStates.FightInterpretation.PRESSURE_LOST;
        }
        if (rhythm.state() == SmartFightStates.RhythmState.DESYNCED || rhythm.state() == SmartFightStates.RhythmState.UNSTABLE) {
            return SmartFightStates.FightInterpretation.TEMPO_UNSTABLE;
        }
        if (timing.state() == SmartFightStates.TimingState.SLIPPING) {
            return SmartFightStates.FightInterpretation.INCONSISTENT_TIMING;
        }
        if (pressure.state() == SmartFightStates.PressureState.RECOVERING || rhythm.recovering()) {
            return SmartFightStates.FightInterpretation.RECOVERY;
        }
        if (pressure.state() == SmartFightStates.PressureState.APPLYING_PRESSURE && timing.score() > 0.72F && timing.readyRatio() > 0.58F) {
            return SmartFightStates.FightInterpretation.CLEAN_PRESSURE;
        }
        if (accuracy > 0.64F && stabilityScore > 0.72F && timing.readyRatio() > 0.58F) {
            return SmartFightStates.FightInterpretation.CONTROL_STABLE;
        }
        if (accuracy > 0.55F && session.getMisses() <= Math.max(2, session.getSuccessfulHits() / 2) && timing.readyRatio() > 0.48F) {
            return SmartFightStates.FightInterpretation.GOOD_ENGAGE;
        }
        return SmartFightStates.FightInterpretation.STABLE_EXCHANGES;
    }

    private String buildSubline(
        SmartFightStates.FightInterpretation interpretation,
        TimingAnalyzer.Result timing,
        ReachAnalyzer.Result reach,
        RhythmAnalyzer.Result rhythm,
        PressureAnalyzer.Result pressure,
        PatternAnalyzer.Result pattern
    ) {
        return switch (interpretation) {
            case OVERCOMMITTING -> timing.insight();
            case POOR_SPACING -> reach.insight();
            case PRESSURE_LOST, RECOVERY -> pressure.insight();
            case TEMPO_UNSTABLE, LOSING_RHYTHM -> rhythm.insight();
            case CLEAN_PRESSURE, CONTROL_STABLE, GOOD_ENGAGE -> pressure.insight() + " | " + timing.insight();
            default -> pattern.insight();
        };
    }

    private String buildSummaryDiagnosis(FightStateSnapshot snapshot) {
        return switch (snapshot.interpretation()) {
            case CONTROL_STABLE -> "Combat propre et stable";
            case CLEAN_PRESSURE -> "Bonne pression, lecture nette";
            case GOOD_ENGAGE -> "Engages cohérents";
            case POOR_SPACING -> "Spacing à resserrer";
            case OVERCOMMITTING -> "Trop d'engages forcés";
            case PRESSURE_LOST -> "Pression perdue sous échange";
            case RECOVERY -> "Bonne reprise en fin d'échange";
            case TEMPO_UNSTABLE -> "Tempo encore irrégulier";
            case INCONSISTENT_TIMING -> "Timing à lisser";
            default -> "Échanges corrects mais perfectibles";
        };
    }
}
