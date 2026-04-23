package net.elysiastudios;

import net.elysiastudios.client.config.ConfigManager;
import net.elysiastudios.client.module.ModuleManager;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ElysiaClient implements ModInitializer {
    public static final String MOD_ID = "elysia-client";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static boolean isVIP() {
        return Boolean.getBoolean("elysia.vip");
    }

    @Override
    public void onInitialize() {
        LOGGER.info("Initialisation du Core Elysia Client...");
        
        // Initialiser les modules
        ModuleManager.getInstance();
        
        // Charger la configuration (Settings HUD et Modules)
        ConfigManager.getInstance().load();
        
        LOGGER.info("Elysia Client est prêt !");
    }
}
