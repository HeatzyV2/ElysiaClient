package net.elysiastudios.client.module.impl;

import net.elysiastudios.client.module.Category;
import net.minecraft.util.Util;

import java.io.File;

public class OpenScreenshotsFolderModule extends InstantActionModule {
    public OpenScreenshotsFolderModule() {
        super("OpenScreenshots", "Ouvre le dossier des captures d'écran.", Category.GENERAL, "SHOT");
    }

    @Override
    protected void execute() {
        File folder = new File(mc.gameDirectory, "screenshots");
        if (!folder.exists()) {
            folder.mkdirs();
        }
        Util.getPlatform().openFile(folder);
        toast("Dossier captures d'écran ouvert");
    }
}
