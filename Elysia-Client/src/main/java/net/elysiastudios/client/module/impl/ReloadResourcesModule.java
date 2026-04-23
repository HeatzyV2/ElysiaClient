package net.elysiastudios.client.module.impl;

import net.elysiastudios.client.module.Category;

public class ReloadResourcesModule extends InstantActionModule {
    public ReloadResourcesModule() {
        super("ReloadResources", "Recharge les ressources du client.", Category.GENERAL, "RLD");
    }

    @Override
    protected void execute() {
        mc.reloadResourcePacks();
        toast("Ressources rechargées");
    }
}
