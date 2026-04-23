package net.elysiastudios.client.module.impl;

import com.google.gson.JsonObject;
import net.elysiastudios.client.module.Category;
import net.elysiastudios.client.module.Module;
import net.elysiastudios.client.setting.EnumModuleSetting;
import net.elysiastudios.client.setting.IntModuleSetting;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.GraphicsPreset;
import net.minecraft.client.Options;
import net.minecraft.server.level.ParticleStatus;

public class FPSBoost extends Module {
    private static final String SNAPSHOT_KEY = "fpsboost_previous_options";
    private static final int DEFAULT_TEXTURE_MIPMAP_LEVELS = 4;

    private Snapshot previous;
    private BoostPreset preset;
    private int targetFps;

    public FPSBoost() {
        super("FPSBoost", "Applique un profil vidéo léger sans pixeliser les textures.", Category.OPTIMIZATION, 0, "FPS");
        this.preset = BoostPreset.AGGRESSIVE;
        this.targetFps = 120;
        registerBoostSettings();
    }

    @Override
    public void onEnable() {
        if (mc.options == null) return;
        previous = Snapshot.capture(mc.options);
        applyProfile(mc.options);
    }

    @Override
    public void onDisable() {
        if (mc.options == null) return;

        int mipmapsBefore = mc.options.mipmapLevels().get();
        if (previous != null) {
            previous.restore(mc.options);
        } else {
            ensureReadableTextures(mc.options);
            mc.options.save();
        }

        previous = null;
        refreshRenderer(mipmapsBefore != mc.options.mipmapLevels().get());
    }

    @Override
    public void onTick() {
        if (previous == null && mc.options != null) {
            previous = Snapshot.capture(mc.options);
            applyProfile(mc.options);
        }
    }

    @Override
    public void loadModuleSettings(JsonObject json) {
        if (isEnabled() && json != null && json.has(SNAPSHOT_KEY) && json.get(SNAPSHOT_KEY).isJsonObject()) {
            previous = Snapshot.fromJson(json.getAsJsonObject(SNAPSHOT_KEY));
        }

        super.loadModuleSettings(json);

        if (isEnabled() && mc.options != null) {
            if (previous == null) {
                previous = Snapshot.capture(mc.options);
            }
            applyProfile(mc.options);
        }
    }

    @Override
    public JsonObject saveModuleSettings() {
        JsonObject json = super.saveModuleSettings();
        if (previous != null) {
            json.add(SNAPSHOT_KEY, previous.toJson());
        }
        return json;
    }

    private void applyProfile(Options options) {
        int mipmapsBefore = options.mipmapLevels().get();
        options.graphicsPreset().set(preset == BoostPreset.BALANCED ? GraphicsPreset.FABULOUS : GraphicsPreset.FAST);
        options.renderDistance().set(preset.renderDistance);
        options.simulationDistance().set(preset.simulationDistance);
        options.entityDistanceScaling().set(preset.entityScale);
        options.framerateLimit().set(targetFps);
        options.cloudStatus().set(preset.cloudStatus);
        options.cloudRange().set(preset.cloudRange);
        options.weatherRadius().set(preset.weatherRadius);
        options.cutoutLeaves().set(false);
        options.vignette().set(preset != BoostPreset.BALANCED);
        options.improvedTransparency().set(false);
        options.ambientOcclusion().set(preset == BoostPreset.BALANCED);
        options.chunkSectionFadeInTime().set(preset == BoostPreset.BALANCED ? 0.2D : 0.0D);
        ensureReadableTextures(options);
        options.biomeBlendRadius().set(preset == BoostPreset.BALANCED ? 1 : 0);
        options.enableVsync().set(false);
        options.entityShadows().set(preset == BoostPreset.BALANCED);
        options.bobView().set(preset == BoostPreset.BALANCED);
        options.particles().set(preset.particles);
        options.menuBackgroundBlurriness().set(preset == BoostPreset.EXTREME ? 0 : 2);
        options.save();
        refreshRenderer(mipmapsBefore != options.mipmapLevels().get());
    }

    private boolean ensureReadableTextures(Options options) {
        if (options.mipmapLevels().get() > 0) {
            return false;
        }

        options.mipmapLevels().set(DEFAULT_TEXTURE_MIPMAP_LEVELS);
        return true;
    }

    private void reapplyProfileIfEnabled() {
        if (!isEnabled() || mc.options == null) {
            return;
        }

        if (previous == null) {
            previous = Snapshot.capture(mc.options);
        }
        applyProfile(mc.options);
    }

    private void refreshRenderer(boolean reloadTextures) {
        if (mc.levelRenderer != null) {
            mc.levelRenderer.allChanged();
        }
        if (reloadTextures) {
            mc.reloadResourcePacks();
        }
    }

    private void registerBoostSettings() {
        addSetting(new EnumModuleSetting<>(
            "fpsboost_preset",
            "Preset",
            "Choisit l'intensite de l'optimisation.",
            "Boost",
            true,
            () -> preset,
            value -> {
                preset = value;
                reapplyProfileIfEnabled();
            },
            BoostPreset.AGGRESSIVE,
            BoostPreset.values(),
            BoostPreset::getLabel
        ));
        addSetting(new IntModuleSetting(
            "fpsboost_target_fps",
            "Limite FPS",
            "Fixe la limite de FPS pendant le profil d'optimisation.",
            "Boost",
            true,
            () -> targetFps,
            value -> {
                targetFps = value;
                reapplyProfileIfEnabled();
            },
            120,
            60,
            360,
            15,
            value -> value + " FPS"
        ));
    }

    private enum BoostPreset {
        BALANCED("Equilibre", 10, 8, 0.85D, CloudStatus.FAST, 8, 2, ParticleStatus.DECREASED),
        AGGRESSIVE("Agressif", 6, 5, 0.50D, CloudStatus.OFF, 4, 0, ParticleStatus.MINIMAL),
        EXTREME("Extreme", 4, 5, 0.35D, CloudStatus.OFF, 0, 0, ParticleStatus.MINIMAL);

        private final String label;
        private final int renderDistance;
        private final int simulationDistance;
        private final double entityScale;
        private final CloudStatus cloudStatus;
        private final int cloudRange;
        private final int weatherRadius;
        private final ParticleStatus particles;

        BoostPreset(String label, int renderDistance, int simulationDistance, double entityScale, CloudStatus cloudStatus, int cloudRange, int weatherRadius, ParticleStatus particles) {
            this.label = label;
            this.renderDistance = renderDistance;
            this.simulationDistance = simulationDistance;
            this.entityScale = entityScale;
            this.cloudStatus = cloudStatus;
            this.cloudRange = cloudRange;
            this.weatherRadius = weatherRadius;
            this.particles = particles;
        }

        public String getLabel() {
            return label;
        }
    }

    private record Snapshot(
        GraphicsPreset graphicsPreset,
        int renderDistance,
        int simulationDistance,
        double entityDistanceScaling,
        int framerateLimit,
        CloudStatus cloudStatus,
        int cloudRange,
        int weatherRadius,
        boolean cutoutLeaves,
        boolean vignette,
        boolean improvedTransparency,
        boolean ambientOcclusion,
        double chunkSectionFadeInTime,
        int mipmapLevels,
        int biomeBlendRadius,
        boolean enableVsync,
        boolean entityShadows,
        boolean bobView,
        ParticleStatus particles,
        int menuBackgroundBlurriness
    ) {
        static Snapshot capture(Options options) {
            return new Snapshot(
                options.graphicsPreset().get(),
                options.renderDistance().get(),
                options.simulationDistance().get(),
                options.entityDistanceScaling().get(),
                options.framerateLimit().get(),
                options.cloudStatus().get(),
                options.cloudRange().get(),
                options.weatherRadius().get(),
                options.cutoutLeaves().get(),
                options.vignette().get(),
                options.improvedTransparency().get(),
                options.ambientOcclusion().get(),
                options.chunkSectionFadeInTime().get(),
                options.mipmapLevels().get(),
                options.biomeBlendRadius().get(),
                options.enableVsync().get(),
                options.entityShadows().get(),
                options.bobView().get(),
                options.particles().get(),
                options.menuBackgroundBlurriness().get()
            );
        }

        void restore(Options options) {
            options.graphicsPreset().set(graphicsPreset);
            options.renderDistance().set(renderDistance);
            options.simulationDistance().set(simulationDistance);
            options.entityDistanceScaling().set(entityDistanceScaling);
            options.framerateLimit().set(framerateLimit);
            options.cloudStatus().set(cloudStatus);
            options.cloudRange().set(cloudRange);
            options.weatherRadius().set(weatherRadius);
            options.cutoutLeaves().set(cutoutLeaves);
            options.vignette().set(vignette);
            options.improvedTransparency().set(improvedTransparency);
            options.ambientOcclusion().set(ambientOcclusion);
            options.chunkSectionFadeInTime().set(chunkSectionFadeInTime);
            options.mipmapLevels().set(mipmapLevels <= 0 ? DEFAULT_TEXTURE_MIPMAP_LEVELS : mipmapLevels);
            options.biomeBlendRadius().set(biomeBlendRadius);
            options.enableVsync().set(enableVsync);
            options.entityShadows().set(entityShadows);
            options.bobView().set(bobView);
            options.particles().set(particles);
            options.menuBackgroundBlurriness().set(menuBackgroundBlurriness);
            options.save();
        }

        JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("graphicsPreset", graphicsPreset.name());
            json.addProperty("renderDistance", renderDistance);
            json.addProperty("simulationDistance", simulationDistance);
            json.addProperty("entityDistanceScaling", entityDistanceScaling);
            json.addProperty("framerateLimit", framerateLimit);
            json.addProperty("cloudStatus", cloudStatus.name());
            json.addProperty("cloudRange", cloudRange);
            json.addProperty("weatherRadius", weatherRadius);
            json.addProperty("cutoutLeaves", cutoutLeaves);
            json.addProperty("vignette", vignette);
            json.addProperty("improvedTransparency", improvedTransparency);
            json.addProperty("ambientOcclusion", ambientOcclusion);
            json.addProperty("chunkSectionFadeInTime", chunkSectionFadeInTime);
            json.addProperty("mipmapLevels", mipmapLevels);
            json.addProperty("biomeBlendRadius", biomeBlendRadius);
            json.addProperty("enableVsync", enableVsync);
            json.addProperty("entityShadows", entityShadows);
            json.addProperty("bobView", bobView);
            json.addProperty("particles", particles.name());
            json.addProperty("menuBackgroundBlurriness", menuBackgroundBlurriness);
            return json;
        }

        static Snapshot fromJson(JsonObject json) {
            if (json == null) {
                return null;
            }

            return new Snapshot(
                readEnum(json, "graphicsPreset", GraphicsPreset.FANCY),
                readInt(json, "renderDistance", 12),
                readInt(json, "simulationDistance", 8),
                readDouble(json, "entityDistanceScaling", 1.0D),
                readInt(json, "framerateLimit", 120),
                readEnum(json, "cloudStatus", CloudStatus.FANCY),
                readInt(json, "cloudRange", 12),
                readInt(json, "weatherRadius", 0),
                readBoolean(json, "cutoutLeaves", true),
                readBoolean(json, "vignette", false),
                readBoolean(json, "improvedTransparency", true),
                readBoolean(json, "ambientOcclusion", true),
                readDouble(json, "chunkSectionFadeInTime", 0.2D),
                readInt(json, "mipmapLevels", DEFAULT_TEXTURE_MIPMAP_LEVELS),
                readInt(json, "biomeBlendRadius", 2),
                readBoolean(json, "enableVsync", true),
                readBoolean(json, "entityShadows", true),
                readBoolean(json, "bobView", true),
                readEnum(json, "particles", ParticleStatus.ALL),
                readInt(json, "menuBackgroundBlurriness", 2)
            );
        }

        private static int readInt(JsonObject json, String key, int fallback) {
            return json.has(key) ? json.get(key).getAsInt() : fallback;
        }

        private static double readDouble(JsonObject json, String key, double fallback) {
            return json.has(key) ? json.get(key).getAsDouble() : fallback;
        }

        private static boolean readBoolean(JsonObject json, String key, boolean fallback) {
            return json.has(key) ? json.get(key).getAsBoolean() : fallback;
        }

        private static <E extends Enum<E>> E readEnum(JsonObject json, String key, E fallback) {
            if (!json.has(key)) {
                return fallback;
            }

            try {
                return Enum.valueOf(fallback.getDeclaringClass(), json.get(key).getAsString());
            } catch (IllegalArgumentException ignored) {
                return fallback;
            }
        }
    }
}
