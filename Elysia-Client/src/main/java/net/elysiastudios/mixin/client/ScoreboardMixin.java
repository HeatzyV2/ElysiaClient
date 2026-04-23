package net.elysiastudios.mixin.client;

import net.elysiastudios.client.hud.VanillaHudTransform;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.scores.Objective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to intercept scoreboard sidebar rendering and apply
 * position/scale offsets from the HUD editor config.
 */
@Mixin(Gui.class)
public class ScoreboardMixin {

    @Inject(method = "displayScoreboardSidebar", at = @At("HEAD"))
    private void elysia$preScoreboard(GuiGraphics guiGraphics, Objective objective, CallbackInfo ci) {
        VanillaHudTransform.push(guiGraphics, "scoreboard");
    }

    @Inject(method = "displayScoreboardSidebar", at = @At("RETURN"))
    private void elysia$postScoreboard(GuiGraphics guiGraphics, Objective objective, CallbackInfo ci) {
        VanillaHudTransform.pop(guiGraphics);
    }
}
