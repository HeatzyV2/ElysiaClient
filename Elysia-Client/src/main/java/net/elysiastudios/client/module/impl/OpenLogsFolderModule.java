package net.elysiastudios.client.module.impl;

import net.elysiastudios.client.module.Category;
import net.minecraft.util.Util;

import java.io.File;

public class OpenLogsFolderModule extends InstantActionModule {
    public OpenLogsFolderModule() {
        super("OpenLogsFolder", "Ouvre le dossier logs.", Category.GENERAL, "LOG");
    }

    @Override
    protected void execute() {
        File folder = new File(mc.gameDirectory, "logs");
        if (!folder.exists()) folder.mkdirs();
        Util.getPlatform().openFile(folder);
        toast("Dossier logs ouvert");
    }
}
