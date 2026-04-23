package net.elysiastudios.mixin.client;

import net.elysiastudios.client.module.ModuleManager;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class ClientTickMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    private void elysia$onTick(CallbackInfo ci) {
        // Appeler le tick de tous les modules actifs (AutoSprint, Fullbright, etc.)
        if (Minecraft.getInstance().level != null) {
            ModuleManager.getInstance().onTick();
        }
    }
}
