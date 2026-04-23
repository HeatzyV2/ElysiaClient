package net.elysiastudios.client.module.impl;

import net.elysiastudios.client.module.Category;
import net.elysiastudios.client.module.Module;
import net.elysiastudios.client.module.ModuleManager;

public class PanicDisableModule extends InstantActionModule {
    public PanicDisableModule() {
        super("PanicDisable", "Désactive tous les modules actifs.", Category.GENERAL, "OFF");
    }

    @Override
    protected void execute() {
        int disabled = 0;
        for (Module module : ModuleManager.getInstance().getModules()) {
            if (module != this && module.isEnabled()) {
                module.setEnabled(false);
                disabled++;
            }
        }
        toast(disabled + " modules désactivés");
    }
}
