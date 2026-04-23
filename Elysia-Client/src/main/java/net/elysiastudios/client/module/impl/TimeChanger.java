package net.elysiastudios.client.module.impl;

import net.elysiastudios.client.module.Category;
import net.elysiastudios.client.module.Module;
import net.elysiastudios.client.setting.EnumModuleSetting;
import net.elysiastudios.client.setting.IntModuleSetting;

public class TimeChanger extends Module {
    private TimePreset preset;
    private int customTime;

    public TimeChanger() {
        super("TimeChanger", "Verrouille le monde client sur une lumière de jour.", Category.RENDER, 0, "DAY");
        this.preset = TimePreset.DAY;
        this.customTime = 1000;
        registerTimeSettings();
    }

    @Override
    public void onTick() {
        if (mc.level != null) {
            mc.level.getLevelData().setDayTime(resolveTimeValue());
        }
    }

    private long resolveTimeValue() {
        return switch (preset) {
            case DAWN -> 23000L;
            case DAY -> 1000L;
            case SUNSET -> 12000L;
            case NIGHT -> 18000L;
            case CUSTOM -> customTime;
        };
    }

    private void registerTimeSettings() {
        addSetting(new EnumModuleSetting<>(
            "time_preset",
            "Preset",
            "Choisit le moment de la journée affiché côté client.",
            "Temps",
            true,
            () -> preset,
            value -> preset = value,
            TimePreset.DAY,
            TimePreset.values(),
            TimePreset::getLabel
        ));
        addSetting(new IntModuleSetting(
            "time_custom_ticks",
            "Temps personnalisé",
            "Utilisé uniquement si le preset est réglé sur Personnalisé.",
            "Temps",
            true,
            () -> customTime,
            value -> customTime = value,
            1000,
            0,
            23999,
            250,
            Integer::toString
        ));
    }

    private enum TimePreset {
        DAWN("Aube"),
        DAY("Jour"),
        SUNSET("Crépuscule"),
        NIGHT("Nuit"),
        CUSTOM("Personnalisé");

        private final String label;

        TimePreset(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }
}
