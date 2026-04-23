package net.elysiastudios.client.module.impl;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class DateModule extends TextHudModule {
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public DateModule() {
        super("Date", "Affiche la date actuelle.", "DATE");
    }

    @Override
    protected String getText() {
        return LocalDate.now().format(FORMAT);
    }
}
