package net.elysiastudios.client.module.impl;

import net.elysiastudios.client.module.Category;
import net.minecraft.util.Util;

import java.io.File;

public class OpenModsFolderModule extends InstantActionModule {
    public OpenModsFolderModule() {
        super("OpenModsFolder", "Ouvre le dossier mods.", Category.GENERAL, "MODS");
    }

    @Override
    protected void execute() {
        File folder = new File(mc.gameDirectory, "mods");
        if (!folder.exists()) folder.mkdirs();
        Util.getPlatform().openFile(folder);
        toast("Dossier mods ouvert");
    }
}
