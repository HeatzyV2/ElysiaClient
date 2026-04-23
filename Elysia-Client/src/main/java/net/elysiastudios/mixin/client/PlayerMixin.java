package net.elysiastudios.mixin.client;

import net.elysiastudios.client.module.impl.ComboDisplayModule;
import net.elysiastudios.client.module.impl.smartfight.SmartFightSignals;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public class PlayerMixin {

    @Inject(method = "handleEntityEvent", at = @At("HEAD"))
    private void elysia$onEntityStatus(byte status, CallbackInfo ci) {
        // Status 2 = Damage animation
        if (status == 2) {
            ComboDisplayModule.onHurt();
            SmartFightSignals.recordIncomingHit();
        }
    }
}
