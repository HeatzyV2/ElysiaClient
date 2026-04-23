package net.elysiastudios.client.module.impl;

public class ServerBrandModule extends TextHudModule {
    public ServerBrandModule() {
        super("ServerBrand", "Affiche la marque serveur reçue.", "BRAND");
    }

    @Override
    protected String getText() {
        if (mc.getConnection() == null) return null;
        String brand = mc.getConnection().serverBrand();
        return "Brand: " + (brand == null || brand.isBlank() ? "Unknown" : brand);
    }
}
