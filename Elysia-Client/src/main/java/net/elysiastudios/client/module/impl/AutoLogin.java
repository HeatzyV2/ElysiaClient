package net.elysiastudios.client.module.impl;

import net.elysiastudios.client.module.Category;
import net.elysiastudios.client.module.Module;

public class AutoLogin extends Module {
    public AutoLogin() {
        super("AutoLogin", "Se connecte automatiquement au serveur.", Category.UTILITY, 0, "🔑");
    }
}
