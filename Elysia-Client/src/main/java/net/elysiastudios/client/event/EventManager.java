package net.elysiastudios.client.event;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.elysiastudios.client.module.ModuleManager;
import net.minecraft.client.Minecraft;

public class EventManager {
    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null) {
                ModuleManager.getInstance().onTick();
            }
        });
    }
}
