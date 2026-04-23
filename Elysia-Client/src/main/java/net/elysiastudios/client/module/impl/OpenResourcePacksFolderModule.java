package net.elysiastudios.client.module.impl;

import net.elysiastudios.client.module.Category;
import net.minecraft.util.Util;

import java.io.File;

public class OpenResourcePacksFolderModule extends InstantActionModule {
    public OpenResourcePacksFolderModule() {
        super("OpenResourcePacks", "Ouvre le dossier des packs de ressources.", Category.GENERAL, "RP");
    }

    @Override
    protected void execute() {
        File folder = new File(mc.gameDirectory, "resourcepacks");
        if (!folder.exists()) folder.mkdirs();
        Util.getPlatform().openFile(folder);
        toast("Dossier packs de ressources ouvert");
    }
}
