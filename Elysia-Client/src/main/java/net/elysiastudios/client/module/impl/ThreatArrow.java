package net.elysiastudios.client.module.impl;

import net.elysiastudios.client.module.Category;
import net.elysiastudios.client.module.HudWidget;
import net.elysiastudios.client.module.HudWidgetProvider;
import net.elysiastudios.client.module.Module;
import net.elysiastudios.client.setting.BooleanModuleSetting;
import net.elysiastudios.client.setting.IntModuleSetting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class ThreatArrow extends Module implements HudWidgetProvider {
    private static final int ACCENT_COLOR = 0xFF41D9FF;

    private final ThreatArrowWidget widget;

    private LivingEntity trackedThreat;
    private UUID trackedThreatUuid;
    private int trackedThreatEntityId;
    private long lastThreatTime;
    private int lastPlayerHurtTime;
    private float displayedYawOffset;
    private float displayedVerticalOffset;
    private float visibility;

    private int radius;
    private int pointerSize;
    private int keepTargetMillis;
    private int maxDistance;
    private boolean showLabel;
    private boolean showDistance;
    private boolean trackPlayers;
    private boolean trackMobs;

    public ThreatArrow() {
        super("ThreatArrow", "Pointe vers l'entite qui vient de vous attaquer.", Category.RENDER, 0, "THR");
        this.widget = new ThreatArrowWidget();
        this.trackedThreatEntityId = -1;
        this.radius = 28;
        this.pointerSize = 13;
        this.keepTargetMillis = 1600;
        this.maxDistance = 28;
        this.showLabel = true;
        this.showDistance = true;
        this.trackPlayers = true;
        this.trackMobs = true;
        registerThreatSettings();
    }

    @Override
    public void onDisable() {
        trackedThreat = null;
        trackedThreatUuid = null;
        trackedThreatEntityId = -1;
        lastThreatTime = 0L;
        lastPlayerHurtTime = 0;
        displayedYawOffset = 0.0F;
        displayedVerticalOffset = 0.0F;
        visibility = 0.0F;
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.level == null) {
            clearThreatState();
            return;
        }

        long now = System.currentTimeMillis();
        LivingEntity resolvedTracked = resolveTrackedThreat();
        int hurtTime = mc.player.hurtTime;
        boolean receivedNewHit = hurtTime > 0 && hurtTime > lastPlayerHurtTime;
        lastPlayerHurtTime = hurtTime;

        if (receivedNewHit) {
            LivingEntity aggressor = resolveRecentAggressor();
            if (aggressor != null) {
                trackThreat(aggressor, now);
                resolvedTracked = aggressor;
            }
        } else if (resolvedTracked == null && now - lastThreatTime <= keepTargetMillis) {
            LivingEntity fallback = resolveFallbackThreat();
            if (fallback != null) {
                trackThreat(fallback, now);
                resolvedTracked = fallback;
            }
        }

        boolean activeThreat = resolvedTracked != null && now - lastThreatTime <= keepTargetMillis;
        float targetVisibility = activeThreat ? 1.0F : 0.0F;
        visibility += (targetVisibility - visibility) * 0.22F;

        if (activeThreat) {
            float yawOffset = computeYawOffset(resolvedTracked);
            float verticalOffset = computeVerticalOffset(resolvedTracked);
            displayedYawOffset = lerpAngle(displayedYawOffset, yawOffset, 0.24F);
            displayedVerticalOffset += (verticalOffset - displayedVerticalOffset) * 0.18F;
        } else {
            displayedVerticalOffset += (0.0F - displayedVerticalOffset) * 0.20F;
        }
    }

    @Override
    public List<HudWidget> getHudWidgets() {
        return List.of(widget);
    }

    private void clearThreatState() {
        trackedThreat = null;
        trackedThreatUuid = null;
        trackedThreatEntityId = -1;
        lastThreatTime = 0L;
        lastPlayerHurtTime = 0;
        displayedVerticalOffset = 0.0F;
        visibility = 0.0F;
    }

    private void trackThreat(LivingEntity threat, long now) {
        trackedThreat = threat;
        trackedThreatUuid = threat.getUUID();
        trackedThreatEntityId = threat.getId();
        lastThreatTime = now;
    }

    private LivingEntity resolveTrackedThreat() {
        if (isValidThreat(trackedThreat)) {
            return trackedThreat;
        }

        if (mc.level == null) {
            trackedThreat = null;
            trackedThreatUuid = null;
            trackedThreatEntityId = -1;
            return null;
        }

        if (trackedThreatEntityId >= 0) {
            Entity entity = mc.level.getEntity(trackedThreatEntityId);
            if (entity instanceof LivingEntity living && isValidThreat(living)) {
                trackedThreat = living;
                return living;
            }
        }

        if (trackedThreatUuid != null) {
            for (Entity entity : mc.level.entitiesForRendering()) {
                if (entity instanceof LivingEntity living && trackedThreatUuid.equals(living.getUUID()) && isValidThreat(living)) {
                    trackedThreat = living;
                    trackedThreatEntityId = living.getId();
                    return living;
                }
            }
        }

        trackedThreat = null;
        trackedThreatUuid = null;
        trackedThreatEntityId = -1;
        return null;
    }

    private LivingEntity resolveRecentAggressor() {
        LivingEntity fromDamageSource = resolveDamageSourceThreat();
        if (fromDamageSource != null) {
            return fromDamageSource;
        }

        if (isValidThreat(mc.player.getLastHurtByMob())) {
            return mc.player.getLastHurtByMob();
        }

        return resolveFallbackThreat();
    }

    private LivingEntity resolveDamageSourceThreat() {
        DamageSource damageSource = mc.player.getLastDamageSource();
        if (damageSource == null) {
            return null;
        }

        if (damageSource.getEntity() instanceof LivingEntity causingEntity && isValidThreat(causingEntity)) {
            return causingEntity;
        }

        if (damageSource.getDirectEntity() instanceof LivingEntity directEntity && isValidThreat(directEntity)) {
            return directEntity;
        }

        return null;
    }

    private LivingEntity resolveFallbackThreat() {
        if (mc.player == null || mc.level == null) {
            return null;
        }

        LivingEntity bestThreat = null;
        double bestScore = -1.0D;
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity living) || !isValidThreat(living)) {
                continue;
            }

            double distance = mc.player.distanceTo(living);
            if (distance > maxDistance) {
                continue;
            }

            double score = Math.max(0.0D, (maxDistance + 2.0D) - distance);
            if (living == mc.player.getLastHurtByMob()) {
                score += 70.0D;
            }
            if (entity instanceof Mob mob && mob.getTarget() == mc.player) {
                score += 45.0D;
            }
            if (living.hurtTime > 0) {
                score += 8.0D;
            }
            score += computeAimPressureScore(living);

            if (score > bestScore) {
                bestScore = score;
                bestThreat = living;
            }
        }

        return bestScore >= 16.0D ? bestThreat : null;
    }

    private double computeAimPressureScore(LivingEntity entity) {
        if (mc.player == null) {
            return 0.0D;
        }

        double deltaX = mc.player.getX() - entity.getX();
        double deltaY = mc.player.getEyeY() - entity.getEyeY();
        double deltaZ = mc.player.getZ() - entity.getZ();
        double length = Math.sqrt((deltaX * deltaX) + (deltaY * deltaY) + (deltaZ * deltaZ));
        if (length < 1.0E-4D) {
            return 0.0D;
        }

        double dirX = deltaX / length;
        double dirY = deltaY / length;
        double dirZ = deltaZ / length;
        var look = entity.getViewVector(1.0F);
        double dot = Mth.clamp((float) ((look.x * dirX) + (look.y * dirY) + (look.z * dirZ)), -1.0F, 1.0F);
        return Math.max(0.0D, dot) * 14.0D;
    }

    private boolean isValidThreat(LivingEntity entity) {
        if (entity == null || mc.player == null) {
            return false;
        }
        if (entity == mc.player || !entity.isAlive() || entity.isRemoved() || entity.isSpectator()) {
            return false;
        }
        if (!trackPlayers && entity instanceof Player) {
            return false;
        }
        if (!trackMobs && !(entity instanceof Player)) {
            return false;
        }
        return mc.player.distanceTo(entity) <= Math.max(6.0F, maxDistance + 2.0F);
    }

    private float computeYawOffset(LivingEntity entity) {
        double deltaX = entity.getX() - mc.player.getX();
        double deltaZ = entity.getZ() - mc.player.getZ();
        float targetYaw = (float) (Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90.0D);
        return Mth.wrapDegrees(targetYaw - mc.player.getYRot());
    }

    private float computeVerticalOffset(LivingEntity entity) {
        double deltaX = entity.getX() - mc.player.getX();
        double deltaY = (entity.getY() + (entity.getBbHeight() * 0.65D)) - mc.player.getEyeY();
        double deltaZ = entity.getZ() - mc.player.getZ();
        double horizontal = Math.sqrt((deltaX * deltaX) + (deltaZ * deltaZ));
        if (horizontal < 1.0E-4D) {
            return 0.0F;
        }

        float pitchToTarget = (float) -Math.toDegrees(Math.atan2(deltaY, horizontal));
        return Mth.clamp(Mth.wrapDegrees(pitchToTarget - mc.player.getXRot()) / 65.0F, -1.0F, 1.0F);
    }

    private float lerpAngle(float current, float target, float factor) {
        return current + (Mth.wrapDegrees(target - current) * factor);
    }

    private int getWidgetDiameter() {
        return (radius * 2) + (pointerSize * 4);
    }

    private String trimToWidth(String text, int maxWidth) {
        if (text == null || maxWidth <= 0) {
            return "";
        }
        if (mc.font.width(text) <= maxWidth) {
            return text;
        }

        String ellipsis = "...";
        int availableWidth = Math.max(0, maxWidth - mc.font.width(ellipsis));
        String trimmed = text;
        while (!trimmed.isEmpty() && mc.font.width(trimmed) > availableWidth) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed.isEmpty() ? ellipsis : trimmed + ellipsis;
    }

    private int getWidgetHeight() {
        return getWidgetDiameter() + (showLabel ? mc.font.lineHeight + 8 : 0);
    }

    private void registerThreatSettings() {
        addSetting(new IntModuleSetting(
            "threat_radius",
            "Rayon",
            "Distance entre le centre du widget et la fleche.",
            "Affichage",
            true,
            () -> radius,
            value -> radius = value,
            28,
            16,
            72,
            2,
            value -> value + " px"
        ));
        addSetting(new IntModuleSetting(
            "threat_pointer_size",
            "Taille",
            "Taille generale de la fleche.",
            "Affichage",
            true,
            () -> pointerSize,
            value -> pointerSize = value,
            13,
            8,
            24,
            1,
            value -> value + " px"
        ));
        addSetting(new IntModuleSetting(
            "threat_keep_ms",
            "Maintien",
            "Temps pendant lequel la menace reste affichee apres le hit.",
            "Logique",
            true,
            () -> keepTargetMillis,
            value -> keepTargetMillis = value,
            1600,
            350,
            4000,
            50,
            value -> value + " ms"
        ));
        addSetting(new IntModuleSetting(
            "threat_max_distance",
            "Distance max",
            "Ignore les menaces trop eloignees.",
            "Logique",
            true,
            () -> maxDistance,
            value -> maxDistance = value,
            28,
            6,
            96,
            2,
            value -> value + " m"
        ));
        addSetting(new BooleanModuleSetting(
            "threat_show_label",
            "Afficher nom",
            "Affiche le nom de la menace sous la fleche.",
            "Affichage",
            true,
            () -> showLabel,
            value -> showLabel = value,
            true
        ));
        addSetting(new BooleanModuleSetting(
            "threat_show_distance",
            "Afficher distance",
            "Ajoute la distance quand le nom est visible.",
            "Affichage",
            true,
            () -> showDistance,
            value -> showDistance = value,
            true
        ));
        addSetting(new BooleanModuleSetting(
            "threat_track_players",
            "Suivre joueurs",
            "Autorise les joueurs comme menaces suivies.",
            "Filtres",
            true,
            () -> trackPlayers,
            value -> trackPlayers = value,
            true
        ));
        addSetting(new BooleanModuleSetting(
            "threat_track_mobs",
            "Suivre mobs",
            "Autorise les mobs et monstres comme menaces suivies.",
            "Filtres",
            true,
            () -> trackMobs,
            value -> trackMobs = value,
            true
        ));
    }

    private int applyAlpha(int color, float alphaMultiplier) {
        int alpha = (color >>> 24) & 0xFF;
        int resolvedAlpha = Math.max(0, Math.min(255, Math.round(alpha * alphaMultiplier)));
        return (resolvedAlpha << 24) | (color & 0x00FFFFFF);
    }

    private void fillTriangle(GuiGraphics guiGraphics, float x1, float y1, float x2, float y2, float x3, float y3, int color) {
        int minX = Mth.floor(Math.min(x1, Math.min(x2, x3)));
        int maxX = Mth.ceil(Math.max(x1, Math.max(x2, x3)));
        int minY = Mth.floor(Math.min(y1, Math.min(y2, y3)));
        int maxY = Mth.ceil(Math.max(y1, Math.max(y2, y3)));

        float area = edge(x1, y1, x2, y2, x3, y3);
        if (Math.abs(area) < 1.0E-4F) {
            return;
        }

        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                float px = x + 0.5F;
                float py = y + 0.5F;
                float w0 = edge(x2, y2, x3, y3, px, py);
                float w1 = edge(x3, y3, x1, y1, px, py);
                float w2 = edge(x1, y1, x2, y2, px, py);
                if ((w0 >= 0.0F && w1 >= 0.0F && w2 >= 0.0F) || (w0 <= 0.0F && w1 <= 0.0F && w2 <= 0.0F)) {
                    guiGraphics.fill(x, y, x + 1, y + 1, color);
                }
            }
        }
    }

    private float edge(float ax, float ay, float bx, float by, float px, float py) {
        return ((px - ax) * (by - ay)) - ((py - ay) * (bx - ax));
    }

    private final class ThreatArrowWidget implements HudWidget {
        @Override
        public String getHudId() {
            return "threat-arrow";
        }

        @Override
        public String getDisplayName() {
            return "Threat Arrow";
        }

        @Override
        public int getEditorWidth() {
            return Math.max(getWidgetDiameter(), showLabel ? 124 : 0);
        }

        @Override
        public int getEditorHeight() {
            return getWidgetHeight();
        }

        @Override
        public int getDefaultX() {
            int width = mc.getWindow().getGuiScaledWidth();
            return Math.max(8, (width / 2) - (getEditorWidth() / 2));
        }

        @Override
        public int getDefaultY() {
            int height = mc.getWindow().getGuiScaledHeight();
            return Math.max(8, (height / 2) - (getEditorHeight() / 2));
        }

        @Override
        public void render(GuiGraphics guiGraphics, boolean preview) {
            LivingEntity threat = preview ? null : resolveTrackedThreat();
            if (!preview && (threat == null || visibility < 0.04F || System.currentTimeMillis() - lastThreatTime > keepTargetMillis)) {
                return;
            }

            float alpha = preview ? 1.0F : visibility;
            float yawOffset = preview ? 38.0F : displayedYawOffset;
            float verticalBias = preview ? -0.18F : displayedVerticalOffset;
            int width = getEditorWidth();
            int height = getEditorHeight();
            int circleSize = getWidgetDiameter();

            guiGraphics.pose().pushMatrix();
            guiGraphics.pose().translate((float) getX(), (float) getY());
            guiGraphics.pose().scale(getScale(), getScale());

            float centerX = width / 2.0F;
            float centerY = circleSize / 2.0F;
            float angle = (float) Math.toRadians(yawOffset - 90.0F);
            float directionX = (float) Math.cos(angle);
            float directionY = (float) Math.sin(angle);
            float arrowX = centerX + (directionX * radius);
            float arrowY = centerY + (directionY * radius) + (verticalBias * radius * 0.38F);

            float size = pointerSize;
            float tipX = arrowX + (directionX * size);
            float tipY = arrowY + (directionY * size);
            float baseX = arrowX - (directionX * size * 0.62F);
            float baseY = arrowY - (directionY * size * 0.62F);
            float perpX = -directionY;
            float perpY = directionX;

            float leftX = baseX + (perpX * size * 0.58F);
            float leftY = baseY + (perpY * size * 0.58F);
            float rightX = baseX - (perpX * size * 0.58F);
            float rightY = baseY - (perpY * size * 0.58F);

            float glowSize = size * 1.45F;
            float glowTipX = arrowX + (directionX * glowSize);
            float glowTipY = arrowY + (directionY * glowSize);
            float glowBaseX = arrowX - (directionX * glowSize * 0.58F);
            float glowBaseY = arrowY - (directionY * glowSize * 0.58F);
            float glowLeftX = glowBaseX + (perpX * glowSize * 0.66F);
            float glowLeftY = glowBaseY + (perpY * glowSize * 0.66F);
            float glowRightX = glowBaseX - (perpX * glowSize * 0.66F);
            float glowRightY = glowBaseY - (perpY * glowSize * 0.66F);

            fillTriangle(guiGraphics, glowTipX, glowTipY, glowLeftX, glowLeftY, glowRightX, glowRightY, applyAlpha(ACCENT_COLOR, alpha * 0.20F));
            fillTriangle(guiGraphics, tipX, tipY, leftX, leftY, rightX, rightY, applyAlpha(ACCENT_COLOR, alpha));

            float innerSize = size * 0.52F;
            float innerTipX = arrowX + (directionX * innerSize);
            float innerTipY = arrowY + (directionY * innerSize);
            float innerBaseX = arrowX - (directionX * innerSize * 0.52F);
            float innerBaseY = arrowY - (directionY * innerSize * 0.52F);
            float innerLeftX = innerBaseX + (perpX * innerSize * 0.48F);
            float innerLeftY = innerBaseY + (perpY * innerSize * 0.48F);
            float innerRightX = innerBaseX - (perpX * innerSize * 0.48F);
            float innerRightY = innerBaseY - (perpY * innerSize * 0.48F);
            fillTriangle(guiGraphics, innerTipX, innerTipY, innerLeftX, innerLeftY, innerRightX, innerRightY, applyAlpha(0xFFFFFFFF, alpha * 0.78F));

            if (showLabel) {
                String label;
                if (preview) {
                    label = showDistance ? "Skeleton 6.4m" : "Skeleton";
                } else {
                    double distance = mc.player.distanceTo(threat);
                    String threatName = threat.getName().getString();
                    label = showDistance
                        ? threatName + " " + String.format(Locale.ROOT, "%.1fm", distance)
                        : threatName;
                }
                label = trimToWidth(label, width - 6);
                int textY = circleSize + 4;
                int textX = Math.max(0, (width - mc.font.width(label)) / 2);
                guiGraphics.drawString(mc.font, label, textX, textY, applyAlpha(0xFFE6F8FF, alpha), true);
            }

            guiGraphics.pose().popMatrix();
        }
    }
}
