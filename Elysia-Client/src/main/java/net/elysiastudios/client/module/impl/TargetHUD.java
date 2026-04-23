package net.elysiastudios.client.module.impl;

import net.elysiastudios.client.module.HudModule;
import net.elysiastudios.client.setting.BooleanModuleSetting;
import net.elysiastudios.client.setting.IntModuleSetting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;

public class TargetHUD extends HudModule {
    private LivingEntity target;
    private float displayHealth;
    private long lastSeenTime;
    private boolean showDistance;
    private boolean showHealthText;
    private int keepTargetMillis;

    public TargetHUD() {
        super("TargetHUD", "Affiche les infos de la cible.", 0, "🎯");
        this.showDistance = true;
        this.showHealthText = true;
        this.keepTargetMillis = 900;
        registerTargetHudSettings();
    }

    @Override
    public int getEditorWidth() {
        return 132;
    }

    @Override
    public int getEditorHeight() {
        return showDistance ? 52 : 40;
    }

    @Override
    public void onTick() {
        long now = System.currentTimeMillis();
        if (mc.hitResult instanceof EntityHitResult entityHitResult && entityHitResult.getEntity() instanceof LivingEntity living) {
            target = living;
            lastSeenTime = now;
        } else if (target != null && (now - lastSeenTime > keepTargetMillis || !target.isAlive() || (mc.player != null && mc.player.distanceTo(target) > 12.0F))) {
            target = null;
        }

        if (target != null) {
            displayHealth += (target.getHealth() - displayHealth) * 0.12F;
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics) {
        String name = "Training Bot";
        float health = 16.5F;
        float maxHealth = 20.0F;
        float distance = 3.1F;

        if (!isPreviewRender()) {
            if (target == null || mc.player == null) {
                return;
            }
            name = target.getName().getString();
            health = displayHealth;
            maxHealth = target.getMaxHealth();
            distance = mc.player.distanceTo(target);
        }

        int width = getEditorWidth();
        int height = getEditorHeight();
        beginHudRender(guiGraphics);
        drawHudFrame(guiGraphics, width, height, 0xFFA855F7);

        drawHudText(guiGraphics, name, getHudPadding(), getHudPadding(), 0xFFFFFFFF);

        int barY = getHudPadding() + mc.font.lineHeight + 4;
        int barX = getHudPadding();
        int barWidth = width - getHudPadding() * 2;
        float healthPercent = Math.max(0.0F, Math.min(1.0F, health / Math.max(1.0F, maxHealth)));
        guiGraphics.fill(barX, barY, barX + barWidth, barY + 6, applyHudOpacity(0x55000000));
        guiGraphics.fill(barX, barY, barX + Math.round(barWidth * healthPercent), barY + 6, resolveHudAccentColor(0xFF22C55E));

        int lineY = barY + 10;
        if (showHealthText) {
            drawHudText(guiGraphics, String.format("%.1f / %.1f", health, maxHealth), getHudPadding(), lineY, 0xFFCBD5E1);
        }
        if (showDistance) {
            String distanceText = String.format("Distance: %.1f", distance);
            int distanceY = showHealthText ? lineY + 10 : lineY;
            drawHudText(guiGraphics, distanceText, getHudPadding(), distanceY, 0xFF94A3B8);
        }

        endHudRender(guiGraphics);
    }

    private void registerTargetHudSettings() {
        addSetting(new BooleanModuleSetting(
            "targethud_show_distance",
            "Afficher distance",
            "Ajoute la distance à la cible dans le widget.",
            "TargetHUD",
            true,
            () -> showDistance,
            value -> showDistance = value,
            true
        ));
        addSetting(new BooleanModuleSetting(
            "targethud_show_health_text",
            "Afficher texte de vie",
            "Ajoute la valeur exacte des points de vie.",
            "TargetHUD",
            true,
            () -> showHealthText,
            value -> showHealthText = value,
            true
        ));
        addSetting(new IntModuleSetting(
            "targethud_keep_target_ms",
            "Maintien cible",
            "Temps pendant lequel la cible reste affichée après la perdre de vue.",
            "TargetHUD",
            true,
            () -> keepTargetMillis,
            value -> keepTargetMillis = value,
            900,
            250,
            3000,
            50,
            value -> value + " ms"
        ));
    }
}
