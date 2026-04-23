package net.elysiastudios.client.module.impl;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class ClockSecondsModule extends TextHudModule {
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    public ClockSecondsModule() {
        super("ClockSeconds", "Affiche l'heure avec les secondes.", "TIME");
    }

    @Override
    protected String getText() {
        return LocalTime.now().format(FORMAT);
    }
}
