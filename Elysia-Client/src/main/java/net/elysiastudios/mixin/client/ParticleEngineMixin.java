package net.elysiastudios.mixin.client;

import net.elysiastudios.client.module.ModuleManager;
import net.elysiastudios.client.module.impl.ParticleLimiter;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.particles.ParticleOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ParticleEngine.class)
public class ParticleEngineMixin {
    @Inject(method = "createParticle", at = @At("HEAD"), cancellable = true)
    private void elysia$limitParticles(ParticleOptions particleOptions, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, CallbackInfoReturnable<Particle> cir) {
        if (ModuleManager.getInstance().isModuleEnabled(ParticleLimiter.class) && ParticleLimiter.shouldDrop(particleOptions)) {
            cir.setReturnValue(null);
        }
    }
}
