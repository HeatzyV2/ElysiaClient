package net.elysiastudios.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.elysiastudios.client.module.ModuleManager;
import net.elysiastudios.client.module.impl.LowFire;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ScreenEffectRenderer.class)
public class ScreenEffectRendererMixin {
    @Unique
    private static boolean elysia$lowFirePosePushed;

    @Inject(method = "renderFire", at = @At("HEAD"))
    private static void elysia$pushLowFire(PoseStack poseStack, MultiBufferSource bufferSource, TextureAtlasSprite sprite, CallbackInfo ci) {
        elysia$lowFirePosePushed = ModuleManager.getInstance().isModuleEnabled(LowFire.class);
        if (elysia$lowFirePosePushed) {
            poseStack.pushPose();
            poseStack.translate(0.0F, -LowFire.getVerticalOffset(), 0.0F);
        }
    }

    @Inject(method = "renderFire", at = @At("RETURN"))
    private static void elysia$popLowFire(PoseStack poseStack, MultiBufferSource bufferSource, TextureAtlasSprite sprite, CallbackInfo ci) {
        if (elysia$lowFirePosePushed) {
            poseStack.popPose();
            elysia$lowFirePosePushed = false;
        }
    }
}
