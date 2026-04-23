package net.elysiastudios.client.module.impl;

import net.elysiastudios.client.module.Category;
import net.elysiastudios.client.module.Module;
import net.elysiastudios.client.setting.BooleanModuleSetting;
import org.lwjgl.glfw.GLFW;

public class AutoSprint extends Module {
    private boolean onlyForward;
    private boolean stopInWater;
    private boolean requireFood;
    private boolean requireGround;
    private boolean stopWhileUsingItem;

    public AutoSprint() {
        super("AutoSprint", "Sprints automatiquement.", Category.MOVEMENT, GLFW.GLFW_KEY_V);
        this.onlyForward = true;
        this.stopInWater = true;
        this.requireFood = true;
        this.requireGround = false;
        this.stopWhileUsingItem = true;
        registerMovementSettings();
    }

    @Override
    public void onTick() {
        if (mc.player == null) {
            return;
        }

        boolean movingForward = !onlyForward || mc.options.keyUp.isDown();
        boolean canUseInWater = !stopInWater || !mc.player.isInWater();
        boolean hasFood = !requireFood || mc.player.getFoodData().getFoodLevel() > 6 || mc.player.getAbilities().invulnerable;
        boolean grounded = !requireGround || mc.player.onGround();
        boolean freeHands = !stopWhileUsingItem || !mc.player.isUsingItem();

        if (movingForward && !mc.player.horizontalCollision && canUseInWater && hasFood && grounded && freeHands) {
            mc.player.setSprinting(true);
        }
    }

    private void registerMovementSettings() {
        addSetting(new BooleanModuleSetting(
            "sprint_only_forward",
            "Seulement vers l'avant",
            "Active le sprint auto uniquement en avançant.",
            "Mouvement",
            true,
            () -> onlyForward,
            value -> onlyForward = value,
            true
        ));
        addSetting(new BooleanModuleSetting(
            "sprint_stop_water",
            "Stop dans l'eau",
            "N'essaie pas de forcer le sprint dans l'eau.",
            "Mouvement",
            true,
            () -> stopInWater,
            value -> stopInWater = value,
            true
        ));
        addSetting(new BooleanModuleSetting(
            "sprint_require_food",
            "Respecter la faim",
            "Évite de forcer le sprint quand la faim est trop basse.",
            "Mouvement",
            true,
            () -> requireFood,
            value -> requireFood = value,
            true
        ));
        addSetting(new BooleanModuleSetting(
            "sprint_require_ground",
            "Seulement au sol",
            "Ne force le sprint que lorsque le joueur est au sol.",
            "Mouvement",
            true,
            () -> requireGround,
            value -> requireGround = value,
            false
        ));
        addSetting(new BooleanModuleSetting(
            "sprint_stop_using_item",
            "Stop en utilisant un item",
            "Laisse le contrôle au joueur pendant l'utilisation d'un item.",
            "Mouvement",
            true,
            () -> stopWhileUsingItem,
            value -> stopWhileUsingItem = value,
            true
        ));
    }
}
