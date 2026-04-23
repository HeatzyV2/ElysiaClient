package net.elysiastudios.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.elysiastudios.ElysiaClient;
import net.elysiastudios.client.module.Module;
import net.elysiastudios.client.module.ModuleManager;
import net.elysiastudios.client.module.PersistentModuleSettings;
import net.minecraft.client.Minecraft;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;

public class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = new File(Minecraft.getInstance().gameDirectory, "config/elysia-client/features.json");

    private static ConfigManager instance;
    private final Map<String, HudConfig> hudConfigs = new HashMap<>();
    private final Map<String, JsonObject> moduleData = new HashMap<>();
    private boolean loading;

    public static ConfigManager getInstance() {
        if (instance == null) instance = new ConfigManager();
        return instance;
    }

    public void load() {
        loading = true;
        if (!CONFIG_FILE.exists()) {
            loading = false;
            save();
            return;
        }

        try (FileReader reader = new FileReader(CONFIG_FILE)) {
            JsonObject json = GSON.fromJson(reader, JsonObject.class);

            if (json.has("modules")) {
                JsonObject modulesJson = json.getAsJsonObject("modules");
                for (Module module : ModuleManager.getInstance().getModules()) {
                    String id = module.getName().toLowerCase().replace(" ", "-");
                    if (modulesJson.has(id)) {
                        module.setEnabled(modulesJson.getAsJsonObject(id).get("enabled").getAsBoolean());
                    }
                }
            }

            if (json.has("hud")) {
                JsonObject hudJson = json.getAsJsonObject("hud");
                for (String key : hudJson.keySet()) {
                    JsonObject entry = hudJson.getAsJsonObject(key);
                    HudConfig config = new HudConfig(
                        entry.get("x").getAsInt(),
                        entry.get("y").getAsInt(),
                        entry.get("scale").getAsFloat(),
                        entry.get("visible").getAsBoolean()
                    );
                    hudConfigs.put(key, config);
                }
            }

            if (json.has("moduleData")) {
                JsonObject moduleDataJson = json.getAsJsonObject("moduleData");
                for (String key : moduleDataJson.keySet()) {
                    moduleData.put(key, moduleDataJson.getAsJsonObject(key).deepCopy());
                }
            }

            for (Module module : ModuleManager.getInstance().getModules()) {
                if (module instanceof PersistentModuleSettings persistentModuleSettings) {
                    JsonObject stored = moduleData.getOrDefault(persistentModuleSettings.getConfigId(), new JsonObject());
                    persistentModuleSettings.loadModuleSettings(stored.deepCopy());
                }
            }
        } catch (Exception e) {
            ElysiaClient.LOGGER.error("Erreur lors du chargement de la config: {}", e.getMessage());
        } finally {
            loading = false;
        }
    }

    public void save() {
        if (loading) {
            return;
        }

        try {
            if (!CONFIG_FILE.getParentFile().exists()) {
                CONFIG_FILE.getParentFile().mkdirs();
            }

            JsonObject json = new JsonObject();
            
            JsonObject modulesJson = new JsonObject();
            ModuleManager moduleManager = ModuleManager.getInstance();
            if (moduleManager == null) {
                return;
            }

            for (Module module : moduleManager.getModules()) {
                String id = module.getName().toLowerCase().replace(" ", "-");
                JsonObject modJson = new JsonObject();
                modJson.addProperty("enabled", module.isEnabled());
                modulesJson.add(id, modJson);
            }
            json.add("modules", modulesJson);

            JsonObject hudJson = new JsonObject();
            for (Map.Entry<String, HudConfig> entry : hudConfigs.entrySet()) {
                JsonObject configJson = new JsonObject();
                configJson.addProperty("x", entry.getValue().x);
                configJson.addProperty("y", entry.getValue().y);
                configJson.addProperty("scale", entry.getValue().scale);
                configJson.addProperty("visible", entry.getValue().visible);
                hudJson.add(entry.getKey(), configJson);
            }
            json.add("hud", hudJson);

            for (Module module : moduleManager.getModules()) {
                if (module instanceof PersistentModuleSettings persistentModuleSettings) {
                    moduleData.put(persistentModuleSettings.getConfigId(), persistentModuleSettings.saveModuleSettings().deepCopy());
                }
            }

            JsonObject moduleDataJson = new JsonObject();
            for (Map.Entry<String, JsonObject> entry : moduleData.entrySet()) {
                moduleDataJson.add(entry.getKey(), entry.getValue().deepCopy());
            }
            json.add("moduleData", moduleDataJson);

            try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
                GSON.toJson(json, writer);
            }
        } catch (Exception e) {
            ElysiaClient.LOGGER.error("Erreur lors de la sauvegarde de la config: {}", e.getMessage());
        }
    }

    public HudConfig getHudConfig(String id) {
        return hudConfigs.computeIfAbsent(id, k -> new HudConfig(10, 10, 1.0f, true));
    }

    public HudConfig getHudConfig(String id, int defaultX, int defaultY, float defaultScale, boolean visible) {
        return hudConfigs.computeIfAbsent(id, k -> new HudConfig(defaultX, defaultY, defaultScale, visible));
    }

    public JsonObject getModuleData(String id) {
        return moduleData.computeIfAbsent(id, ignored -> new JsonObject()).deepCopy();
    }

    public void setModuleData(String id, JsonObject jsonObject) {
        moduleData.put(id, jsonObject == null ? new JsonObject() : jsonObject.deepCopy());
    }

    public static class HudConfig {
        public int x, y;
        public float scale;
        public boolean visible;

        public HudConfig(int x, int y, float scale, boolean visible) {
            this.x = x;
            this.y = y;
            this.scale = scale;
            this.visible = visible;
        }
    }
}
