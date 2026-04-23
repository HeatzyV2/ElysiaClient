const { ipcMain } = require('electron');
const { Launch, Microsoft } = require('minecraft-java-core');
const fs = require('fs');
const path = require('path');
const os = require('os');
const { execFile } = require('child_process');
const { store } = require('./store');
const { addPlaytime, recordSessionStart } = require('./stats');

let launcher = null;
let gameProcess = null;
let activeGame = null;
let startTime = 0;
let sessionSavedPlaytime = 0;
let playtimeSyncInterval = null;
let statusMonitorInterval = null;

const ELYSIA_LAUNCH_FLAG = '-Delysia.launcher=true';
const PLAYTIME_SYNC_INTERVAL_MS = 15000;

const LOW_END_JVM_ARGS = [
    '-XX:+UseG1GC',
    '-XX:+ParallelRefProcEnabled',
    '-XX:MaxGCPauseMillis=200',
    '-XX:+DisableExplicitGC',
    '-XX:+PerfDisableSharedMem',
    '-XX:G1HeapRegionSize=4M',
    '-Dsun.java2d.opengl=false'
];

const LOW_END_OPTIONS = {
    graphicsMode: 'fast',
    renderDistance: '4',
    simulationDistance: '5',
    entityDistanceScaling: '0.5',
    particles: '2',
    clouds: 'false',
    maxFps: '60',
    mipmapLevels: '2',
    biomeBlendRadius: '0',
    ao: 'false',
    enableVsync: 'false',
    bobView: 'false',
    fovEffectScale: '0.0',
    screenEffectScale: '0.5'
};

function getDataPath() {
    const appData = process.env.APPDATA || path.join(os.homedir(), 'AppData', 'Roaming');
    const dir = path.join(appData, '.elysia-launcher');
    if (!fs.existsSync(dir)) fs.mkdirSync(dir, { recursive: true });
    return dir;
}

function getMinecraftPath() {
    return path.join(getDataPath(), 'minecraft');
}

function getInstancePath(settings) {
    if (!settings) return getMinecraftPath();
    const version = settings.version || '1.21.4';
    let loader = settings.loader?.type && settings.loader.type !== 'vanilla' ? settings.loader.type : 'vanilla';
    
    // ElysiaClient shares the same instance folder as Fabric for easier mod management
    if (loader === 'elysiaclient') loader = 'fabric';
    
    return path.join(getMinecraftPath(), 'instances', `${version}-${loader}`);
}

function splitArgs(args) {
    return String(args || '').split(/\s+/).map(arg => arg.trim()).filter(Boolean);
}

function mergeArgs(baseArgs, extraArgs) {
    const merged = [...baseArgs];
    const keys = new Set(baseArgs.map(arg => arg.split('=')[0]));

    for (const arg of extraArgs) {
        const key = arg.split('=')[0];
        if (!keys.has(key)) {
            keys.add(key);
            merged.push(arg);
        }
    }

    return merged;
}

function getLowEndMemory(settings) {
    const configuredMax = Number(settings.memory?.max) || 4;
    const totalRam = Math.floor(os.totalmem() / 1024 / 1024 / 1024);
    const safeMax = totalRam <= 4 ? 2 : 3;
    const max = Math.max(1, Math.min(configuredMax, safeMax));
    return {
        min: 1,
        max
    };
}

function upsertMinecraftOptions(instancePath, optionsToApply) {
    if (!fs.existsSync(instancePath)) fs.mkdirSync(instancePath, { recursive: true });

    const optionsPath = path.join(instancePath, 'options.txt');
    const lines = fs.existsSync(optionsPath)
        ? fs.readFileSync(optionsPath, 'utf8').split(/\r?\n/).filter(line => line.length > 0)
        : [];

    const used = new Set();
    const updated = lines.map(line => {
        const separator = line.indexOf(':');
        if (separator <= 0) return line;

        const key = line.slice(0, separator);
        if (Object.prototype.hasOwnProperty.call(optionsToApply, key)) {
            used.add(key);
            return `${key}:${optionsToApply[key]}`;
        }

        return line;
    });

    for (const [key, value] of Object.entries(optionsToApply)) {
        if (!used.has(key)) updated.push(`${key}:${value}`);
    }

    fs.writeFileSync(optionsPath, `${updated.join(os.EOL)}${os.EOL}`, 'utf8');
}

function repairLegacyPixelatedOptions(instancePath, mainWindow) {
    try {
        const optionsPath = path.join(instancePath, 'options.txt');
        if (!fs.existsSync(optionsPath)) return;

        let changed = false;
        const lines = fs.readFileSync(optionsPath, 'utf8').split(/\r?\n/).filter(line => line.length > 0);
        const updated = lines.map(line => {
            const separator = line.indexOf(':');
            if (separator <= 0) return line;

            const key = line.slice(0, separator);
            const value = line.slice(separator + 1);
            if (key === 'mipmapLevels' && Number(value) <= 0) {
                changed = true;
                return 'mipmapLevels:4';
            }

            return line;
        });

        if (changed) {
            fs.writeFileSync(optionsPath, `${updated.join(os.EOL)}${os.EOL}`, 'utf8');
            mainWindow?.webContents.send('launch:data', '[Elysia] Qualite textures restauree: mipmaps remises a 4.');
        }
    } catch (error) {
        console.error('[Visual Repair] Failed to repair options:', error);
        mainWindow?.webContents.send('launch:data', `[Elysia] Impossible de reparer options.txt (${error.message}).`);
    }
}

function applyLowEndMinecraftOptions(instancePath, mainWindow) {
    try {
        upsertMinecraftOptions(instancePath, LOW_END_OPTIONS);
        mainWindow?.webContents.send('launch:data', '[Elysia] Mode PC faible: options Minecraft optimisees (4 chunks, graphismes rapides, particules minimales, mipmaps lisibles).');
    } catch (error) {
        console.error('[Low End Mode] Failed to apply options:', error);
        mainWindow?.webContents.send('launch:data', `[Elysia] Mode PC faible: impossible d'ecrire options.txt (${error.message}).`);
    }
}

function getRuntimeDir() {
    const dir = path.join(getDataPath(), 'runtime');
    if (!fs.existsSync(dir)) fs.mkdirSync(dir, { recursive: true });
    return dir;
}

function getActiveGamePath() {
    return path.join(getRuntimeDir(), 'active-game.json');
}

function getPresencePath() {
    return path.join(getMinecraftPath(), 'runtime', 'presence.json');
}

function clearPresenceFile() {
    try {
        const presencePath = getPresencePath();
        if (fs.existsSync(presencePath)) fs.unlinkSync(presencePath);
    } catch (error) {
        // Best effort cleanup only.
    }
}

function readPresenceFile() {
    try {
        const presencePath = getPresencePath();
        if (!fs.existsSync(presencePath)) return null;
        return JSON.parse(fs.readFileSync(presencePath, 'utf8'));
    } catch (error) {
        return undefined;
    }
}

function createLaunchId() {
    return `${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 10)}`;
}

function sanitizePresenceText(value) {
    return String(value || '').replace(/\s+/g, ' ').trim();
}

function formatServerAddress(host, port) {
    const cleanHost = sanitizePresenceText(host);
    const cleanPort = Number(port);
    if (!cleanHost) return null;
    if (cleanPort > 0 && cleanPort !== 25565) {
        return `${cleanHost}:${cleanPort}`;
    }
    return cleanHost;
}

function normalizePresenceMode(presence) {
    const mode = sanitizePresenceText(presence?.serverType || presence?.mode).toLowerCase();
    if (mode === 'singleplayer' || mode === 'multiplayer' || mode === 'menu') {
        return mode;
    }

    const server = sanitizePresenceText(presence?.server).toLowerCase();
    if (server === 'solo') {
        return 'singleplayer';
    }
    if (server) {
        return 'multiplayer';
    }
    return 'menu';
}

function resolvePresenceServerDisplay(presence, mode) {
    if (mode === 'singleplayer') return 'Solo';
    if (mode === 'menu') return 'Menus';

    const candidates = [
        sanitizePresenceText(presence?.serverAddress),
        sanitizePresenceText(presence?.server),
        sanitizePresenceText(presence?.serverName)
    ].filter(Boolean);

    return candidates[0] || null;
}

function normalizeCommandText(value) {
    return String(value || '').replace(/\\/g, '/').toLowerCase();
}

function commandContainsPath(commandLine, targetPath) {
    if (!targetPath) return true;
    return normalizeCommandText(commandLine).includes(normalizeCommandText(targetPath));
}

function writeActiveGameMarker(game) {
    fs.writeFileSync(getActiveGamePath(), JSON.stringify(game, null, 2), 'utf8');
}

function readActiveGameMarker() {
    try {
        const filePath = getActiveGamePath();
        if (!fs.existsSync(filePath)) return null;
        return JSON.parse(fs.readFileSync(filePath, 'utf8'));
    } catch (error) {
        return null;
    }
}

function clearActiveGameMarker() {
    try {
        const filePath = getActiveGamePath();
        if (fs.existsSync(filePath)) fs.unlinkSync(filePath);
    } catch (error) {
        // Best effort cleanup only.
    }
}

function resolveProcessId(processInfo) {
    return processInfo?.pid
        || processInfo?.process?.pid
        || processInfo?.child?.pid
        || processInfo?.minecraft?.pid
        || null;
}

function queryJavaProcesses() {
    return new Promise((resolve) => {
        if (process.platform !== 'win32') {
            execFile('ps', ['-axo', 'pid=,command='], { maxBuffer: 4 * 1024 * 1024 }, (error, stdout) => {
                if (error) return resolve([]);
                const processes = stdout.split(/\r?\n/)
                    .map(line => line.trim())
                    .filter(line => /\bjavaw?\b/i.test(line))
                    .map(line => {
                        const match = line.match(/^(\d+)\s+(.*)$/);
                        return match ? { pid: Number(match[1]), commandLine: match[2] } : null;
                    })
                    .filter(Boolean);
                resolve(processes);
            });
            return;
        }

        const script = [
            "Get-CimInstance Win32_Process -Filter \"Name = 'javaw.exe' OR Name = 'java.exe'\"",
            "| Select-Object ProcessId, CommandLine",
            "| ConvertTo-Json -Compress"
        ].join(' ');

        execFile('powershell.exe', ['-NoProfile', '-Command', script], { windowsHide: true, maxBuffer: 8 * 1024 * 1024 }, (error, stdout) => {
            if (error || !stdout.trim()) return resolve([]);
            try {
                const json = JSON.parse(stdout.trim());
                const entries = Array.isArray(json) ? json : [json];
                resolve(entries.map(entry => ({
                    pid: Number(entry.ProcessId),
                    commandLine: entry.CommandLine || ''
                })));
            } catch (parseError) {
                resolve([]);
            }
        });
    });
}

function isElysiaGameProcess(processEntry, expected = {}) {
    const commandLine = processEntry?.commandLine || '';
    const normalized = normalizeCommandText(commandLine);
    if (!normalized.includes('net.minecraft') && !normalized.includes('knotclient')) {
        return false;
    }

    const hasMarker = normalized.includes(ELYSIA_LAUNCH_FLAG.toLowerCase());
    const hasLaunchId = expected.launchId && normalized.includes(`-delysia.launch.id=${expected.launchId}`.toLowerCase());
    const hasElysiaGameDir = normalized.includes('/.elysia-launcher/minecraft') && normalized.includes('--gamedir');
    const matchesInstance = commandContainsPath(commandLine, expected.instancePath);

    if (hasLaunchId) {
        return matchesInstance;
    }

    if (hasMarker) {
        return matchesInstance;
    }

    // Legacy fallback for games launched by older Elysia builds before the JVM marker existed.
    return expected.allowLegacy === true && hasElysiaGameDir && matchesInstance;
}

async function findRunningElysiaGame(expected = {}) {
    const marker = readActiveGameMarker();
    const wanted = {
        ...marker,
        ...expected,
        allowLegacy: expected.allowLegacy ?? true
    };

    const processes = await queryJavaProcesses();
    const running = processes.find(processEntry => isElysiaGameProcess(processEntry, wanted));
    if (!running) {
        return { running: false };
    }

    return {
        running: true,
        pid: running.pid,
        commandLine: running.commandLine,
        launchId: wanted.launchId || marker?.launchId || null,
        instancePath: wanted.instancePath || marker?.instancePath || null,
        version: wanted.version || marker?.version || null,
        loader: wanted.loader || marker?.loader || null,
        username: wanted.username || marker?.username || null
    };
}

function setupLaunchIpc(mainWindow) {
    function buildMenuStatus(overrides = {}) {
        const statusData = {
            status: 'menu',
            version: overrides.version || activeGame?.version || 'Elysia',
            username: overrides.username || activeGame?.username || null
        };

        const serverDisplay = sanitizePresenceText(overrides.serverDisplay || activeGame?.pendingServerDisplay);
        const serverType = sanitizePresenceText(overrides.serverType || (serverDisplay ? 'multiplayer' : ''));

        if (serverType) statusData.serverType = serverType;
        if (serverDisplay) {
            statusData.serverDisplay = serverDisplay;
            statusData.server = serverDisplay;
        }

        return statusData;
    }

    function buildPlayingStatusFromPresence(presence) {
        const mode = normalizePresenceMode(presence);
        const serverDisplay = resolvePresenceServerDisplay(presence, mode);

        return {
            status: 'playing',
            version: activeGame?.version || 'Elysia',
            username: sanitizePresenceText(presence?.username) || activeGame?.username || null,
            serverType: mode === 'singleplayer' ? 'singleplayer' : 'multiplayer',
            serverDisplay: serverDisplay || (mode === 'singleplayer' ? 'Solo' : null),
            serverAddress: sanitizePresenceText(presence?.serverAddress) || null,
            serverName: sanitizePresenceText(presence?.serverName) || null,
            server: serverDisplay || (mode === 'singleplayer' ? 'Solo' : null)
        };
    }

    function syncPlaytime() {
        if (startTime <= 0) {
            return null;
        }

        const elapsed = Date.now() - startTime;
        const delta = elapsed - sessionSavedPlaytime;
        if (delta <= 0) {
            return null;
        }

        sessionSavedPlaytime += delta;
        const stats = addPlaytime(delta);
        if (activeGame) {
            activeGame.lastSyncedAt = Date.now();
            writeActiveGameMarker(activeGame);
        }
        mainWindow?.webContents.send('stats:update', stats);
        return stats;
    }

    function startPlaytimeTracking(statusData) {
        if (playtimeSyncInterval) clearInterval(playtimeSyncInterval);

        startTime = Date.now();
        sessionSavedPlaytime = 0;
        recordSessionStart();
        mainWindow?.webContents.send('stats:update', addPlaytime(0));

        playtimeSyncInterval = setInterval(syncPlaytime, PLAYTIME_SYNC_INTERVAL_MS);
        ipcMain.emit('game:status', null, statusData);
        mainWindow?.webContents.send('launch:status', statusData);
    }

    function resumePlaytimeTracking(game, statusData) {
        if (playtimeSyncInterval) clearInterval(playtimeSyncInterval);

        activeGame = game;
        startTime = Number(game?.lastSyncedAt) || Date.now();
        sessionSavedPlaytime = 0;
        playtimeSyncInterval = setInterval(syncPlaytime, PLAYTIME_SYNC_INTERVAL_MS);
        startStatusMonitor();
        ipcMain.emit('game:status', null, statusData);
        mainWindow?.webContents.send('launch:status', statusData);
    }

    async function finishGame(finalCode = 0, finalCrashReport = null, shouldNotifyRenderer = true) {
        const hadTrackedGame = activeGame || startTime > 0 || playtimeSyncInterval || readActiveGameMarker();
        if (!hadTrackedGame && shouldNotifyRenderer) {
            return;
        }

        if (playtimeSyncInterval) {
            clearInterval(playtimeSyncInterval);
            playtimeSyncInterval = null;
        }
        if (statusMonitorInterval) {
            clearInterval(statusMonitorInterval);
            statusMonitorInterval = null;
        }

        syncPlaytime();
        startTime = 0;
        sessionSavedPlaytime = 0;
        activeGame = null;
        gameProcess = null;
        clearActiveGameMarker();
        clearPresenceFile();

        const idleStatus = { status: 'idle' };
        ipcMain.emit('game:status', null, idleStatus);
        mainWindow?.webContents.send('launch:status', idleStatus);

        if (shouldNotifyRenderer) {
            mainWindow?.webContents.send('launch:close', { code: finalCode, crashReport: finalCrashReport });
        }
    }

    function startStatusMonitor() {
        if (statusMonitorInterval) clearInterval(statusMonitorInterval);

        let pollInFlight = false;
        const pollStatus = async () => {
            if (pollInFlight) return;
            pollInFlight = true;

            try {
                if (!activeGame && !readActiveGameMarker()) {
                    return;
                }

                const result = await findRunningElysiaGame({
                    launchId: activeGame?.launchId,
                    instancePath: activeGame?.instancePath,
                    allowLegacy: !activeGame?.launchId
                });

                if (!result.running) {
                    await finishGame(0, null, true);
                    return;
                }

                const presence = readPresenceFile();
                if (presence === undefined) {
                    return;
                }

                if (presence && presence.status === 'online') {
                    if (activeGame) activeGame.pendingServerDisplay = null;
                    ipcMain.emit('game:status', null, buildPlayingStatusFromPresence(presence));
                    return;
                }

                ipcMain.emit('game:status', null, buildMenuStatus({
                    username: sanitizePresenceText(presence?.username) || activeGame?.username || null
                }));
            } finally {
                pollInFlight = false;
            }
        };

        pollStatus();
        statusMonitorInterval = setInterval(pollStatus, 5000);
    }

    ipcMain.handle('launch:start', async (_, options) => {
        try {
            const accounts = store.get('accounts') || [];
            const activeId = store.get('activeAccountId');
            const account = accounts.find(a => a.id === activeId);
            
            if (!account) {
                return { success: false, error: 'Veuillez vous connecter d\'abord' };
            }

            // --- SESSION REVALIDATION ---
            if (account.type === 'microsoft' && account.refresh_token) {
                try {
                    mainWindow?.webContents.send('launch:data', `[Elysia] Vérification de la session Microsoft...`);
                    const msAuth = new Microsoft();
                    // Some versions of minecraft-java-core might return the same object if still valid, 
                    // or a new one if refreshed.
                    const refreshed = await msAuth.refresh(account);
                    
                    if (refreshed && !refreshed.error) {
                        refreshed.type = 'microsoft';
                        refreshed.id = account.id; // preserve ID
                        
                        // Update in store
                        const updatedAccounts = store.get('accounts') || [];
                        const index = updatedAccounts.findIndex(a => a.id === account.id);
                        if (index >= 0) {
                            updatedAccounts[index] = refreshed;
                            store.set('accounts', updatedAccounts);
                        }
                        
                        // Use the refreshed account for launching
                        account.access_token = refreshed.access_token;
                        account.uuid = refreshed.uuid;
                        account.name = refreshed.name;
                        // update reference for the rest of the function
                        Object.assign(account, refreshed);
                        
                        mainWindow?.webContents.send('launch:data', `[Elysia] Session revalidée avec succès.`);
                    } else {
                        return { success: false, error: `La session Microsoft a expiré. Veuillez vous reconnecter.` };
                    }
                    
                    // Notify renderer that accounts might have been updated (for UI)
                    mainWindow?.webContents.send('accounts:updated');
                } catch (refreshError) {
                    console.error('Failed to refresh Microsoft session:', refreshError);
                    // If refresh fails, we might still try to launch, but it will likely fail.
                    // Better to warn the user.
                    return { success: false, error: 'Impossible de valider votre session Microsoft. Veuillez vous reconnecter.' };
                }
            }
            // ----------------------------

            const settings = store.get('settings') || {};
            launcher = new Launch();

            const mcPath = getMinecraftPath();
            if (!fs.existsSync(mcPath)) fs.mkdirSync(mcPath, { recursive: true });

            const loaderConfig = {
                type: settings.loader?.type?.toLowerCase() || null,
                build: settings.loader?.build || 'latest',
                enable: (settings.loader?.type && settings.loader.type !== 'vanilla')
            };

            const instancePath = getInstancePath(settings);
            if (!fs.existsSync(instancePath)) fs.mkdirSync(instancePath, { recursive: true });

            const existingGame = await findRunningElysiaGame({ allowLegacy: true });
            if (existingGame.running) {
                activeGame = {
                    ...readActiveGameMarker(),
                    ...existingGame,
                    instancePath: existingGame.instancePath || readActiveGameMarker()?.instancePath || null,
                    version: settings.version || 'latest_release',
                    loader: settings.loader?.type || 'vanilla',
                    username: account.name,
                    lastSyncedAt: readActiveGameMarker()?.lastSyncedAt || Date.now()
                };
                writeActiveGameMarker(activeGame);
                resumePlaytimeTracking(activeGame, {
                    status: 'menu',
                    version: activeGame.version,
                    username: activeGame.username,
                    source: 'elysia'
                });
                return { success: true, alreadyRunning: true };
            }

            // --- ELYSIA CLIENT AUTO-INSTALL ---
            if (settings.loader?.type?.toLowerCase() === 'elysiaclient') {
                try {
                    const { installElysiaClientJar, installElysiaFeaturePack } = require('./mods');
                    const clientInstall = await installElysiaClientJar({
                        version: settings.version || 'latest_release',
                        loader: 'elysiaclient',
                        mainWindow
                    });

                    if (!clientInstall.success) {
                        return { success: false, error: clientInstall.error };
                    }

                    await installElysiaFeaturePack({
                        version: settings.version || 'latest_release',
                        loader: 'elysiaclient',
                        mainWindow
                    });
                } catch (e) {
                    console.error('Failed to install Elysia Client:', e);
                    return { success: false, error: `Installation Elysia Client impossible: ${e.message}` };
                }
            }

            // --- ANTI-CHEAT SCAN ---
            const { performAntiCheatScan } = require('./anticheat');
            const acResult = await performAntiCheatScan(instancePath);
            if (!acResult.success) {
                return { success: false, error: acResult.error };
            }
            // -----------------------

            const lowEndMode = !!settings.lowPerfMode;
            const lowEndMemory = lowEndMode ? getLowEndMemory(settings) : null;
            const jvmArgs = lowEndMode
                ? mergeArgs(LOW_END_JVM_ARGS, splitArgs(settings.jvmArgs))
                : splitArgs(settings.jvmArgs);
            const launchId = createLaunchId();

            if (lowEndMode) {
                applyLowEndMinecraftOptions(instancePath, mainWindow);
                mainWindow?.webContents.send('launch:data', `[Elysia] Mode PC faible actif: RAM ${lowEndMemory.min}G-${lowEndMemory.max}G, fenetre 1280x720, telechargements reduits.`);
            } else {
                repairLegacyPixelatedOptions(instancePath, mainWindow);
            }

            const launchOpts = {
                path: mcPath,
                authenticator: account,
                version: settings.version || 'latest_release',
                downloadFileMultiple: lowEndMode ? 6 : 15,
                loader: {
                    type: (settings.loader?.type === 'elysiaclient') ? 'fabric' : (settings.loader?.type || 'vanilla'),
                    build: settings.loader?.build || 'latest',
                    enable: (settings.loader?.type && settings.loader.type !== 'vanilla')
                },
                memory: {
                    min: `${lowEndMode ? lowEndMemory.min : (settings.memory?.min || 2)}G`,
                    max: `${lowEndMode ? lowEndMemory.max : (settings.memory?.max || 4)}G`
                },
                java: {
                    path: settings.java?.path || null,
                    type: settings.java?.type || 'jre'
                },
                screen: {
                    width: lowEndMode ? 1280 : (settings.screen?.width || null),
                    height: lowEndMode ? 720 : (settings.screen?.height || null),
                    fullscreen: lowEndMode ? false : (settings.screen?.fullscreen || false)
                },
                JVM_ARGS: [
                    ...jvmArgs,
                    ELYSIA_LAUNCH_FLAG,
                    `-Delysia.launch.id=${launchId}`,
                    `-Delysia.vip=${!!store.get('discordProfile')}`
                ],
                GAME_ARGS: ['--gameDir', instancePath, ...(settings.gameArgs ? settings.gameArgs.split(' ').filter(Boolean) : [])],
                detached: true, // Allow game to run independently
                ignored: ['config', 'logs', 'options.txt', 'screenshots', 'shaderpacks'],
                overrides: {
                    logj4ConfigurationFile: `file:///${mcPath.replace(/\\/g, '/')}/assets/log_configs/client-1.21.2.xml`
                }
            };

            // Support Quick Join
            let quickJoinDisplay = null;
            if (settings.quickJoin) {
                const [host, portStr] = settings.quickJoin.split(':');
                const port = parseInt(portStr) || 25565;
                launchOpts.server = {
                    ip: host,
                    host: host,
                    port
                };
                quickJoinDisplay = formatServerAddress(host, port);
                settings.quickJoin = null;
                store.set('settings', settings);
            }

            // Debug logs
            mainWindow?.webContents.send('launch:data', `[Elysia] Version: ${launchOpts.version}`);
            mainWindow?.webContents.send('launch:data', `[Elysia] Loader: ${launchOpts.loader.type} (enabled: ${launchOpts.loader.enable})`);
            mainWindow?.webContents.send('launch:data', `[Elysia] Assets: ${mcPath}`);
            mainWindow?.webContents.send('launch:data', `[Elysia] GameDir: ${instancePath}`);
            
            // Verify mods exist
            const modsDir = path.join(instancePath, 'mods');
            if (fs.existsSync(modsDir)) {
                const modFiles = fs.readdirSync(modsDir).filter(f => f.endsWith('.jar'));
                mainWindow?.webContents.send('launch:data', `[Elysia] ${modFiles.length} mod(s) detecte(s)`);
            } else {
                mainWindow?.webContents.send('launch:data', `[Elysia] Aucun dossier mods/ dans ${instancePath}`);
            }

            // Initial state: Downloading/Preparing
            ipcMain.emit('game:status', null, { 
                status: 'downloading',
                version: launchOpts.version, 
                username: account.name 
            });

            // --- LOG BATCHING & PROGRESS THROTTLING ---
            let logBuffer = [];
            let lastLogSend = Date.now();
            let lastProgressPercent = -1;
            let lastProgressTime = 0;

            const flushLogs = () => {
                if (logBuffer.length > 0) {
                    mainWindow?.webContents.send('launch:data', logBuffer.join('\n'));
                    logBuffer = [];
                    lastLogSend = Date.now();
                }
            };

            const logInterval = setInterval(() => {
                if (Date.now() - lastLogSend > 150) flushLogs();
            }, 200);

            // Events
            launcher.on('progress', (progress, size, type) => {
                const percent = size > 0 ? parseFloat(((progress / size) * 100).toFixed(1)) : 0;
                const now = Date.now();
                
                // Only send progress if percent changed significantly or 200ms passed
                if (percent !== lastProgressPercent || now - lastProgressTime > 200) {
                    mainWindow?.webContents.send('launch:progress', {
                        progress,
                        size,
                        type,
                        percent
                    });
                    lastProgressPercent = percent;
                    lastProgressTime = now;
                }
            });

            launcher.on('speed', (speed) => {
                // Throttle speed updates
                if (Math.random() > 0.7) mainWindow?.webContents.send('launch:speed', speed);
            });
            
            launcher.on('estimated', (time) => mainWindow?.webContents.send('launch:estimated', time));
            
            launcher.on('data', (line) => {
                logBuffer.push(line);
                if (logBuffer.length > 50) flushLogs(); // Flush if too many logs
            });
            
            launcher.on('patch', (patch) => mainWindow?.webContents.send('launch:patch', patch));

            launcher.on('close', async (code) => {
                clearInterval(logInterval);
                flushLogs();
                
                let crashReport = null;
                if (code !== 0) {
                    try {
                        const logsPath = path.join(instancePath, 'logs', 'latest.log');
                        if (fs.existsSync(logsPath)) {
                            const logs = fs.readFileSync(logsPath, 'utf8');
                            if (logs.includes('OutOfMemoryError')) {
                                crashReport = "Manque de RAM. Veuillez augmenter la RAM allouée dans les paramètres.";
                            } else if (logs.includes('UnsupportedClassVersionError')) {
                                crashReport = "Version Java incorrecte. Essayez de changer le chemin Java dans les paramètres.";
                            } else if (logs.includes('MissingModsException')) {
                                crashReport = "Des mods requis sont manquants pour rejoindre ce serveur ou lancer ce monde.";
                            } else {
                                crashReport = "Minecraft s'est arrêté de manière inattendue. Code: " + code;
                            }
                        }
                    } catch (e) {
                        // ignore
                    }
                }

                await finishGame(code, crashReport);
            });

            launcher.on('error', (err) => {
                mainWindow?.webContents.send('launch:error', err?.message || err);
            });

            // Java Version Check helper
            const checkJava = async (javaPath) => {
                const { exec } = require('child_process');
                return new Promise((resolve) => {
                    const cmd = javaPath ? `"${javaPath}" -version` : 'java -version';
                    exec(cmd, (error, stdout, stderr) => {
                        const versionOutput = stderr || stdout || '';
                        const match = versionOutput.match(/version "(?:1\.)?(\d+)/);
                        const major = match ? parseInt(match[1]) : 0;
                        resolve(major);
                    });
                });
            };

            let javaVer = await checkJava(settings.java?.path);
            
            if (javaVer < 17 && !settings.java?.path) {
                const commonPaths = [
                    'C:\\Program Files\\Java\\jdk-21\\bin\\java.exe',
                    'C:\\Program Files\\Microsoft\\jdk-21.0.3.9-hotspot\\bin\\java.exe',
                    'C:\\Program Files\\Eclipse Foundation\\jdk-21.0.2.13-hotspot\\bin\\java.exe'
                ];
                for (const p of commonPaths) {
                    if (fs.existsSync(p)) {
                        const v = await checkJava(p);
                        if (v >= 21) {
                            settings.java = { ...settings.java, path: p };
                            javaVer = v;
                            break;
                        }
                    }
                }
            }

            // --- CONTROLLER MODE AUTO-INJECTION ---
            if (settings.controllerMode && launchOpts.loader.type !== 'vanilla') {
                try {
                    const { installControllerMods } = require('./mods');
                    await installControllerMods({
                        version: settings.version || 'latest_release',
                        loader: settings.loader?.type || 'vanilla',
                        mainWindow
                    });
                } catch (e) {
                    console.error('Failed to install Controller Mods:', e);
                }
            }
            
            clearPresenceFile();
            const processInfo = await launcher.Launch(launchOpts);
            gameProcess = processInfo;

            const detectedProcess = await findRunningElysiaGame({ launchId, instancePath, allowLegacy: false });
            activeGame = {
                launchId,
                pid: detectedProcess.pid || resolveProcessId(processInfo),
                instancePath,
                mcPath,
                version: settings.version || 'latest_release',
                loader: settings.loader?.type || 'vanilla',
                username: account.name,
                pendingServerDisplay: quickJoinDisplay,
                startedAt: Date.now(),
                lastSyncedAt: Date.now()
            };
            writeActiveGameMarker(activeGame);
            startStatusMonitor();

            startPlaytimeTracking({
                status: 'menu',
                version: settings.version || 'latest_release',
                username: account.name,
                serverType: quickJoinDisplay ? 'multiplayer' : undefined,
                serverDisplay: quickJoinDisplay,
                source: 'elysia'
            });

            // Notify about packs
            setTimeout(() => {
                mainWindow?.webContents.send('notify', {
                    message: 'N\'oubliez pas d\'activer vos packs de ressources et shaders dans les options du jeu !',
                    type: 'info'
                });
            }, 3000);

            if (settings.gameBooster) {
                try {
                    const exec = require('child_process').exec;
                    exec('powershell.exe -NoProfile -Command "Get-Process -Name java,javaw -ErrorAction SilentlyContinue | ForEach-Object { $_.PriorityClass = \'High\' }"', (err) => {
                        if (!err) console.log('[Game Booster] Priorité Haute appliquée à Java.');
                    });
                } catch (e) {
                    console.error('[Game Booster] Erreur :', e);
                }
            }

            if (settings.closeOnLaunch) {
                mainWindow?.hide();
            }

            return { success: true };
        } catch (error) {
            console.error('Launch error:', error);
            return { success: false, error: error.message };
        }
    });

    ipcMain.handle('launch:stop', async () => {
        if (gameProcess) {
            if (typeof gameProcess.kill === 'function') {
                gameProcess.kill();
            }
            gameProcess = null;
        }
        await finishGame(0, null, true);
        return { success: true };
    });

    ipcMain.handle('launch:openFolder', async () => {
        const settings = store.get('settings') || {};
        const instancePath = getInstancePath(settings);
        if (!fs.existsSync(instancePath)) fs.mkdirSync(instancePath, { recursive: true });
        require('electron').shell.openPath(instancePath);
        return true;
    });

    ipcMain.handle('launch:getInstanceInfo', async () => {
        const settings = store.get('settings') || {};
        const version = settings.version || '1.21.4';
        const loader = settings.loader?.type || 'vanilla';
        const path = getInstancePath(settings);
        return { version, loader, path };
    });

    ipcMain.handle('launch:listInstances', async () => {
        const instancesDir = path.join(getMinecraftPath(), 'instances');
        if (!fs.existsSync(instancesDir)) return [];
        
        const dirs = fs.readdirSync(instancesDir, { withFileTypes: true })
            .filter(dirent => dirent.isDirectory())
            .map(dirent => dirent.name);
            
        return dirs;
    });

    ipcMain.handle('launch:checkStatus', async () => {
        const settings = store.get('settings') || {};
        const marker = readActiveGameMarker();
        const result = await findRunningElysiaGame({
            launchId: marker?.launchId || activeGame?.launchId,
            instancePath: marker?.instancePath || activeGame?.instancePath || null,
            allowLegacy: true
        });

        if (!result.running) {
            if (activeGame || marker) {
                await finishGame(0, null, false);
            }
            return false;
        }

        if (!activeGame) {
            activeGame = {
                ...marker,
                ...result,
                instancePath: result.instancePath || marker?.instancePath || getInstancePath(settings),
                version: result.version || settings.version || 'latest_release',
                loader: result.loader || settings.loader?.type || 'vanilla',
                username: result.username || null,
                lastSyncedAt: marker?.lastSyncedAt || Date.now()
            };
            writeActiveGameMarker(activeGame);
            resumePlaytimeTracking(activeGame, {
                status: 'menu',
                version: activeGame.version,
                username: activeGame.username,
                source: 'elysia'
            });
        }

        return true;
    });

    ipcMain.handle('launch:getStartTime', () => {
        return startTime;
    });
}

module.exports = { setupLaunchIpc, getMinecraftPath, getDataPath, getInstancePath };
