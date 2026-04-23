package net.elysiastudios.client.module.impl;

import net.elysiastudios.client.module.Category;
import net.minecraft.util.Util;

import java.io.File;

public class OpenConfigFolderModule extends InstantActionModule {
    public OpenConfigFolderModule() {
        super("OpenConfigFolder", "Ouvre le dossier config.", Category.GENERAL, "CFG");
    }

    @Override
    protected void execute() {
        File folder = new File(mc.gameDirectory, "config");
        if (!folder.exists()) folder.mkdirs();
        Util.getPlatform().openFile(folder);
        toast("Dossier config ouvert");
    }
}
