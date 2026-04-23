package net.elysiastudios.client.module;

public enum Category {
    COMBAT("Combat", "COM"),
    MOVEMENT("Mouvement", "MOV"),
    RENDER("Visuel", "VIS"),
    OPTIMIZATION("Optimisations", "FPS"),
    UTILITY("Utilitaire", "UTL"),
    HUD("HUD", "HUD"),
    GENERAL("Général", "GEN");

    private final String name;
    private final String icon;

    Category(String name, String icon) {
        this.name = name;
        this.icon = icon;
    }

    public String getName() {
        return name;
    }

    public String getIcon() {
        return icon;
    }
}
