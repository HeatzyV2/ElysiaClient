const DiscordRPC = require('discord-rpc');
const { ipcMain } = require('electron');
const { store } = require('./store');

const clientId = '1495874037472035037';
let rpc;
let currentActivity = null;
let rpcStartTime = Date.now();

let isConnected = false;

function sanitizeText(value) {
    return String(value || '').replace(/\s+/g, ' ').trim();
}

function truncateText(value, max = 120) {
    const text = sanitizeText(value);
    if (text.length <= max) return text;
    return `${text.slice(0, Math.max(0, max - 3)).trim()}...`;
}

function normalizeServerType(vars) {
    const serverType = sanitizeText(vars.serverType).toLowerCase();
    if (serverType === 'singleplayer' || serverType === 'multiplayer') {
        return serverType;
    }

    const serverDisplay = sanitizeText(vars.serverDisplay || vars.serverAddress || vars.server);
    if (!serverDisplay) return null;
    return serverDisplay.toLowerCase() === 'solo' ? 'singleplayer' : 'multiplayer';
}

function resolveServerDisplay(vars) {
    const candidates = [
        vars.serverDisplay,
        vars.serverAddress,
        vars.server,
        vars.serverName
    ].map(sanitizeText).filter(Boolean);

    return candidates[0] || null;
}

function setActivity(activity) {
    if (!rpc || !isConnected) return;

    const nextSignature = JSON.stringify(activity);
    const currentSignature = currentActivity ? JSON.stringify(currentActivity) : null;
    if (nextSignature === currentSignature) return;

    currentActivity = activity;
    try {
        rpc.setActivity(activity).catch(err => {
            console.error('RPC setActivity error:', err);
            isConnected = false;
        });
    } catch (e) {
        console.error('RPC crash prevented:', e);
        isConnected = false;
    }
}

function initDiscordRPC() {
    const config = store.get('discordRpc');
    if (!config || !config.enabled) return;

    if (rpc && isConnected) {
        updatePresenceFromConfig(global.lastRpcVars || { status: 'idle' });
        return;
    }

    if (rpc) {
        try { rpc.destroy().catch(() => {}); } catch (e) {}
        rpc = null;
        isConnected = false;
    }

    rpc = new DiscordRPC.Client({ transport: 'ipc' });

    rpc.on('ready', () => {
        console.log('Discord RPC connected as', rpc.user?.username);
        isConnected = true;
        currentActivity = null;
        rpcStartTime = Date.now();
        updatePresenceFromConfig(global.lastRpcVars || { status: 'idle' });
    });

    rpc.on('disconnected', () => {
        console.log('Discord RPC disconnected');
        isConnected = false;
        rpc = null;
        currentActivity = null;
    });

    rpc.login({ clientId }).catch(err => {
        console.error('Failed to connect to Discord RPC:', err.message);
        rpc = null;
        isConnected = false;
        currentActivity = null;
    });
}

function stopDiscordRPC() {
    const oldRpc = rpc;
    rpc = null;
    isConnected = false;
    currentActivity = null;
    if (oldRpc) {
        try { oldRpc.clearActivity().catch(() => {}); } catch (e) {}
        setTimeout(() => {
            oldRpc.destroy().catch(() => {});
        }, 500);
    }
}

function updatePresenceFromConfig(customVars = {}) {
    const config = store.get('discordRpc') || {};
    if (!config.enabled) {
        stopDiscordRPC();
        return;
    }

    if (!rpc || !isConnected) {
        initDiscordRPC();
        return;
    }

    if (customVars && Object.keys(customVars).length > 0) {
        global.lastRpcVars = { ...global.lastRpcVars, ...customVars };
    }

    const vars = global.lastRpcVars || { status: 'idle' };
    const status = sanitizeText(vars.status).toLowerCase() || 'idle';

    let username = sanitizeText(vars.username);
    if (!username) {
        const accounts = store.get('accounts') || [];
        const activeId = store.get('activeAccountId');
        const activeAccount = accounts.find(a => a.id === activeId);
        username = activeAccount ? activeAccount.name : 'Joueur';
    }

    const version = sanitizeText(vars.version) || 'Elysia';
    const serverType = normalizeServerType(vars);
    const serverDisplay = resolveServerDisplay(vars);

    const activity = {
        largeImageKey: 'elysia_logo',
        largeImageText: 'Elysia Launcher - Premium PvP Client',
        instance: true,
        buttons: [
            { label: 'Site Web', url: 'https://elysiastudios.net' },
            { label: 'Rejoindre le Discord', url: 'https://discord.gg/elysiastudios' }
        ]
    };

    if (status === 'idle') {
        activity.details = 'Dans le launcher';
        activity.state = truncateText(`Compte : ${username}`);
        activity.startTimestamp = rpcStartTime;
        global.playStartTime = null;
        global.downloadStartTime = null;
    } else if (status === 'downloading') {
        activity.details = 'Préparation du client';
        activity.state = truncateText(`Version : ${version}`);
        if (!global.downloadStartTime) global.downloadStartTime = Date.now();
        activity.startTimestamp = global.downloadStartTime;
        global.playStartTime = null;
    } else if (status === 'menu') {
        activity.details = `Client ouvert - ${version}`;
        activity.state = truncateText(serverDisplay ? `Connexion : ${serverDisplay}` : `Dans les menus - ${username}`);
        if (!global.playStartTime) global.playStartTime = Date.now();
        activity.startTimestamp = global.playStartTime;
        global.downloadStartTime = null;
    } else if (status === 'playing') {
        if (serverType === 'singleplayer') {
            activity.details = `En solo - ${version}`;
            activity.state = truncateText(`Monde local - ${username}`);
        } else if (serverDisplay) {
            activity.details = `En multijoueur - ${version}`;
            activity.state = truncateText(serverDisplay);
        } else {
            activity.details = `En jeu - ${version}`;
            activity.state = truncateText(`Compte : ${username}`);
        }
        if (!global.playStartTime) global.playStartTime = Date.now();
        activity.startTimestamp = global.playStartTime;
        global.downloadStartTime = null;
    } else {
        activity.details = 'Dans le launcher';
        activity.state = truncateText(`Compte : ${username}`);
        activity.startTimestamp = rpcStartTime;
    }

    setActivity(activity);
}

function setupDiscordIpc() {
    ipcMain.handle('discord:toggle', (_, enabled) => {
        const config = store.get('discordRpc');
        config.enabled = enabled;
        store.set('discordRpc', config);

        if (enabled) {
            initDiscordRPC();
        } else {
            stopDiscordRPC();
        }
        return true;
    });

    ipcMain.handle('discord:update', (_, newConfig) => {
        const config = store.get('discordRpc');
        store.set('discordRpc', { ...config, ...newConfig });
        updatePresenceFromConfig();
        return true;
    });

    ipcMain.handle('discord:reconnect', async () => {
        stopDiscordRPC();
        await new Promise(r => setTimeout(r, 2000));
        initDiscordRPC();
        await new Promise(r => setTimeout(r, 3000));
        return isConnected;
    });

    ipcMain.on('game:status', (_, statusData) => {
        updatePresenceFromConfig(statusData);

        if (statusData && (statusData.status === 'playing' || statusData.status === 'menu')) {
            setTimeout(() => {
                updatePresenceFromConfig(statusData);
            }, 5000);
            setTimeout(() => {
                updatePresenceFromConfig(statusData);
            }, 15000);
        }
    });

    const config = store.get('discordRpc') || {};
    if (config.enabled) {
        const accounts = store.get('accounts') || [];
        const activeId = store.get('activeAccountId');
        const activeAccount = accounts.find(a => a.id === activeId);
        if (activeAccount) {
            global.lastRpcVars = { ...global.lastRpcVars, username: activeAccount.name };
        }

        setTimeout(() => initDiscordRPC(), 2000);
    }

    ipcMain.handle('discord:link', async () => {
        if (!rpc || !isConnected) {
            const config = store.get('discordRpc') || {};
            const wasEnabled = config.enabled;
            if (!wasEnabled) {
                config.enabled = true;
                store.set('discordRpc', config);
            }

            stopDiscordRPC();
            await new Promise(r => setTimeout(r, 1000));

            rpc = new DiscordRPC.Client({ transport: 'ipc' });

            return new Promise((resolve) => {
                const timeout = setTimeout(() => {
                    if (!wasEnabled) {
                        const restored = store.get('discordRpc') || {};
                        restored.enabled = wasEnabled;
                        store.set('discordRpc', restored);
                    }
                    resolve({ success: false, error: "Discord n'est pas lancé ou la connexion a échoué. Vérifie que Discord est ouvert." });
                }, 8000);

                rpc.on('ready', () => {
                    clearTimeout(timeout);
                    isConnected = true;
                    currentActivity = null;

                    if (rpc.user) {
                        const profile = {
                            id: rpc.user.id,
                            username: rpc.user.username,
                            avatarUrl: rpc.user.avatar
                                ? `https://cdn.discordapp.com/avatars/${rpc.user.id}/${rpc.user.avatar}.png?size=256`
                                : null
                        };
                        store.set('discordProfile', profile);
                        updatePresenceFromConfig(global.lastRpcVars || { status: 'idle' });
                        resolve({ success: true, profile });
                    } else {
                        resolve({ success: false, error: 'Connexion établie mais profil introuvable.' });
                    }
                });

                rpc.login({ clientId }).catch(err => {
                    clearTimeout(timeout);
                    resolve({ success: false, error: `Erreur de connexion Discord : ${err.message}` });
                });
            });
        }

        if (rpc.user) {
            const profile = {
                id: rpc.user.id,
                username: rpc.user.username,
                avatarUrl: rpc.user.avatar
                    ? `https://cdn.discordapp.com/avatars/${rpc.user.id}/${rpc.user.avatar}.png?size=256`
                    : null
            };
            store.set('discordProfile', profile);
            updatePresenceFromConfig(global.lastRpcVars || { status: 'idle' });
            return { success: true, profile };
        }

        return { success: false, error: 'Discord RPC connecté mais aucun utilisateur trouvé.' };
    });
}

module.exports = { setupDiscordIpc, updatePresenceFromConfig };
