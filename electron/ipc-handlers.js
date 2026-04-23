const { ipcMain, dialog } = require('electron');
const { Launch, Microsoft } = require('minecraft-java-core');
const fs = require('fs');
const path = require('path');
const os = require('os');

let mainWindow = null;
let launcher = null;
let gameProcess = null;

// Paths
function getDataPath() {
    const appData = process.env.APPDATA || path.join(os.homedir(), 'AppData', 'Roaming');
    const dir = path.join(appData, '.elysia-launcher');
    if (!fs.existsSync(dir)) fs.mkdirSync(dir, { recursive: true });
    return dir;
}

function getAccountPath() {
    return path.join(getDataPath(), 'accounts.json');
}

function getSettingsPath() {
    return path.join(getDataPath(), 'settings.json');
}

function getMinecraftPath() {
    return path.join(getDataPath(), 'minecraft');
}

// Account management
function loadAccount() {
    try {
        if (fs.existsSync(getAccountPath())) {
            return JSON.parse(fs.readFileSync(getAccountPath(), 'utf8'));
        }
    } catch (e) {
        console.error('Failed to load account:', e);
    }
    return null;
}

function saveAccount(account) {
    fs.writeFileSync(getAccountPath(), JSON.stringify(account, null, 4));
}

function deleteAccount() {
    if (fs.existsSync(getAccountPath())) {
        fs.unlinkSync(getAccountPath());
    }
}

// Settings management
function loadSettings() {
    try {
        if (fs.existsSync(getSettingsPath())) {
            return JSON.parse(fs.readFileSync(getSettingsPath(), 'utf8'));
        }
    } catch (e) {
        console.error('Failed to load settings:', e);
    }
    return getDefaultSettings();
}

function getDefaultSettings() {
    const totalMem = Math.floor(os.totalmem() / (1024 * 1024 * 1024));
    const maxRam = Math.min(Math.max(Math.floor(totalMem / 2), 2), 16);
    return {
        version: 'latest_release',
        loader: {
            type: 'vanilla',
            build: 'latest',
            enable: false
        },
        memory: {
            min: 2,
            max: maxRam
        },
        java: {
            path: null,
            version: null,
            type: 'jre'
        },
        screen: {
            width: null,
            height: null,
            fullscreen: false
        },
        closeOnLaunch: false,
        jvmArgs: '',
        gameArgs: ''
    };
}

function saveSettings(settings) {
    fs.writeFileSync(getSettingsPath(), JSON.stringify(settings, null, 4));
}

// Setup all IPC handlers
function setupIpcHandlers(win) {
    mainWindow = win;

    // ─── Authentication ───────────────────────────────────────

    ipcMain.handle('auth:microsoft', async () => {
        try {
            // Using default Client ID because custom Azure IDs often fail to verify Minecraft ownership
            const msAuth = new Microsoft();

            const account = await msAuth.getAuth();
            if (account && !account.error) {
                account.type = 'microsoft';
                saveAccount(account);
                return { success: true, account };
            }
            return { success: false, error: account?.error || 'Authentication failed' };
        } catch (error) {
            console.error('Microsoft auth error:', error);
            return { success: false, error: error.message };
        }
    });

    ipcMain.handle('auth:offline', async (_, username) => {
        if (!username || username.trim().length < 3 || username.trim().length > 16) {
            return { success: false, error: 'Le pseudo doit contenir entre 3 et 16 caractères' };
        }
        // Validate username characters
        if (!/^[a-zA-Z0-9_]+$/.test(username.trim())) {
            return { success: false, error: 'Le pseudo ne peut contenir que des lettres, chiffres et underscores' };
        }
        const account = {
            access_token: '0',
            client_token: '0',
            uuid: '00000000-0000-0000-0000-000000000000',
            name: username.trim(),
            user_properties: '{}',
            meta: {
                type: 'offline',
                offline: true
            },
            type: 'offline'
        };
        saveAccount(account);
        return { success: true, account };
    });

    ipcMain.handle('auth:refresh', async () => {
        const account = loadAccount();
        if (!account) return { success: false, error: 'No account saved' };

        if (account.type === 'offline') {
            return { success: true, account };
        }

        try {
            if (account.refresh_token) {
                const msAuth = new Microsoft();
                const refreshed = await msAuth.refresh(account);
                if (refreshed && !refreshed.error) {
                    refreshed.type = 'microsoft';
                    saveAccount(refreshed);
                    return { success: true, account: refreshed };
                }
            }
            return { success: false, error: 'Token expired, please re-authenticate' };
        } catch (error) {
            return { success: false, error: error.message };
        }
    });

    ipcMain.handle('auth:logout', async () => {
        deleteAccount();
        return { success: true };
    });

    ipcMain.handle('auth:getAccount', async () => {
        const account = loadAccount();
        if (!account) return { success: false };
        return { success: true, account };
    });

    // ─── Launch ───────────────────────────────────────────────

    ipcMain.handle('launch:start', async (_, options) => {
        try {
            const account = loadAccount();
            if (!account) {
                return { success: false, error: 'Veuillez vous connecter d\'abord' };
            }

            const settings = loadSettings();
            launcher = new Launch();

            const mcPath = getMinecraftPath();
            if (!fs.existsSync(mcPath)) fs.mkdirSync(mcPath, { recursive: true });

            const loaderConfig = {
                type: null,
                build: 'latest',
                enable: false
            };

            if (settings.loader && settings.loader.type && settings.loader.type !== 'vanilla') {
                loaderConfig.type = settings.loader.type;
                loaderConfig.build = settings.loader.build || 'latest';
                loaderConfig.enable = true;
            }

            const launchOpts = {
                path: mcPath,
                authenticator: account,
                version: settings.version || 'latest_release',
                instance: 'ElysiaMC',
                detached: true,
                downloadFileMultiple: 10,
                loader: loaderConfig,
                memory: {
                    min: `${settings.memory?.min || 2}G`,
                    max: `${settings.memory?.max || 4}G`
                },
                java: {
                    path: settings.java?.path || null,
                    type: settings.java?.type || 'jre'
                },
                screen: {
                    width: settings.screen?.width || null,
                    height: settings.screen?.height || null,
                    fullscreen: settings.screen?.fullscreen || false
                },
                JVM_ARGS: settings.jvmArgs ? settings.jvmArgs.split(' ').filter(Boolean) : [],
                GAME_ARGS: settings.gameArgs ? settings.gameArgs.split(' ').filter(Boolean) : [],
                ignored: ['config', 'logs', 'resourcepacks', 'options.txt', 'screenshots', 'shaderpacks']
            };

            // Events
            launcher.on('progress', (progress, size, type) => {
                mainWindow?.webContents.send('launch:progress', {
                    progress,
                    size,
                    type,
                    percent: size > 0 ? ((progress / size) * 100).toFixed(1) : 0
                });
            });

            launcher.on('speed', (speed) => {
                mainWindow?.webContents.send('launch:speed', speed);
            });

            launcher.on('estimated', (time) => {
                mainWindow?.webContents.send('launch:estimated', time);
            });

            launcher.on('data', (line) => {
                mainWindow?.webContents.send('launch:data', line);
            });

            launcher.on('patch', (patch) => {
                mainWindow?.webContents.send('launch:patch', patch);
            });

            launcher.on('close', (code) => {
                mainWindow?.webContents.send('launch:close', code);
                gameProcess = null;
            });

            launcher.on('error', (err) => {
                mainWindow?.webContents.send('launch:error', err?.message || err);
            });

            await launcher.Launch(launchOpts);

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
            gameProcess.kill();
            gameProcess = null;
        }
        return { success: true };
    });

    // ─── Settings ─────────────────────────────────────────────

    ipcMain.handle('settings:get', async () => {
        return loadSettings();
    });

    ipcMain.handle('settings:save', async (_, settings) => {
        saveSettings(settings);
        return { success: true };
    });

    ipcMain.handle('settings:selectJavaPath', async () => {
        const result = await dialog.showOpenDialog(mainWindow, {
            properties: ['openFile'],
            filters: [
                { name: 'Java', extensions: ['exe'] },
                { name: 'All Files', extensions: ['*'] }
            ]
        });
        if (!result.canceled && result.filePaths.length > 0) {
            return { success: true, path: result.filePaths[0] };
        }
        return { success: false };
    });

    ipcMain.handle('settings:getMemoryInfo', async () => {
        const totalMem = Math.floor(os.totalmem() / (1024 * 1024 * 1024));
        return { totalMemGB: totalMem };
    });

    ipcMain.handle('settings:getVersions', async () => {
        try {
            const res = await fetch('https://launchermeta.mojang.com/mc/game/version_manifest.json');
            if (!res.ok) throw new Error('Failed to fetch versions');
            const data = await res.json();
            
            const versions = ['latest_release', 'latest_snapshot'];
            if (data && data.versions) {
                data.versions.forEach(v => {
                    versions.push(v.id);
                });
            }
            return versions;
        } catch (error) {
            console.error('Failed to get versions:', error);
            // Fallback to common versions
            return [
                'latest_release', 'latest_snapshot',
                '1.21.4', '1.21.3', '1.21.2', '1.21.1', '1.21',
                '1.20.6', '1.20.4', '1.20.3', '1.20.2', '1.20.1', '1.20',
                '1.19.4', '1.19.3', '1.19.2', '1.19.1', '1.19',
                '1.18.2', '1.18.1', '1.18',
                '1.17.1', '1.17',
                '1.16.5', '1.16.4', '1.16.3', '1.16.2', '1.16.1',
                '1.15.2', '1.14.4', '1.13.2', '1.12.2',
                '1.11.2', '1.10.2', '1.9.4', '1.8.9', '1.7.10'
            ];
        }
    });

    // ─── Mods ─────────────────────────────────────────────────

    ipcMain.handle('mods:search', async (_, { query, version, loader }) => {
        try {
            const facets = [];
            if (loader && loader !== 'vanilla') facets.push([`categories:${loader}`]);
            if (version && version !== 'latest_release' && version !== 'latest_snapshot') {
                facets.push([`versions:${version}`]);
            }

            let url = `https://api.modrinth.com/v2/search?query=${encodeURIComponent(query)}&limit=20`;
            if (facets.length > 0) {
                url += `&facets=${encodeURIComponent(JSON.stringify(facets))}`;
            }

            const res = await fetch(url);
            if (!res.ok) throw new Error(`HTTP error ${res.status}`);
            return { success: true, data: await res.json() };
        } catch (error) {
            console.error('Mods search error:', error);
            return { success: false, error: error.message };
        }
    });

    ipcMain.handle('mods:install', async (_, { projectId, version, loader }) => {
        try {
            if (!version || version === 'latest_release') {
                return { success: false, error: 'Veuillez sélectionner une version spécifique pour télécharger des mods.' };
            }
            if (!loader || loader === 'vanilla') {
                return { success: false, error: 'Veuillez sélectionner un Mod Loader (Fabric, Forge, etc.) dans les paramètres.' };
            }

            const url = `https://api.modrinth.com/v2/project/${projectId}/version?loaders=["${loader}"]&game_versions=["${version}"]`;
            const res = await fetch(url);
            if (!res.ok) throw new Error(`HTTP error ${res.status}`);
            
            const versions = await res.json();
            if (!versions || versions.length === 0) {
                return { success: false, error: 'Aucune version compatible trouvée pour ce mod.' };
            }

            const latest = versions[0];
            const primaryFile = latest.files.find(f => f.primary) || latest.files[0];
            if (!primaryFile) {
                return { success: false, error: 'Fichier de mod introuvable.' };
            }

            const mcPath = getMinecraftPath();
            const modsDir = path.join(mcPath, 'mods');
            if (!fs.existsSync(modsDir)) fs.mkdirSync(modsDir, { recursive: true });

            const filePath = path.join(modsDir, primaryFile.filename);
            
            // Download
            mainWindow?.webContents.send('launch:progress', {
                progress: 0,
                size: primaryFile.size,
                type: primaryFile.filename,
                percent: 0
            });

            const downloadRes = await fetch(primaryFile.url);
            if (!downloadRes.ok) throw new Error(`Download failed: ${downloadRes.status}`);
            
            const arrayBuffer = await downloadRes.arrayBuffer();
            const buffer = Buffer.from(arrayBuffer);
            fs.writeFileSync(filePath, buffer);

            mainWindow?.webContents.send('launch:progress', {
                progress: primaryFile.size,
                size: primaryFile.size,
                type: primaryFile.filename,
                percent: 100
            });

            return { success: true };
        } catch (error) {
            console.error('Mod install error:', error);
            return { success: false, error: error.message };
        }
    });

    ipcMain.handle('mods:list', async () => {
        try {
            const mcPath = getMinecraftPath();
            const modsDir = path.join(mcPath, 'mods');
            if (!fs.existsSync(modsDir)) return { success: true, mods: [] };

            const files = fs.readdirSync(modsDir).filter(f => f.endsWith('.jar'));
            return { success: true, mods: files };
        } catch (error) {
            return { success: false, error: error.message };
        }
    });

    ipcMain.handle('mods:delete', async (_, filename) => {
        try {
            const mcPath = getMinecraftPath();
            const filePath = path.join(mcPath, 'mods', filename);
            if (fs.existsSync(filePath)) {
                fs.unlinkSync(filePath);
            }
            return { success: true };
        } catch (error) {
            return { success: false, error: error.message };
        }
    });
}

module.exports = { setupIpcHandlers };
