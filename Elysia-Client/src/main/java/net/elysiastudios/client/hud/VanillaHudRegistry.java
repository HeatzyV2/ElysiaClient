package net.elysiastudios.client.hud;

import java.util.ArrayList;
import java.util.List;

/**
 * Registry of vanilla HUD elements that can be moved/scaled in the HUD editor.
 */
public class VanillaHudRegistry {
    private static final List<VanillaHudElement> elements = new ArrayList<>();

    static {
        elements.add(new VanillaHudElement("scoreboard", "Tableau des scores", "BOARD", 180, 132,
            (w, h) -> w - 190,
            (w, h) -> h / 2 - 66
        ));

        elements.add(new VanillaHudElement("bossbar", "Barre de boss", "BOSS", 182, 19,
            (w, h) -> w / 2 - 91,
            (w, h) -> 12
        ));

        elements.add(new VanillaHudElement("effects", "Effets", "FX", 86, 72,
            (w, h) -> w - 96,
            (w, h) -> 10
        ));

        elements.add(new VanillaHudElement("hotbar", "Hotbar", "HOTBAR", 182, 22,
            (w, h) -> w / 2 - 91,
            (w, h) -> h - 22
        ));
    }

    public static List<VanillaHudElement> getElements() {
        return elements;
    }

    public static VanillaHudElement getElement(String id) {
        for (VanillaHudElement element : elements) {
            if (element.getId().equals(id)) {
                return element;
            }
        }
        return null;
    }
}
