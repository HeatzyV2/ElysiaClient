package net.elysiastudios.client.module.impl.smartfight;

public final class SmartFightStates {
    private SmartFightStates() {
    }

    public enum TimingState {
        GOOD("Propre", 0xFF22C55E),
        SOLID("Bon", 0xFF38BDF8),
        SLIPPING("En baisse", 0xFFF59E0B),
        FORCED("Forcé", 0xFFF97316),
        WASTEFUL("Trop de swings", 0xFFEF4444);

        private final String label;
        private final int color;

        TimingState(String label, int color) {
            this.label = label;
            this.color = color;
        }

        public String getLabel() {
            return label;
        }

        public int getColor() {
            return color;
        }
    }

    public enum SpacingState {
        OPTIMAL("Optimal", 0xFF22C55E),
        TOO_CLOSE("Trop proche", 0xFFF97316),
        TOO_FAR("Trop loin", 0xFFF59E0B),
        UNSTABLE("Instable", 0xFFEF4444);

        private final String label;
        private final int color;

        SpacingState(String label, int color) {
            this.label = label;
            this.color = color;
        }

        public String getLabel() {
            return label;
        }

        public int getColor() {
            return color;
        }
    }

    public enum RhythmState {
        STABLE("Stable", 0xFF22C55E),
        AGGRESSIVE("Agressif", 0xFF38BDF8),
        UNSTABLE("Instable", 0xFFF97316),
        PASSIVE("Passif", 0xFFF59E0B),
        DESYNCED("Désync.", 0xFFEF4444);

        private final String label;
        private final int color;

        RhythmState(String label, int color) {
            this.label = label;
            this.color = color;
        }

        public String getLabel() {
            return label;
        }

        public int getColor() {
            return color;
        }
    }

    public enum PressureState {
        APPLYING_PRESSURE("En main", 0xFF22C55E),
        NEUTRAL("Neutre", 0xFF38BDF8),
        UNDER_PRESSURE("Subie", 0xFFEF4444),
        RECOVERING("Reprise", 0xFFF59E0B);

        private final String label;
        private final int color;

        PressureState(String label, int color) {
            this.label = label;
            this.color = color;
        }

        public String getLabel() {
            return label;
        }

        public int getColor() {
            return color;
        }
    }

    public enum OpponentPattern {
        AGGRESSIVE("Agressif", 0xFFFB7185),
        RESET_HIT("Recul + hit", 0xFF38BDF8),
        JUMP_RESET("Jump reset", 0xFFF59E0B),
        SPAM("Spam", 0xFFF97316),
        FRONTAL("Frontal", 0xFF60A5FA),
        CONSTANT_PRESSURE("Pression constante", 0xFF34D399),
        CHAOTIC("Chaotique", 0xFFEF4444),
        STABLE("Stable", 0xFF22C55E),
        UNKNOWN("Lecture...", 0xFF64748B);

        private final String label;
        private final int color;

        OpponentPattern(String label, int color) {
            this.label = label;
            this.color = color;
        }

        public String getLabel() {
            return label;
        }

        public int getColor() {
            return color;
        }
    }

    public enum FightInterpretation {
        CONTROL_STABLE("Contrôle stable", 0xFF22C55E),
        LOSING_RHYTHM("Rythme en perte", 0xFFF59E0B),
        POOR_SPACING("Spacing fragile", 0xFFF97316),
        OVERCOMMITTING("Sur-engage", 0xFFEF4444),
        STABLE_EXCHANGES("Échanges propres", 0xFF38BDF8),
        PANIC_PATTERN("Pattern panique", 0xFFFB7185),
        CLEAN_PRESSURE("Pression propre", 0xFF22C55E),
        INCONSISTENT_TIMING("Timing irrégulier", 0xFFF59E0B),
        RECOVERY("Reprise en cours", 0xFF38BDF8),
        PRESSURE_LOST("Pression perdue", 0xFFEF4444),
        GOOD_ENGAGE("Engages propres", 0xFF34D399),
        TEMPO_UNSTABLE("Tempo instable", 0xFFF97316);

        private final String label;
        private final int color;

        FightInterpretation(String label, int color) {
            this.label = label;
            this.color = color;
        }

        public String getLabel() {
            return label;
        }

        public int getColor() {
            return color;
        }
    }
}
