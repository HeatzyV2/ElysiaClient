package net.elysiastudios.client.module.impl;

import net.elysiastudios.client.module.Category;

public class ClearChatModule extends InstantActionModule {
    public ClearChatModule() {
        super("ClearChat", "Nettoie le chat client.", Category.UTILITY, "CHAT");
    }

    @Override
    protected void execute() {
        mc.gui.getChat().clearMessages(false);
        toast("Chat nettoyé");
    }
}
