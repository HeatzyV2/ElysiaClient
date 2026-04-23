package net.elysiastudios.client.module.impl;

import net.elysiastudios.client.module.Category;
import net.elysiastudios.client.module.Module;
import net.elysiastudios.client.setting.BooleanModuleSetting;
import net.elysiastudios.client.setting.FloatModuleSetting;

import java.util.Locale;

public class Parkour extends Module {
    private boolean onlyWhileForward;
    private boolean onlyWhileSprinting;
    private boolean disableWhenSneaking;
    private float edgeDepth;

    public Parkour() {
        super("Parkour", "Saute automatiquement au bord des blocs.", Category.MOVEMENT, 0, "🏃");
        this.onlyWhileForward = true;
        this.onlyWhileSprinting = false;
        this.disableWhenSneaking = true;
        this.edgeDepth = 0.50F;
        registerParkourSettings();
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.level == null) {
            return;
        }

        if (onlyWhileForward && !mc.options.keyUp.isDown()) {
            return;
        }
        if (onlyWhileSprinting && !mc.player.isSprinting()) {
            return;
        }
        if (disableWhenSneaking && mc.player.isCrouching()) {
            return;
        }

        boolean noBlockAhead = !mc.level.getBlockCollisions(
            mc.player,
            mc.player.getBoundingBox().move(mc.player.getDeltaMovement().x, -edgeDepth, mc.player.getDeltaMovement().z)
        ).iterator().hasNext();

        if (mc.player.onGround() && !mc.options.keyJump.isDown() && noBlockAhead) {
            mc.player.jumpFromGround();
        }
    }

    private void registerParkourSettings() {
        addSetting(new BooleanModuleSetting(
            "parkour_only_forward",
            "Seulement vers l'avant",
            "Déclenche l'assistance uniquement si le joueur avance.",
            "Parkour",
            true,
            () -> onlyWhileForward,
            value -> onlyWhileForward = value,
            true
        ));
        addSetting(new BooleanModuleSetting(
            "parkour_only_sprinting",
            "Seulement en sprint",
            "Réserve le saut auto aux phases de sprint.",
            "Parkour",
            true,
            () -> onlyWhileSprinting,
            value -> onlyWhileSprinting = value,
            false
        ));
        addSetting(new BooleanModuleSetting(
            "parkour_disable_sneak",
            "Respecter le sneak",
            "Désactive le saut auto quand le joueur se met à sneak.",
            "Parkour",
            true,
            () -> disableWhenSneaking,
            value -> disableWhenSneaking = value,
            true
        ));
        addSetting(new FloatModuleSetting(
            "parkour_edge_depth",
            "Détection du vide",
            "Ajuste la profondeur utilisée pour détecter le bord d'un bloc.",
            "Parkour",
            true,
            () -> edgeDepth,
            value -> edgeDepth = value,
            0.50F,
            0.20F,
            0.90F,
            0.05F,
            value -> String.format(Locale.ROOT, "%.2f", value)
        ));
    }
}
