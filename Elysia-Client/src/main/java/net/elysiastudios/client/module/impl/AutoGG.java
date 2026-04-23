package net.elysiastudios.client.module.impl;

import net.elysiastudios.client.module.Category;
import net.elysiastudios.client.module.Module;

public class AutoGG extends Module {
    public AutoGG() {
        super("AutoGG", "Envoie automatiquement 'GG' à la fin d'une partie.", Category.COMBAT, 0, "💬");
    }

    // Logic would listen to chat or game state.
}
