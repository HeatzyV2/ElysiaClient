package net.elysiastudios.mixin.client;

import net.elysiastudios.client.module.ModuleManager;
import net.elysiastudios.client.module.impl.EntityCullingLite;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherMixin {
    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private <E extends Entity> void elysia$skipFarEntities(E entity, Frustum frustum, double cameraX, double cameraY, double cameraZ, CallbackInfoReturnable<Boolean> cir) {
        if (ModuleManager.getInstance().isModuleEnabled(EntityCullingLite.class) && EntityCullingLite.shouldSkip(entity, cameraX, cameraY, cameraZ)) {
            cir.setReturnValue(false);
        }
    }
}
