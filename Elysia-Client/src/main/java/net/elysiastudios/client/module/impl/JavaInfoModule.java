package net.elysiastudios.client.module.impl;

public class JavaInfoModule extends TextHudModule {
    public JavaInfoModule() {
        super("JavaInfo", "Affiche la version Java utilisée.", "JAVA");
    }

    @Override
    protected String getText() {
        return "Java " + System.getProperty("java.version", "unknown");
    }
}
