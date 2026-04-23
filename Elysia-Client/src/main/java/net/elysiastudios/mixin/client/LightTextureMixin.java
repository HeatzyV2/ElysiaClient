package net.elysiastudios.mixin.client;

import net.elysiastudios.client.module.impl.Fullbright;
import net.minecraft.client.renderer.LightTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LightTexture.class)
public class LightTextureMixin {
    @Inject(method = "getBrightness", at = @At("HEAD"), cancellable = true)
    private static void elysia$onGetBrightness(CallbackInfoReturnable<Float> cir) {
        if (Fullbright.shouldForceLightmap()) {
            cir.setReturnValue(15.0F);
        }
    }
}
