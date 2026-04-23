package net.elysiastudios.client.module.impl;

import net.elysiastudios.client.config.ConfigManager;
import net.elysiastudios.client.event.ClickTracker;
import net.elysiastudios.client.module.Category;
import net.elysiastudios.client.module.ConfigurableModule;
import net.elysiastudios.client.module.HudWidget;
import net.elysiastudios.client.module.HudWidgetProvider;
import net.elysiastudios.client.module.Module;
import net.elysiastudios.client.module.PersistentModuleSettings;
import net.elysiastudios.client.module.impl.smartfight.FightSession;
import net.elysiastudios.client.module.impl.smartfight.FightStateSnapshot;
import net.elysiastudios.client.module.impl.smartfight.FightSummary;
import net.elysiastudios.client.module.impl.smartfight.FightSummaryWidget;
import net.elysiastudios.client.module.impl.smartfight.SmartFightConfigScreen;
import net.elysiastudios.client.module.impl.smartfight.SmartFightEngine;
import net.elysiastudios.client.module.impl.smartfight.SmartFightHUDWidget;
import net.elysiastudios.client.module.impl.smartfight.SmartFightSettings;
import net.elysiastudios.client.module.impl.smartfight.SmartFightSignals;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;

import com.google.gson.JsonObject;

import java.util.List;
import java.util.UUID;

public class SmartFightModule extends Module implements HudWidgetProvider, ConfigurableModule, PersistentModuleSettings {
    private static final String CONFIG_ID = "smartfight";
    private static final int DEFAULT_WIDGET_X = 18;
    private static final int DEFAULT_WIDGET_Y = 110;
    private static final int LEGACY_SUMMARY_Y = 204;
    private static final int SUMMARY_GAP = 18;

    private final SmartFightEngine engine = new SmartFightEngine();
    private SmartFightSettings settings;
    private final SmartFightHUDWidget mainWidget;
    private final FightSummaryWidget summaryWidget;

    private FightSession currentSession;
    private FightSummary currentSummary;
    private FightStateSnapshot currentSnapshot;

    public SmartFightModule() {
        super("SmartFight", "Assistant d'analyse PvP premium, discret et contextuel.", Category.COMBAT, 0, "SF");
        this.settings = new SmartFightSettings();
        this.mainWidget = new SmartFightHUDWidget(this);
        this.summaryWidget = new FightSummaryWidget(this);
        this.currentSnapshot = FightStateSnapshot.idle(System.currentTimeMillis());
    }

    @Override
    public void onEnable() {
        currentSnapshot = FightStateSnapshot.idle(System.currentTimeMillis());
    }

    @Override
    public void onDisable() {
        currentSession = null;
        currentSummary = null;
        currentSnapshot = FightStateSnapshot.idle(System.currentTimeMillis());
    }

    @Override
    public void onTick() {
        long now = System.currentTimeMillis();
        if (mc.player == null || mc.level == null) {
            currentSession = null;
            currentSummary = null;
            currentSnapshot = FightStateSnapshot.idle(now);
            return;
        }

        int matchedAttacks = processOutgoingAttacks(now);
        int matchedCrystalBreaks = processCrystalBreaks(now);
        processPendingMisses(now, matchedAttacks + matchedCrystalBreaks);
        processIncomingHits(now);
        detectPassiveCombatStart(now);

        Player tracked = resolveObservedTarget();
        if (currentSession != null) {
            if (tracked != null) {
                currentSession.sampleTarget(mc.player, tracked, now);
            }

            if (now - currentSession.getLastExchangeTime() > settings.combatTimeoutMs) {
                finishSession(now);
                return;
            }

            currentSnapshot = engine.analyze(currentSession, settings, now);
        } else {
            currentSnapshot = FightStateSnapshot.idle(now);
        }

        if (currentSummary != null && !currentSummary.isVisible(now)) {
            currentSummary = null;
        }
    }

    @Override
    public List<HudWidget> getHudWidgets() {
        return List.of(mainWidget, summaryWidget);
    }

    @Override
    public Screen createConfigScreen(Screen parent) {
        return new SmartFightConfigScreen(parent, this);
    }

    public SmartFightSettings getSmartFightSettings() {
        return settings;
    }

    public int getMainWidgetDefaultX() {
        return DEFAULT_WIDGET_X;
    }

    public int getMainWidgetDefaultY() {
        return DEFAULT_WIDGET_Y;
    }

    public int getRecommendedSummaryY() {
        ConfigManager.HudConfig mainConfig = mainWidget.getConfig();
        int mainHeight = Math.max(72, Math.round(mainWidget.getEditorHeight() * mainConfig.scale));
        return mainConfig.y + mainHeight + SUMMARY_GAP;
    }

    public FightStateSnapshot getSnapshotForRender(boolean preview) {
        if (preview) {
            return FightStateSnapshot.preview(System.currentTimeMillis());
        }
        return currentSnapshot != null ? currentSnapshot : FightStateSnapshot.idle(System.currentTimeMillis());
    }

    public FightSummary getSummaryForRender(boolean preview) {
        if (!settings.fightSummary.enabled) {
            return null;
        }
        if (preview) {
            return FightSummary.preview(System.currentTimeMillis());
        }
        return currentSummary;
    }

    public boolean isFightActive() {
        return currentSession != null && currentSnapshot != null && currentSnapshot.active();
    }

    public boolean shouldRenderMainWidget() {
        return !settings.showOnlyInCombat || isFightActive();
    }

    public boolean shouldRenderSummaryWidget() {
        return settings.fightSummary.enabled && currentSummary != null && currentSummary.isVisible(System.currentTimeMillis());
    }

    public void saveSettings() {
        settings.sanitize();
        ConfigManager.getInstance().setModuleData(CONFIG_ID, settings.toJson());
        ConfigManager.getInstance().save();
    }

    @Override
    public String getConfigId() {
        return CONFIG_ID;
    }

    @Override
    public void loadModuleSettings(JsonObject json) {
        this.settings = SmartFightSettings.fromJson(json);
        migrateLegacyHudLayout();
    }

    @Override
    public JsonObject saveModuleSettings() {
        settings.sanitize();
        return settings.toJson();
    }

    private void migrateLegacyHudLayout() {
        ConfigManager.HudConfig mainConfig = mainWidget.getConfig();
        ConfigManager.HudConfig summaryConfig = summaryWidget.getConfig();

        boolean mainAtDefault = mainConfig.x == DEFAULT_WIDGET_X && mainConfig.y == DEFAULT_WIDGET_Y;
        boolean summaryAtLegacyDefault = summaryConfig.x == DEFAULT_WIDGET_X && summaryConfig.y == LEGACY_SUMMARY_Y;
        if (mainAtDefault && summaryAtLegacyDefault) {
            summaryConfig.y = getRecommendedSummaryY();
        }
    }

    private int processOutgoingAttacks(long now) {
        int matched = 0;
        for (SmartFightSignals.AttackRecord record : SmartFightSignals.drainOutgoingAttacks()) {
            Player target = resolvePlayer(record.entityId(), record.uuid());
            if (target == null) {
                continue;
            }

            FightSession session = ensureSession(now, target);
            session.recordHit(target, record.distance(), record.attackStrength(), record.timestamp());
            session.sampleTarget(mc.player, target, now);
            matched++;
        }
        return matched;
    }

    private int processCrystalBreaks(long now) {
        int matched = 0;
        for (SmartFightSignals.CrystalBreakRecord record : SmartFightSignals.drainCrystalBreaks()) {
            matched++;
            if (settings.combatMode != SmartFightSettings.CombatMode.CRYSTAL_PVP) {
                continue;
            }

            Player target = resolveObservedTarget();
            if (target == null) {
                target = resolveTrackedTarget();
            }
            if (target == null) {
                continue;
            }

            FightSession session = ensureSession(now, target);
            session.recordCrystalAction(target, record.distance(), record.timestamp());
            session.sampleTarget(mc.player, target, now);
        }
        return matched;
    }

    private void processPendingMisses(long now, int matchedAttacks) {
        int pendingClicks = ClickTracker.consumePendingLeftClicks();
        int remainingMisses = Math.max(0, pendingClicks - matchedAttacks);
        if (remainingMisses <= 0) {
            return;
        }

        Player target = resolveObservedTarget();
        if (target == null || mc.player.distanceTo(target) > 4.35F) {
            return;
        }

        long recentAttackWindow = now - ClickTracker.getLastLeftClickTime();
        if (recentAttackWindow > 550L) {
            return;
        }

        FightSession session = ensureSession(now, target);
        double distance = mc.player.distanceTo(target);
        for (int i = 0; i < remainingMisses; i++) {
            session.recordMiss(target, distance, mc.player.getAttackStrengthScale(0.0F), now);
        }
        session.sampleTarget(mc.player, target, now);
    }

    private void processIncomingHits(long now) {
        int incomingHits = SmartFightSignals.drainIncomingHits();
        if (incomingHits <= 0) {
            return;
        }

        Player attacker = resolveThreatTarget();
        FightSession session = ensureSession(now, attacker);
        double distance = attacker != null ? mc.player.distanceTo(attacker) : 0.0D;
        for (int i = 0; i < incomingHits; i++) {
            session.recordIncomingHit(attacker, distance, now);
        }
    }

    private void detectPassiveCombatStart(long now) {
        if (currentSession != null) {
            return;
        }

        Player target = resolveObservedTarget();
        if (target == null || mc.player.distanceTo(target) > 4.0F) {
            return;
        }

        boolean recentInteraction = now - ClickTracker.getLastLeftClickTime() < 450L || mc.player.hurtTime > 0 || target.hurtTime > 0;
        if (recentInteraction) {
            ensureSession(now, target);
        }
    }

    private FightSession ensureSession(long now, Player target) {
        if (currentSession == null) {
            currentSession = new FightSession(now, target);
        } else if (target != null) {
            currentSession.attachTarget(target);
        }
        return currentSession;
    }

    private void finishSession(long now) {
        if (currentSession != null && settings.fightSummary.enabled && currentSession.getDurationMillis(now) >= 1200L) {
            FightStateSnapshot summarySnapshot = currentSnapshot != null && currentSnapshot.active()
                ? currentSnapshot
                : engine.analyze(currentSession, settings, now);
            currentSummary = engine.buildSummary(currentSession, summarySnapshot, settings, now);
        }
        currentSession = null;
        currentSnapshot = FightStateSnapshot.idle(now);
    }

    private Player resolveObservedTarget() {
        Player tracked = resolveTrackedTarget();
        if (tracked != null) {
            return tracked;
        }

        if (mc.hitResult instanceof EntityHitResult entityHitResult && entityHitResult.getEntity() instanceof Player target && isValidCombatPlayer(target)) {
            return target;
        }

        Player nearest = null;
        double nearestDistance = 5.6D;
        for (Player player : mc.level.players()) {
            if (!isValidCombatPlayer(player)) {
                continue;
            }

            double distance = mc.player.distanceTo(player);
            if (distance < nearestDistance) {
                nearest = player;
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    private Player resolveTrackedTarget() {
        if (currentSession == null) {
            return null;
        }

        Player byId = resolvePlayer(currentSession.getPrimaryTargetEntityId(), currentSession.getPrimaryTargetId());
        if (isValidCombatPlayer(byId) && mc.player.distanceTo(byId) <= 6.2F) {
            return byId;
        }
        return null;
    }

    private Player resolveThreatTarget() {
        if (mc.player.getLastHurtByMob() instanceof Player attacker && isValidCombatPlayer(attacker)) {
            return attacker;
        }

        Player observed = resolveObservedTarget();
        if (observed != null) {
            return observed;
        }

        Player nearest = null;
        double nearestDistance = 5.2D;
        for (Player player : mc.level.players()) {
            if (!isValidCombatPlayer(player)) {
                continue;
            }

            double distance = mc.player.distanceTo(player);
            if (distance < nearestDistance) {
                nearest = player;
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    private Player resolvePlayer(int entityId, UUID uuid) {
        if (mc.level == null) {
            return null;
        }

        if (entityId >= 0) {
            Entity entity = mc.level.getEntity(entityId);
            if (entity instanceof Player player && isValidCombatPlayer(player)) {
                return player;
            }
        }

        if (uuid != null) {
            for (Player player : mc.level.players()) {
                if (uuid.equals(player.getUUID()) && isValidCombatPlayer(player)) {
                    return player;
                }
            }
        }
        return null;
    }

    private boolean isValidCombatPlayer(Player player) {
        return player != null && player.isAlive() && player != mc.player && !player.isSpectator();
    }
}
