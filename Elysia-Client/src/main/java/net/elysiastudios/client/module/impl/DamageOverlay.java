package net.elysiastudios.client.module.impl;

import net.elysiastudios.client.module.Category;
import net.elysiastudios.client.module.Module;

public class DamageOverlay extends Module {
    public DamageOverlay() {
        super("DamageOverlay", "Intensifie l'effet visuel quand vous recevez des dégâts.", Category.COMBAT, 0, "💢");
    }
}
