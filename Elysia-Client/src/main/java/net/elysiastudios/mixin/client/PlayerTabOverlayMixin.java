package net.elysiastudios.mixin.client;

import net.elysiastudios.client.social.ElysiaBadgeFormatter;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerTabOverlay.class)
public class PlayerTabOverlayMixin {
    @Inject(method = "getNameForDisplay", at = @At("RETURN"), cancellable = true)
    private void elysia$decorateTabName(PlayerInfo playerInfo, CallbackInfoReturnable<Component> cir) {
        if (playerInfo == null || playerInfo.getProfile() == null || playerInfo.getProfile().id() == null) {
            return;
        }
        cir.setReturnValue(ElysiaBadgeFormatter.decorate(playerInfo.getProfile().id(), cir.getReturnValue()));
    }
}
