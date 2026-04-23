package net.elysiastudios.client.module.impl;

import net.elysiastudios.client.module.Category;
import net.elysiastudios.client.module.Module;

public class TPSMeter extends Module {
    public TPSMeter() {
        super("TPSMeter", "Affiche les TPS du serveur.", Category.HUD, 0, "⏱️");
    }
}
