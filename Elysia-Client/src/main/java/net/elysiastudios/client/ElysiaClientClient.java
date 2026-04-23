package net.elysiastudios.client;

import net.elysiastudios.ElysiaClient;
import net.elysiastudios.client.compat.EssentialCompat;
import net.elysiastudios.client.event.ClickTracker;
import net.elysiastudios.client.event.EventManager;
import net.elysiastudios.client.hud.HudManager;
import net.elysiastudios.client.module.Module;
import net.elysiastudios.client.module.ModuleManager;
import net.elysiastudios.client.social.ElysiaPresenceManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;

@Environment(EnvType.CLIENT)
public class ElysiaClientClient implements ClientModInitializer {
    private final Map<Integer, Boolean> keyStates = new HashMap<>();
    private boolean leftMouseDown;

    @Override
    public void onInitializeClient() {
        ElysiaClient.LOGGER.info("------------------------------------------");
        ElysiaClient.LOGGER.info("   ELYSIA CLIENT - VERSION CELESTIAL      ");
        ElysiaClient.LOGGER.info("   Modular Architecture (Feather-Style)  ");
        ElysiaClient.LOGGER.info("------------------------------------------");
        if (EssentialCompat.isEssentialLoaded()) {
            ElysiaClient.LOGGER.info("Essential detecte : mode compatibilite actif (menu titre et avatar Elysia desactives).");
        }

        ModuleManager.getInstance();
        net.elysiastudios.client.config.ConfigManager.getInstance().load();
        EventManager.init();
        HudManager.init();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            ElysiaPresenceManager.getInstance().tick(client);

            long window = GLFW.glfwGetCurrentContext();
            if (window == 0L) {
                return;
            }

            for (Module module : ModuleManager.getInstance().getModules()) {
                int keyBind = module.getKeyBind();
                if (keyBind <= 0) {
                    continue;
                }

                boolean pressed = GLFW.glfwGetKey(window, keyBind) == GLFW.GLFW_PRESS;
                boolean wasPressed = keyStates.getOrDefault(keyBind, false);
                if (pressed && !wasPressed) {
                    module.toggle();
                }
                keyStates.put(keyBind, pressed);
            }

            boolean currentlyMouseDown = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
            if (client.player != null && currentlyMouseDown && !leftMouseDown) {
                ClickTracker.addLeftClick();
            }
            leftMouseDown = currentlyMouseDown;
        });
    }
}
