package net.elysiastudios.client.module;

import net.elysiastudios.client.module.impl.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ModuleManager {
    private static final ModuleManager INSTANCE = new ModuleManager();
    private final List<Module> modules = new ArrayList<>();

    private ModuleManager() {
        register(new ClickGUIModule());

        register(new TargetHUD());
        register(new AttackIndicator());
        register(new WTapHelper());
        register(new SmartFightModule());
        register(new ShieldIndicator());
        register(new Hitboxes());
        register(new HealthAlert());

        register(new Fullbright());
        register(new TimeChanger());
        register(new NoFog());
        register(new AntiBlind());
        register(new Freelook());
        register(new NoHurtCam());
        register(new LowFire());
        register(new NoWeather());
        register(new ThreatArrow());

        register(new AutoSprint());
        register(new Parkour());
        register(new Sneak());

        register(new FPSBoost());
        register(new ParticleLimiter());
        register(new EntityCullingLite());
        register(new MemoryOptimizer());

        register(new FPSModule());
        register(new PingModule());
        register(new CoordinatesModule());
        register(new DirectionModule());
        register(new BiomeModule());
        register(new KeystrokesModule());
        register(new ToggleSprintModule());
        register(new ArmorStatusModule());
        register(new CPSModule());
        register(new ReachDisplayModule());
        register(new ComboDisplayModule());
        register(new TimeModule());
        register(new SpeedMeterModule());
        register(new PotionEffectsModule());
        register(new SessionStats());
        register(new ServerIPModule());
        register(new MemoryUsage());
        register(new BuildGuideModule());
        register(new HealthStatusModule());
        register(new FoodStatusModule());
        register(new XPProgressModule());
        register(new LightLevelModule());
        register(new LookingAtModule());
        register(new ChunkInfoModule());
        register(new RotationModule());
        register(new NetherCoordsModule());
        register(new WorldDayModule());
        register(new ClockSecondsModule());
        register(new DateModule());
        register(new InventorySlotsModule());
        register(new HeldItemModule());
        register(new ArmorValueModule());
        register(new DurabilityAlertModule());
        register(new ArrowCounterModule());
        register(new TotemCounterModule());
        register(new PlayerCountModule());
        register(new ServerBrandModule());
        register(new WeatherStatusModule());
        register(new MoonPhaseModule());
        register(new WindowInfoModule());
        register(new AirStatusModule());
        register(new FpsAverageModule());
        register(new AltitudeModule());
        register(new VelocityVectorModule());
        register(new VerticalSpeedModule());
        register(new DistanceTravelledModule());
        register(new DeathCounterModule());
        register(new LastDeathModule());
        register(new TargetDistanceModule());
        register(new FluidStatusModule());
        register(new MountStatusModule());
        register(new MountSpeedModule());
        register(new FrameTimeModule());
        register(new MemoryPercentModule());
        register(new JavaInfoModule());
        register(new GameModeModule());
        register(new DimensionModule());
        register(new WorldTimeModule());
        register(new HeldDurabilityModule());
        register(new OffhandItemModule());
        register(new FoodWarningModule());
        register(new ArmorWarningModule());
        register(new SprintStatusModule());
        register(new SneakStatusModule());
        register(new HotbarSlotModule());

        register(new AutoRespawn());
        register(new CopyCoordinatesModule());
        register(new CopyNetherCoordinatesModule());
        register(new CopyServerAddressModule());
        register(new ClearChatModule());
        register(new CopyLookingBlockModule());
        register(new CopyDimensionModule());
        register(new CopyPlayerStatsModule());

        register(new ReloadResourcesModule());
        register(new OpenGameFolderModule());
        register(new OpenScreenshotsFolderModule());
        register(new OpenModsFolderModule());
        register(new OpenConfigFolderModule());
        register(new OpenResourcePacksFolderModule());
        register(new OpenLogsFolderModule());
        register(new PanicDisableModule());
    }

    public static ModuleManager getInstance() {
        return INSTANCE;
    }

    public void register(Module module) {
        modules.add(module);
    }

    public List<Module> getModules() {
        return modules;
    }

    public List<Module> getModulesByCategory(Category category) {
        return modules.stream().filter(module -> module.getCategory() == category).collect(Collectors.toList());
    }

    public <T extends Module> T getModule(Class<T> moduleClass) {
        for (Module module : modules) {
            if (moduleClass.isInstance(module)) {
                return moduleClass.cast(module);
            }
        }
        return null;
    }

    public boolean isModuleEnabled(Class<? extends Module> moduleClass) {
        Module module = getModule(moduleClass);
        return module != null && module.isEnabled();
    }

    public void onTick() {
        for (Module module : modules) {
            if (module.isEnabled()) {
                module.onTick();
            }
        }
    }
}
