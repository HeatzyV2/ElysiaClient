package net.elysiastudios.client.module.impl;

public class MemoryPercentModule extends TextHudModule {
    public MemoryPercentModule() {
        super("MemoryPercent", "Affiche le pourcentage de RAM utilisée.", "RAM%");
    }

    @Override
    protected String getText() {
        Runtime runtime = Runtime.getRuntime();
        long used = runtime.totalMemory() - runtime.freeMemory();
        int percent = (int) (used * 100L / runtime.maxMemory());
        return "Memory " + percent + "%";
    }

    @Override
    protected int getAccentColor() {
        Runtime runtime = Runtime.getRuntime();
        long used = runtime.totalMemory() - runtime.freeMemory();
        int percent = (int) (used * 100L / runtime.maxMemory());
        return percent < 70 ? 0xFF22C55E : percent < 85 ? 0xFFF59E0B : 0xFFEF4444;
    }
}
