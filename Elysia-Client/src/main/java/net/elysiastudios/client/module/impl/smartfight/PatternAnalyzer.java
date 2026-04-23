package net.elysiastudios.client.module.impl.smartfight;

public class PatternAnalyzer {
    public Result analyze(FightSession session, long now) {
        double averageApproach = AnalysisMath.averageDouble(session.getApproachSamples());
        double averageLateral = AnalysisMath.averageDouble(session.getLateralSamples());
        float frontalRatio = AnalysisMath.booleanRatio(session.getFrontalSamples());
        float jumpRatio = AnalysisMath.booleanRatio(session.getJumpResetSamples());
        int sampleCount = session.getApproachSamples().size() + session.getFrontalSamples().size();
        int incoming = session.getRecentIncomingHits(now, 3_400L);
        int outgoing = session.getRecentOutgoingHits(now, 3_400L);

        SmartFightStates.OpponentPattern pattern;
        String insight;
        if (sampleCount < 4) {
            pattern = SmartFightStates.OpponentPattern.UNKNOWN;
            insight = "Lecture progressive";
        } else if (jumpRatio > 0.26F) {
            pattern = SmartFightStates.OpponentPattern.JUMP_RESET;
            insight = "Sauts de reset visibles";
        } else if (averageApproach > 0.055D && frontalRatio > 0.60F && incoming >= outgoing) {
            pattern = SmartFightStates.OpponentPattern.CONSTANT_PRESSURE;
            insight = "Force les échanges";
        } else if (averageApproach > 0.05D && averageLateral < 0.05D) {
            pattern = SmartFightStates.OpponentPattern.AGGRESSIVE;
            insight = "Engages directs";
        } else if (averageApproach < -0.018D && frontalRatio > 0.42F) {
            pattern = SmartFightStates.OpponentPattern.RESET_HIT;
            insight = "Recule avant hit";
        } else if (averageLateral > 0.11D || (averageLateral > 0.08D && jumpRatio > 0.14F)) {
            pattern = SmartFightStates.OpponentPattern.CHAOTIC;
            insight = "Style chaotique";
        } else if (incoming > outgoing + 1 && session.getMisses() > 2) {
            pattern = SmartFightStates.OpponentPattern.SPAM;
            insight = "Pression brouillonne";
        } else if (frontalRatio > 0.74F) {
            pattern = SmartFightStates.OpponentPattern.FRONTAL;
            insight = "Entrées frontales";
        } else {
            pattern = SmartFightStates.OpponentPattern.STABLE;
            insight = "Style stable";
        }

        float confidence = AnalysisMath.clamp(0.20F + (sampleCount / 14.0F), 0.20F, 0.94F);
        return new Result(pattern, confidence, insight);
    }

    public record Result(
        SmartFightStates.OpponentPattern pattern,
        float confidence,
        String insight
    ) {
    }
}
