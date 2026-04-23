package net.elysiastudios.mixin.client;

import net.elysiastudios.client.hud.VanillaHudTransform;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to intercept potion/status effect overlay rendering and apply
 * position/scale offsets from the HUD editor config.
 */
@Mixin(Gui.class)
public class EffectsOverlayMixin {

    @Inject(method = "renderEffects", at = @At("HEAD"))
    private void elysia$preEffects(GuiGraphics guiGraphics, net.minecraft.client.DeltaTracker deltaTracker, CallbackInfo ci) {
        VanillaHudTransform.push(guiGraphics, "effects");
    }

    @Inject(method = "renderEffects", at = @At("RETURN"))
    private void elysia$postEffects(GuiGraphics guiGraphics, net.minecraft.client.DeltaTracker deltaTracker, CallbackInfo ci) {
        VanillaHudTransform.pop(guiGraphics);
    }
}
