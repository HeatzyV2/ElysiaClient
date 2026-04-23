package net.elysiastudios.client.module.impl;

import net.elysiastudios.client.module.Category;
import net.elysiastudios.client.module.Module;
import net.elysiastudios.client.setting.BooleanModuleSetting;
import net.minecraft.network.chat.Component;

abstract class InstantActionModule extends Module {
    private boolean showActionFeedback;
    private boolean prefixModuleName;

    protected InstantActionModule(String name, String description, Category category, String icon) {
        super(name, description, category, 0, icon);
        this.showActionFeedback = true;
        this.prefixModuleName = false;
        registerInstantActionSettings();
    }

    @Override
    public final void onEnable() {
        execute();
        setEnabled(false);
    }

    protected abstract void execute();

    protected void toast(String message) {
        if (showActionFeedback && mc.gui != null) {
            String finalMessage = prefixModuleName ? getName() + " • " + message : message;
            mc.gui.setOverlayMessage(Component.literal(finalMessage), false);
        }
    }

    @Override
    protected boolean shouldEmitToggleFeedback() {
        return false;
    }

    private void registerInstantActionSettings() {
        addSetting(new BooleanModuleSetting(
            "action_feedback",
            "Feedback écran",
            "Affiche un retour visuel après l'action.",
            "Action",
            true,
            () -> showActionFeedback,
            value -> showActionFeedback = value,
            true
        ));
        addSetting(new BooleanModuleSetting(
            "action_prefix",
            "Préfixer le module",
            "Ajoute le nom du module au message affiché.",
            "Action",
            true,
            () -> prefixModuleName,
            value -> prefixModuleName = value,
            false
        ));
    }
}
