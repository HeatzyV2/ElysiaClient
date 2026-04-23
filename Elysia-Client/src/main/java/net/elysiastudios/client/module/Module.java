package net.elysiastudios.client.module;

import com.google.gson.JsonObject;
import net.elysiastudios.client.gui.ModuleSettingsScreen;
import net.elysiastudios.client.setting.BooleanModuleSetting;
import net.elysiastudios.client.setting.EnumModuleSetting;
import net.elysiastudios.client.setting.KeybindModuleSetting;
import net.elysiastudios.client.setting.ModuleSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public abstract class Module implements ConfigurableModule, PersistentModuleSettings {
    private final String name;
    private final String description;
    private final Category category;
    private final List<ModuleSetting<?>> settings;
    private int keyBind;
    private boolean enabled;
    private String icon;
    private boolean toggleFeedback;
    private ToggleFeedbackMode toggleFeedbackMode;

    protected static final Minecraft mc = Minecraft.getInstance();

    public Module(String name, String description, Category category, int keyBind) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.settings = new ArrayList<>();
        this.keyBind = keyBind;
        this.enabled = false;
        this.icon = "MOD";
        this.toggleFeedback = false;
        this.toggleFeedbackMode = ToggleFeedbackMode.SHORT;
        registerBaseSettings();
    }

    public Module(String name, String description, Category category, int keyBind, String icon) {
        this(name, description, category, keyBind);
        this.icon = icon;
    }

    public void setEnabled(boolean enabled) {
        if (this.enabled != enabled) {
            this.enabled = enabled;
            if (enabled) {
                onEnable();
            } else {
                onDisable();
            }

            if (shouldEmitToggleFeedback() && toggleFeedback && mc.gui != null) {
                mc.gui.setOverlayMessage(Component.literal(getToggleFeedbackMessage(enabled)), false);
            }
            net.elysiastudios.client.config.ConfigManager.getInstance().save();
        }
    }

    public void toggle() {
        setEnabled(!enabled);
    }

    public void onEnable() {}

    public void onDisable() {}

    public void onTick() {}

    protected void addSetting(ModuleSetting<?> setting) {
        settings.add(setting);
    }

    private void registerBaseSettings() {
        addSetting(new BooleanModuleSetting(
            "enabled",
            "Activé",
            "Active ou désactive rapidement le module.",
            "Général",
            false,
            this::isEnabled,
            this::setEnabled,
            false
        ));
        addSetting(new KeybindModuleSetting(
            "keybind",
            "Touche",
            "Raccourci clavier du module.",
            "Général",
            true,
            this::getKeyBind,
            this::setKeyBind,
            0
        ));
        addSetting(new BooleanModuleSetting(
            "toggle_feedback",
            "Retour visuel",
            "Affiche un message discret quand le module change d'état.",
            "Retour",
            true,
            () -> toggleFeedback,
            value -> toggleFeedback = value,
            false
        ));
        addSetting(new EnumModuleSetting<>(
            "toggle_feedback_mode",
            "Style du retour",
            "Choisit un retour compact ou détaillé.",
            "Retour",
            true,
            () -> toggleFeedbackMode,
            value -> toggleFeedbackMode = value,
            ToggleFeedbackMode.SHORT,
            ToggleFeedbackMode.values(),
            ToggleFeedbackMode::getLabel
        ));
    }

    public String getName() { return name; }

    public String getDescription() { return description; }

    public Category getCategory() { return category; }

    public int getKeyBind() { return keyBind; }

    public void setKeyBind(int keyBind) { this.keyBind = keyBind; }

    public boolean isEnabled() { return enabled; }

    public String getIcon() { return icon; }

    public boolean isHud() { return category == Category.HUD; }

    public String getId() {
        return name.toLowerCase().replace(" ", "-");
    }

    public List<ModuleSetting<?>> getSettings() {
        return List.copyOf(settings);
    }

    public void resetSettings() {
        for (ModuleSetting<?> setting : settings) {
            setting.reset();
        }
    }

    protected boolean shouldEmitToggleFeedback() {
        return true;
    }

    protected String getToggleFeedbackMessage(boolean enabled) {
        if (toggleFeedbackMode == ToggleFeedbackMode.DETAILED) {
            return getName() + (enabled ? " activé" : " désactivé");
        }
        return getName() + (enabled ? " ON" : " OFF");
    }

    @Override
    public Screen createConfigScreen(Screen parent) {
        return new ModuleSettingsScreen(parent, this);
    }

    @Override
    public String getConfigId() {
        return getId();
    }

    @Override
    public void loadModuleSettings(JsonObject json) {
        for (ModuleSetting<?> setting : settings) {
            setting.load(json);
        }
    }

    @Override
    public JsonObject saveModuleSettings() {
        JsonObject json = new JsonObject();
        for (ModuleSetting<?> setting : settings) {
            setting.save(json);
        }
        return json;
    }

    private enum ToggleFeedbackMode {
        SHORT("Court"),
        DETAILED("Détaillé");

        private final String label;

        ToggleFeedbackMode(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }
}
