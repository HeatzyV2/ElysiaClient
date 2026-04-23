package net.elysiastudios.client.module.impl.smartfight;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.UUID;

public class FightSession {
    private static final int MAX_DISTANCES = 24;
    private static final int MAX_INTERVALS = 18;
    private static final int MAX_SWINGS = 20;
    private static final int MAX_PATTERN_SAMPLES = 18;
    private static final int MAX_CRYSTAL_ACTIONS = 18;

    private final long startTime;
    private long lastExchangeTime;
    private long lastActionTime = -1L;
    private long lastHitTime = -1L;
    private long lastIncomingHitTime = -1L;
    private long lastTargetSampleTime = -1L;

    private UUID primaryTargetId;
    private int primaryTargetEntityId = -1;
    private String primaryTargetName = "Aucune";

    private int totalSwings;
    private int successfulHits;
    private int misses;
    private int incomingHits;
    private int crystalActions;

    private final Deque<Double> distances = new ArrayDeque<>();
    private final Deque<Long> actionIntervals = new ArrayDeque<>();
    private final Deque<Float> swingStrengths = new ArrayDeque<>();
    private final Deque<Boolean> hitHistory = new ArrayDeque<>();
    private final Deque<Long> swingTimes = new ArrayDeque<>();
    private final Deque<Long> crystalActionTimes = new ArrayDeque<>();
    private final Deque<Long> outgoingHitTimes = new ArrayDeque<>();
    private final Deque<Long> incomingHitTimes = new ArrayDeque<>();
    private final Deque<Double> approachSamples = new ArrayDeque<>();
    private final Deque<Double> lateralSamples = new ArrayDeque<>();
    private final Deque<Boolean> frontalSamples = new ArrayDeque<>();
    private final Deque<Boolean> jumpResetSamples = new ArrayDeque<>();

    private Vec3 lastTargetPosition;
    private boolean lastTargetAirborne;

    private SmartFightStates.TimingState timingState = SmartFightStates.TimingState.SOLID;
    private SmartFightStates.SpacingState spacingState = SmartFightStates.SpacingState.OPTIMAL;
    private SmartFightStates.RhythmState rhythmState = SmartFightStates.RhythmState.STABLE;
    private SmartFightStates.PressureState pressureState = SmartFightStates.PressureState.NEUTRAL;
    private SmartFightStates.OpponentPattern opponentPattern = SmartFightStates.OpponentPattern.UNKNOWN;
    private float stabilityScore = 0.70F;
    private FightStateSnapshot displaySnapshot;
    private long lastInterpretationChange;

    public FightSession(long startTime, Player target) {
        this.startTime = startTime;
        this.lastExchangeTime = startTime;
        this.lastInterpretationChange = startTime;
        attachTarget(target);
    }

    public void attachTarget(Player target) {
        if (target == null) {
            return;
        }

        this.primaryTargetId = target.getUUID();
        this.primaryTargetEntityId = target.getId();
        this.primaryTargetName = target.getName().getString();
    }

    public void recordHit(Player target, double distance, float attackStrength, long now) {
        attachTarget(target);
        recordSwing(distance, attackStrength, now, true);
        successfulHits++;
        lastHitTime = now;
        lastExchangeTime = now;
        addLimited(outgoingHitTimes, now, MAX_SWINGS);
        trimTimeDeque(outgoingHitTimes, now, 4_000L);
    }

    public void recordMiss(Player target, double distance, float attackStrength, long now) {
        attachTarget(target);
        recordSwing(distance, attackStrength, now, false);
        misses++;
    }

    public void recordIncomingHit(Player attacker, double distance, long now) {
        attachTarget(attacker);
        incomingHits++;
        lastIncomingHitTime = now;
        lastExchangeTime = now;
        if (distance > 0.0D) {
            addLimited(distances, distance, MAX_DISTANCES);
        }
        addLimited(incomingHitTimes, now, MAX_SWINGS);
        trimTimeDeque(incomingHitTimes, now, 4_000L);
    }

    public void recordCrystalAction(Player target, double distance, long now) {
        attachTarget(target);
        recordActionInterval(now);
        crystalActions++;
        lastExchangeTime = now;
        if (distance > 0.0D) {
            addLimited(distances, distance, MAX_DISTANCES);
        }
        addLimited(crystalActionTimes, now, MAX_CRYSTAL_ACTIONS);
        trimTimeDeque(crystalActionTimes, now, 4_000L);
    }

    public void sampleTarget(Player self, Player target, long now) {
        if (self == null || target == null) {
            return;
        }

        attachTarget(target);
        double distance = self.distanceTo(target);
        addLimited(distances, distance, MAX_DISTANCES);

        Vec3 currentPosition = target.position();
        boolean airborne = !target.onGround() && target.getDeltaMovement().y > 0.04D;
        if (lastTargetPosition != null && lastTargetSampleTime > 0L && now - lastTargetSampleTime <= 500L) {
            Vec3 delta = currentPosition.subtract(lastTargetPosition);
            Vec3 towardPlayer = self.position().subtract(currentPosition);
            if (towardPlayer.lengthSqr() > 1.0E-4D) {
                Vec3 normalized = towardPlayer.normalize();
                double approach = delta.dot(normalized);
                double lateral = Math.abs(delta.x * normalized.z - delta.z * normalized.x);

                addLimited(approachSamples, approach, MAX_PATTERN_SAMPLES);
                addLimited(lateralSamples, lateral, MAX_PATTERN_SAMPLES);
                frontalSamples.addLast(target.getLookAngle().dot(towardPlayer.normalize()) > 0.72D);
                trimBooleanDeque(frontalSamples, MAX_PATTERN_SAMPLES);
            }

            jumpResetSamples.addLast(!lastTargetAirborne && airborne);
            trimBooleanDeque(jumpResetSamples, MAX_PATTERN_SAMPLES);
        }

        lastTargetPosition = currentPosition;
        lastTargetAirborne = airborne;
        lastTargetSampleTime = now;
    }

    public void applySnapshot(FightStateSnapshot snapshot) {
        this.displaySnapshot = snapshot;
        this.timingState = snapshot.timingState();
        this.spacingState = snapshot.spacingState();
        this.rhythmState = snapshot.rhythmState();
        this.pressureState = snapshot.pressureState();
        this.opponentPattern = snapshot.opponentPattern();
        this.stabilityScore = snapshot.stabilityScore();
    }

    public float getAccuracy() {
        return totalSwings <= 0 ? 0.0F : (float) successfulHits / (float) totalSwings;
    }

    public long getDurationMillis(long now) {
        return Math.max(0L, now - startTime);
    }

    public int getRecentOutgoingHits(long now, long windowMs) {
        trimTimeDeque(outgoingHitTimes, now, windowMs);
        return outgoingHitTimes.size();
    }

    public int getRecentIncomingHits(long now, long windowMs) {
        trimTimeDeque(incomingHitTimes, now, windowMs);
        return incomingHitTimes.size();
    }

    public int getRecentSwings(long now, long windowMs) {
        trimTimeDeque(swingTimes, now, windowMs);
        return swingTimes.size();
    }

    public int getRecentCrystalActions(long now, long windowMs) {
        trimTimeDeque(crystalActionTimes, now, windowMs);
        return crystalActionTimes.size();
    }

    public long getStartTime() {
        return startTime;
    }

    public long getLastExchangeTime() {
        return lastExchangeTime;
    }

    public long getLastIncomingHitTime() {
        return lastIncomingHitTime;
    }

    public UUID getPrimaryTargetId() {
        return primaryTargetId;
    }

    public int getPrimaryTargetEntityId() {
        return primaryTargetEntityId;
    }

    public String getPrimaryTargetName() {
        return primaryTargetName;
    }

    public int getTotalSwings() {
        return totalSwings;
    }

    public int getSuccessfulHits() {
        return successfulHits;
    }

    public int getMisses() {
        return misses;
    }

    public int getIncomingHits() {
        return incomingHits;
    }

    public int getCrystalActions() {
        return crystalActions;
    }

    public Deque<Double> getDistances() {
        return distances;
    }

    public Deque<Long> getActionIntervals() {
        return actionIntervals;
    }

    public Deque<Float> getSwingStrengths() {
        return swingStrengths;
    }

    public Deque<Boolean> getHitHistory() {
        return hitHistory;
    }

    public Deque<Double> getApproachSamples() {
        return approachSamples;
    }

    public Deque<Double> getLateralSamples() {
        return lateralSamples;
    }

    public Deque<Boolean> getFrontalSamples() {
        return frontalSamples;
    }

    public Deque<Boolean> getJumpResetSamples() {
        return jumpResetSamples;
    }

    public SmartFightStates.TimingState getTimingState() {
        return timingState;
    }

    public SmartFightStates.SpacingState getSpacingState() {
        return spacingState;
    }

    public SmartFightStates.RhythmState getRhythmState() {
        return rhythmState;
    }

    public SmartFightStates.PressureState getPressureState() {
        return pressureState;
    }

    public SmartFightStates.OpponentPattern getOpponentPattern() {
        return opponentPattern;
    }

    public float getStabilityScore() {
        return stabilityScore;
    }

    public FightStateSnapshot getDisplaySnapshot() {
        return displaySnapshot;
    }

    public long getLastInterpretationChange() {
        return lastInterpretationChange;
    }

    public void setLastInterpretationChange(long lastInterpretationChange) {
        this.lastInterpretationChange = lastInterpretationChange;
    }

    private void recordSwing(double distance, float attackStrength, long now, boolean hit) {
        totalSwings++;
        recordActionInterval(now);
        if (distance > 0.0D) {
            addLimited(distances, distance, MAX_DISTANCES);
        }
        addLimited(swingStrengths, attackStrength, MAX_SWINGS);
        hitHistory.addLast(hit);
        trimBooleanDeque(hitHistory, MAX_SWINGS);
        addLimited(swingTimes, now, MAX_SWINGS);
        trimTimeDeque(swingTimes, now, 4_000L);
    }

    private void recordActionInterval(long now) {
        if (lastActionTime > 0L) {
            addLimited(actionIntervals, now - lastActionTime, MAX_INTERVALS);
        }
        lastActionTime = now;
    }

    private static <T> void addLimited(Deque<T> deque, T value, int maxSize) {
        deque.addLast(value);
        while (deque.size() > maxSize) {
            deque.removeFirst();
        }
    }

    private static void trimBooleanDeque(Deque<Boolean> deque, int maxSize) {
        while (deque.size() > maxSize) {
            deque.removeFirst();
        }
    }

    private static void trimTimeDeque(Deque<Long> deque, long now, long windowMs) {
        while (!deque.isEmpty() && now - deque.peekFirst() > windowMs) {
            deque.removeFirst();
        }
    }
}
