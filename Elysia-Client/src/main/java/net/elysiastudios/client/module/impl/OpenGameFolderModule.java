package net.elysiastudios.client.module.impl;

import net.elysiastudios.client.module.Category;
import net.minecraft.util.Util;

public class OpenGameFolderModule extends InstantActionModule {
    public OpenGameFolderModule() {
        super("OpenGameFolder", "Ouvre le dossier Minecraft.", Category.GENERAL, "DIR");
    }

    @Override
    protected void execute() {
        Util.getPlatform().openFile(mc.gameDirectory);
        toast("Dossier Minecraft ouvert");
    }
}
