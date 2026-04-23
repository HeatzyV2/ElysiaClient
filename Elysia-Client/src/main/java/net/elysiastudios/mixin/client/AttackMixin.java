package net.elysiastudios.mixin.client;

import net.elysiastudios.client.module.impl.ComboDisplayModule;
import net.elysiastudios.client.module.impl.ReachDisplayModule;
import net.elysiastudios.client.module.impl.smartfight.SmartFightSignals;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiPlayerGameMode.class)
public class AttackMixin {

    @Inject(method = "attack", at = @At("HEAD"))
    private void elysia$onAttack(Player player, Entity target, CallbackInfo ci) {
        if (player == Minecraft.getInstance().player) {
            // Calculer la distance de frappe (Reach)
            // On utilise la distance entre les yeux du joueur et le point le plus proche de la hitbox de la cible
            double dist = player.getEyePosition().distanceTo(target.getBoundingBox().getCenter());
            // Approximation pour correspondre au ressenti "Reach" (distance bord à bord)
            float reach = (float) Math.max(0, player.distanceTo(target) - 0.5f);
            
            ReachDisplayModule.onHit(reach);
            ComboDisplayModule.onHit();
            if (target instanceof Player targetPlayer) {
                SmartFightSignals.recordOutgoingAttack(targetPlayer, reach, player.getAttackStrengthScale(0.0F));
            } else if (target instanceof EndCrystal) {
                SmartFightSignals.recordCrystalBreak(reach);
            }
        }
    }
}
