package net.elysiastudios.client.module.impl.smartfight;

import com.google.gson.JsonObject;

public class SmartFightSettings {
    public AnalysisMode analysisMode = AnalysisMode.ADVANCED;
    public CombatMode combatMode = CombatMode.JAVA_COOLDOWN;
    public int combatTimeoutMs = 2800;
    public boolean showOnlyInCombat = true;
    public float hudScale = 1.0F;
    public float hudOpacity = 0.92F;
    public boolean compactMode = false;
    public boolean animations = true;
    public ThemeMode themeMode = ThemeMode.ELYSIA;
    public boolean allowMoveInHudEditor = true;
    public boolean previewInHudEditor = true;

    public final TimingAssistSettings timingAssist = new TimingAssistSettings();
    public final ReachSenseSettings reachSense = new ReachSenseSettings();
    public final RhythmTrackerSettings rhythmTracker = new RhythmTrackerSettings();
    public final PressureAnalyzerSettings pressureAnalyzer = new PressureAnalyzerSettings();
    public final OpponentPatternSettings opponentPattern = new OpponentPatternSettings();
    public final FightSummarySettings fightSummary = new FightSummarySettings();

    public JsonObject toJson() {
        sanitize();

        JsonObject root = new JsonObject();
        root.addProperty("analysisMode", analysisMode.name());
        root.addProperty("combatMode", combatMode.name());
        root.addProperty("combatTimeoutMs", combatTimeoutMs);
        root.addProperty("showOnlyInCombat", showOnlyInCombat);
        root.addProperty("hudScale", hudScale);
        root.addProperty("hudOpacity", hudOpacity);
        root.addProperty("compactMode", compactMode);
        root.addProperty("animations", animations);
        root.addProperty("themeMode", themeMode.name());
        root.addProperty("allowMoveInHudEditor", allowMoveInHudEditor);
        root.addProperty("previewInHudEditor", previewInHudEditor);

        root.add("timingAssist", timingAssist.toJson());
        root.add("reachSense", reachSense.toJson());
        root.add("rhythmTracker", rhythmTracker.toJson());
        root.add("pressureAnalyzer", pressureAnalyzer.toJson());
        root.add("opponentPattern", opponentPattern.toJson());
        root.add("fightSummary", fightSummary.toJson());
        return root;
    }

    public static SmartFightSettings fromJson(JsonObject root) {
        SmartFightSettings settings = new SmartFightSettings();
        if (root == null) {
            return settings;
        }

        settings.analysisMode = readEnum(root, "analysisMode", AnalysisMode.class, settings.analysisMode);
        settings.combatMode = readEnum(root, "combatMode", CombatMode.class, settings.combatMode);
        settings.combatTimeoutMs = readInt(root, "combatTimeoutMs", settings.combatTimeoutMs);
        settings.showOnlyInCombat = readBoolean(root, "showOnlyInCombat", settings.showOnlyInCombat);
        settings.hudScale = readFloat(root, "hudScale", settings.hudScale);
        settings.hudOpacity = readFloat(root, "hudOpacity", settings.hudOpacity);
        settings.compactMode = readBoolean(root, "compactMode", settings.compactMode);
        settings.animations = readBoolean(root, "animations", settings.animations);
        settings.themeMode = readEnum(root, "themeMode", ThemeMode.class, settings.themeMode);
        settings.allowMoveInHudEditor = readBoolean(root, "allowMoveInHudEditor", settings.allowMoveInHudEditor);
        settings.previewInHudEditor = readBoolean(root, "previewInHudEditor", settings.previewInHudEditor);

        settings.timingAssist.load(root.getAsJsonObject("timingAssist"));
        settings.reachSense.load(root.getAsJsonObject("reachSense"));
        settings.rhythmTracker.load(root.getAsJsonObject("rhythmTracker"));
        settings.pressureAnalyzer.load(root.getAsJsonObject("pressureAnalyzer"));
        settings.opponentPattern.load(root.getAsJsonObject("opponentPattern"));
        settings.fightSummary.load(root.getAsJsonObject("fightSummary"));
        settings.sanitize();
        return settings;
    }

    public void sanitize() {
        combatTimeoutMs = clamp(combatTimeoutMs, 1200, 5000);
        hudScale = clamp(hudScale, 0.7F, 1.6F);
        hudOpacity = clamp(hudOpacity, 0.45F, 1.0F);
        timingAssist.feedbackIntensity = clamp(timingAssist.feedbackIntensity, 0.15F, 1.0F);
        timingAssist.smoothing = clamp(timingAssist.smoothing, 0.10F, 0.95F);
        reachSense.smoothing = clamp(reachSense.smoothing, 0.10F, 0.95F);
        opponentPattern.confidenceThreshold = clamp(opponentPattern.confidenceThreshold, 0.15F, 0.95F);
        fightSummary.durationSeconds = clamp(fightSummary.durationSeconds, 2, 10);
    }

    public enum AnalysisMode {
        NORMAL("Normal"),
        ADVANCED("Avancé");

        private final String label;

        AnalysisMode(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    public enum CombatMode {
        JAVA_COOLDOWN("Java Cooldown"),
        CRYSTAL_PVP("Crystal PvP"),
        MACE("Mace"),
        PVP_1_8("PvP 1.8");

        private final String label;

        CombatMode(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    public enum ThemeMode {
        ELYSIA("Elysia"),
        GLACIAL("Glacial"),
        EMBER("Ember");

        private final String label;

        ThemeMode(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    public enum DisplayMode {
        OFF("Désactivé"),
        WIDGET("Widget"),
        CROSSHAIR("Crosshair"),
        BOTH("Les deux");

        private final String label;

        DisplayMode(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    public enum RhythmDisplayMode {
        OFF("Désactivé"),
        WIDGET("Widget"),
        ICON("Icône");

        private final String label;

        RhythmDisplayMode(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    public enum DetailLevel {
        SIMPLE("Simple"),
        ADVANCED("Avancé");

        private final String label;

        DetailLevel(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    public static class TimingAssistSettings {
        public boolean enabled = true;
        public DisplayMode displayMode = DisplayMode.BOTH;
        public float feedbackIntensity = 0.55F;
        public float smoothing = 0.65F;

        private JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("enabled", enabled);
            json.addProperty("displayMode", displayMode.name());
            json.addProperty("feedbackIntensity", feedbackIntensity);
            json.addProperty("smoothing", smoothing);
            return json;
        }

        private void load(JsonObject json) {
            if (json == null) {
                return;
            }
            enabled = readBoolean(json, "enabled", enabled);
            displayMode = readEnum(json, "displayMode", DisplayMode.class, displayMode);
            feedbackIntensity = readFloat(json, "feedbackIntensity", feedbackIntensity);
            smoothing = readFloat(json, "smoothing", smoothing);
        }
    }

    public static class ReachSenseSettings {
        public boolean enabled = true;
        public DisplayMode displayMode = DisplayMode.BOTH;
        public boolean showDistanceState = true;
        public float smoothing = 0.60F;

        private JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("enabled", enabled);
            json.addProperty("displayMode", displayMode.name());
            json.addProperty("showDistanceState", showDistanceState);
            json.addProperty("smoothing", smoothing);
            return json;
        }

        private void load(JsonObject json) {
            if (json == null) {
                return;
            }
            enabled = readBoolean(json, "enabled", enabled);
            displayMode = readEnum(json, "displayMode", DisplayMode.class, displayMode);
            showDistanceState = readBoolean(json, "showDistanceState", showDistanceState);
            smoothing = readFloat(json, "smoothing", smoothing);
        }
    }

    public static class RhythmTrackerSettings {
        public boolean enabled = true;
        public RhythmDisplayMode displayMode = RhythmDisplayMode.WIDGET;
        public DetailLevel detailLevel = DetailLevel.SIMPLE;

        private JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("enabled", enabled);
            json.addProperty("displayMode", displayMode.name());
            json.addProperty("detailLevel", detailLevel.name());
            return json;
        }

        private void load(JsonObject json) {
            if (json == null) {
                return;
            }
            enabled = readBoolean(json, "enabled", enabled);
            displayMode = readEnum(json, "displayMode", RhythmDisplayMode.class, displayMode);
            detailLevel = readEnum(json, "detailLevel", DetailLevel.class, detailLevel);
        }
    }

    public static class PressureAnalyzerSettings {
        public boolean enabled = true;
        public DetailLevel detailLevel = DetailLevel.SIMPLE;

        private JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("enabled", enabled);
            json.addProperty("detailLevel", detailLevel.name());
            return json;
        }

        private void load(JsonObject json) {
            if (json == null) {
                return;
            }
            enabled = readBoolean(json, "enabled", enabled);
            detailLevel = readEnum(json, "detailLevel", DetailLevel.class, detailLevel);
        }
    }

    public static class OpponentPatternSettings {
        public boolean enabled = true;
        public boolean showEstimatedPattern = true;
        public float confidenceThreshold = 0.45F;

        private JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("enabled", enabled);
            json.addProperty("showEstimatedPattern", showEstimatedPattern);
            json.addProperty("confidenceThreshold", confidenceThreshold);
            return json;
        }

        private void load(JsonObject json) {
            if (json == null) {
                return;
            }
            enabled = readBoolean(json, "enabled", enabled);
            showEstimatedPattern = readBoolean(json, "showEstimatedPattern", showEstimatedPattern);
            confidenceThreshold = readFloat(json, "confidenceThreshold", confidenceThreshold);
        }
    }

    public static class FightSummarySettings {
        public boolean enabled = true;
        public int durationSeconds = 6;
        public boolean showDiagnosis = true;
        public boolean showAccuracy = true;
        public boolean showSpacing = true;
        public boolean showRhythm = true;
        public boolean showPressure = true;

        private JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("enabled", enabled);
            json.addProperty("durationSeconds", durationSeconds);
            json.addProperty("showDiagnosis", showDiagnosis);
            json.addProperty("showAccuracy", showAccuracy);
            json.addProperty("showSpacing", showSpacing);
            json.addProperty("showRhythm", showRhythm);
            json.addProperty("showPressure", showPressure);
            return json;
        }

        private void load(JsonObject json) {
            if (json == null) {
                return;
            }
            enabled = readBoolean(json, "enabled", enabled);
            durationSeconds = readInt(json, "durationSeconds", durationSeconds);
            showDiagnosis = readBoolean(json, "showDiagnosis", showDiagnosis);
            showAccuracy = readBoolean(json, "showAccuracy", showAccuracy);
            showSpacing = readBoolean(json, "showSpacing", showSpacing);
            showRhythm = readBoolean(json, "showRhythm", showRhythm);
            showPressure = readBoolean(json, "showPressure", showPressure);
        }
    }

    private static boolean readBoolean(JsonObject json, String key, boolean fallback) {
        return json != null && json.has(key) ? json.get(key).getAsBoolean() : fallback;
    }

    private static int readInt(JsonObject json, String key, int fallback) {
        return json != null && json.has(key) ? json.get(key).getAsInt() : fallback;
    }

    private static float readFloat(JsonObject json, String key, float fallback) {
        return json != null && json.has(key) ? json.get(key).getAsFloat() : fallback;
    }

    private static <E extends Enum<E>> E readEnum(JsonObject json, String key, Class<E> type, E fallback) {
        if (json == null || !json.has(key)) {
            return fallback;
        }

        try {
            return Enum.valueOf(type, json.get(key).getAsString());
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
