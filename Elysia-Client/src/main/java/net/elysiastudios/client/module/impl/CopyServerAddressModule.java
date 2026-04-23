package net.elysiastudios.client.module.impl;

import net.elysiastudios.client.module.Category;
import net.minecraft.client.multiplayer.ServerData;

public class CopyServerAddressModule extends InstantActionModule {
    public CopyServerAddressModule() {
        super("CopyServerAddress", "Copie l'adresse du serveur actuel.", Category.UTILITY, "IP");
    }

    @Override
    protected void execute() {
        ServerData server = mc.getCurrentServer();
        if (server == null || server.ip == null || server.ip.isBlank()) {
            toast("Aucun serveur distant à copier");
            return;
        }
        mc.keyboardHandler.setClipboard(server.ip);
        toast("Serveur copié : " + server.ip);
    }
}
