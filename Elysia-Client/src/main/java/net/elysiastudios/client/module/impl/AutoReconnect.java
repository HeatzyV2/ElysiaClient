package net.elysiastudios.client.module.impl;

import net.elysiastudios.client.module.Category;
import net.elysiastudios.client.module.Module;

public class AutoReconnect extends Module {
    public AutoReconnect() {
        super("AutoReconnect", "Se reconnecte automatiquement au serveur.", Category.UTILITY, 0, "🔌");
    }
}
