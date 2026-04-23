package net.elysiastudios.mixin.client;

import net.elysiastudios.client.social.ElysiaBadgeFormatter;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AvatarRenderer.class)
public class AvatarRendererMixin {
    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V", at = @At("TAIL"))
    private void elysia$decorateNameTag(Avatar avatar, AvatarRenderState renderState, float partialTick, CallbackInfo ci) {
        if (avatar == null || renderState == null || renderState.nameTag == null) {
            return;
        }
        renderState.nameTag = ElysiaBadgeFormatter.decorate(avatar.getUUID(), renderState.nameTag);
    }
}
