package net.elysiastudios.client.module.impl;

import net.elysiastudios.client.module.HudModule;
import net.elysiastudios.client.setting.BooleanModuleSetting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;

public class ShieldIndicator extends HudModule {
    private boolean showWithoutShield;
    private boolean showBar;
    private boolean showPercent;

    public ShieldIndicator() {
        super("ShieldIndicator", "Affiche l'état de votre bouclier.", 0, "🛡️");
        this.showWithoutShield = false;
        this.showBar = true;
        this.showPercent = true;
        registerShieldSettings();
    }

    @Override
    public int getEditorWidth() {
        return 96;
    }

    @Override
    public int getEditorHeight() {
        return getHudPadding() * 2 + 18 + (showPercent ? mc.font.lineHeight + 2 : 0);
    }

    @Override
    public void render(GuiGraphics guiGraphics) {
        boolean preview = isPreviewRender();
        ItemStack shieldStack = ItemStack.EMPTY;
        boolean hasShield = preview;
        float cooldown = preview ? 0.25F : 0.0F;

        if (!preview) {
            if (mc.player == null) {
                return;
            }
            if (mc.player.getMainHandItem().is(Items.SHIELD)) {
                shieldStack = mc.player.getMainHandItem();
                hasShield = true;
            } else if (mc.player.getOffhandItem().is(Items.SHIELD)) {
                shieldStack = mc.player.getOffhandItem();
                hasShield = true;
            }
            if (!hasShield && !showWithoutShield) {
                return;
            }
            if (hasShield) {
                cooldown = mc.player.getCooldowns().getCooldownPercent(shieldStack, 0.0F);
            }
        }

        String text = !hasShield ? "Bouclier: Absent" : cooldown > 0.0F ? "Bouclier: Rechargement" : "Bouclier: Prêt";
        int percent = Math.round((1.0F - cooldown) * 100.0F);
        int width = 96;
        int height = getEditorHeight();
        int accent = !hasShield ? 0xFF94A3B8 : cooldown > 0.0F ? 0xFFEF4444 : 0xFF22C55E;

        beginHudRender(guiGraphics);
        drawHudFrame(guiGraphics, width, height, accent);
        drawHudText(guiGraphics, text, getHudPadding(), getHudPadding(), 0xFFFFFFFF);
        if (showBar) {
            int barY = getHudPadding() + mc.font.lineHeight + 2;
            guiGraphics.fill(getHudPadding(), barY, width - getHudPadding(), barY + 4, applyHudOpacity(0x55000000));
            guiGraphics.fill(getHudPadding(), barY, getHudPadding() + Math.round((width - getHudPadding() * 2) * (1.0F - cooldown)), barY + 4, resolveHudAccentColor(accent));
        }
        if (showPercent && hasShield) {
            drawHudText(guiGraphics, percent + "%", getHudPadding(), height - getHudPadding() - mc.font.lineHeight + 1, accent);
        }
        endHudRender(guiGraphics);
    }

    private void registerShieldSettings() {
        addSetting(new BooleanModuleSetting(
            "shield_show_without_item",
            "Afficher sans bouclier",
            "Garde le widget visible même sans bouclier équipé.",
            "Bouclier",
            true,
            () -> showWithoutShield,
            value -> showWithoutShield = value,
            false
        ));
        addSetting(new BooleanModuleSetting(
            "shield_show_bar",
            "Afficher barre",
            "Ajoute une barre de rechargement compacte.",
            "Bouclier",
            true,
            () -> showBar,
            value -> showBar = value,
            true
        ));
        addSetting(new BooleanModuleSetting(
            "shield_show_percent",
            "Afficher pourcentage",
            "Ajoute le pourcentage de récupération du bouclier.",
            "Bouclier",
            true,
            () -> showPercent,
            value -> showPercent = value,
            true
        ));
    }
}
