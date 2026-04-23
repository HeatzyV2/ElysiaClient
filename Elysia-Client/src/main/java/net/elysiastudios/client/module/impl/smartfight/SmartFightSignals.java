package net.elysiastudios.client.module.impl.smartfight;

import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class SmartFightSignals {
    private static final List<AttackRecord> PENDING_ATTACKS = new ArrayList<>();
    private static final List<CrystalBreakRecord> PENDING_CRYSTAL_BREAKS = new ArrayList<>();
    private static int pendingIncomingHits;

    private SmartFightSignals() {
    }

    public static synchronized void recordOutgoingAttack(Player target, float distance, float attackStrength) {
        if (target == null) {
            return;
        }
        PENDING_ATTACKS.add(new AttackRecord(target.getId(), target.getUUID(), distance, attackStrength, System.currentTimeMillis()));
    }

    public static synchronized List<AttackRecord> drainOutgoingAttacks() {
        List<AttackRecord> copy = new ArrayList<>(PENDING_ATTACKS);
        PENDING_ATTACKS.clear();
        return copy;
    }

    public static synchronized void recordCrystalBreak(float distance) {
        PENDING_CRYSTAL_BREAKS.add(new CrystalBreakRecord(distance, System.currentTimeMillis()));
    }

    public static synchronized List<CrystalBreakRecord> drainCrystalBreaks() {
        List<CrystalBreakRecord> copy = new ArrayList<>(PENDING_CRYSTAL_BREAKS);
        PENDING_CRYSTAL_BREAKS.clear();
        return copy;
    }

    public static synchronized void recordIncomingHit() {
        pendingIncomingHits++;
    }

    public static synchronized int drainIncomingHits() {
        int drained = pendingIncomingHits;
        pendingIncomingHits = 0;
        return drained;
    }

    public record AttackRecord(int entityId, UUID uuid, float distance, float attackStrength, long timestamp) {
    }

    public record CrystalBreakRecord(float distance, long timestamp) {
    }
}
