package net.elysiastudios.mixin.client;

import net.elysiastudios.client.hud.VanillaHudTransform;
import net.minecraft.client.gui.components.BossHealthOverlay;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to intercept boss health bar rendering and apply
 * position/scale offsets from the HUD editor config.
 */
@Mixin(BossHealthOverlay.class)
public class BossBarMixin {

    @Inject(method = "render", at = @At("HEAD"))
    private void elysia$preBossBar(GuiGraphics guiGraphics, CallbackInfo ci) {
        VanillaHudTransform.push(guiGraphics, "bossbar");
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void elysia$postBossBar(GuiGraphics guiGraphics, CallbackInfo ci) {
        VanillaHudTransform.pop(guiGraphics);
    }
}
