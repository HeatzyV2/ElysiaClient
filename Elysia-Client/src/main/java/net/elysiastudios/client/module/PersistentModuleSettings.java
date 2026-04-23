package net.elysiastudios.client.module;

import com.google.gson.JsonObject;

public interface PersistentModuleSettings {
    String getConfigId();

    void loadModuleSettings(JsonObject json);

    JsonObject saveModuleSettings();
}
