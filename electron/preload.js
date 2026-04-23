const { contextBridge, ipcRenderer } = require('electron');

function registerListener(channel, callback) {
    if (typeof callback !== 'function') {
        return () => {};
    }

    const wrapped = (_, data) => callback(data);
    ipcRenderer.on(channel, wrapped);

    return () => {
        ipcRenderer.removeListener(channel, wrapped);
    };
}

contextBridge.exposeInMainWorld('electronAPI', {
    // Window controls
    minimize: () => ipcRenderer.send('window:minimize'),
    maximize: () => ipcRenderer.send('window:maximize'),
    close: () => ipcRenderer.send('window:close'),

    // Authentication
    // Store
    storeGet: (key) => ipcRenderer.invoke('store:get', key),
    storeSet: (key, value) => ipcRenderer.invoke('store:set', key, value),

    // Discord RPC
    discordToggle: (enabled) => ipcRenderer.invoke('discord:toggle', enabled),
    discordUpdate: (config) => ipcRenderer.invoke('discord:update', config),
    discordReconnect: () => ipcRenderer.invoke('discord:reconnect'),
    linkDiscord: () => ipcRenderer.invoke('discord:link'),

    // Auth
    authMicrosoft: () => ipcRenderer.invoke('auth:microsoft'),
    authOffline: (username) => ipcRenderer.invoke('auth:offline', username),
    authRefresh: (accountId) => ipcRenderer.invoke('auth:refresh', accountId),
    authLogout: (accountId) => ipcRenderer.invoke('auth:logout', accountId),
    getAccounts: () => ipcRenderer.invoke('auth:getAccounts'),
    switchAccount: (accountId) => ipcRenderer.invoke('auth:switchAccount', accountId),
    uploadSkin: () => ipcRenderer.invoke('skin:upload'),

    // Launch
    launch: (options) => ipcRenderer.invoke('launch:start', options),
    stopLaunch: () => ipcRenderer.invoke('launch:stop'),
    openFolder: () => ipcRenderer.invoke('launch:openFolder'),
    getInstanceInfo: () => ipcRenderer.invoke('launch:getInstanceInfo'),
    listInstances: () => ipcRenderer.invoke('launch:listInstances'),
    checkLaunchStatus: () => ipcRenderer.invoke('launch:checkStatus'),
    getStartTime: () => ipcRenderer.invoke('launch:getStartTime'),

    // Settings / Stats
    getSettings: () => ipcRenderer.invoke('settings:get'),
    getStats: () => ipcRenderer.invoke('stats:get'),
    saveSettings: (settings) => ipcRenderer.invoke('settings:save', settings),
    getVersions: () => ipcRenderer.invoke('settings:getVersions'),
    selectJavaPath: () => ipcRenderer.invoke('settings:selectJavaPath'),
    selectBackground: () => ipcRenderer.invoke('settings:selectBackground'),
    selectMusic: () => ipcRenderer.invoke('settings:selectMusic'),
    searchMusic: (query) => ipcRenderer.invoke('settings:searchMusic', query),
    getStreamUrl: (videoId) => ipcRenderer.invoke('settings:getStreamUrl', videoId),

    getMemoryInfo: () => ipcRenderer.invoke('settings:getMemoryInfo'),
    getSystemSpecs: () => ipcRenderer.invoke('settings:getSystemSpecs'),
    clearCache: () => ipcRenderer.invoke('settings:clearCache'),
    openMcFolder: () => ipcRenderer.invoke('settings:openMcFolder'),
    exportLogs: () => ipcRenderer.invoke('settings:exportLogs'),

    // Updater
    getUpdaterState: () => ipcRenderer.invoke('updater:getState'),
    checkForLauncherUpdates: () => ipcRenderer.invoke('updater:check'),
    downloadLauncherUpdate: () => ipcRenderer.invoke('updater:download'),
    installLauncherUpdate: () => ipcRenderer.invoke('updater:install'),

    // Mods / Assets
    searchMods: (query, version, loader, type = 'mod') => ipcRenderer.invoke('mods:search', { query, version, loader, type }),
    installMod: (projectId, version, loader, type = 'mod') => ipcRenderer.invoke('mods:install', { projectId, version, loader, type }),
    getInstalledMods: (version, loader, type = 'mod') => ipcRenderer.invoke('mods:list', { version, loader, type }),
    deleteMod: (filename, version, loader, type = 'mod') => ipcRenderer.invoke('mods:delete', { filename, version, loader, type }),
    cleanMods: (version, loader, type = 'mod') => ipcRenderer.invoke('mods:clean', { version, loader, type }),
    dropModFile: (filePath, version, loader, type = 'mod') => ipcRenderer.invoke('mods:dropFile', { filePath, version, loader, type }),
    exportModpack: (version, loader) => ipcRenderer.invoke('mods:exportPack', { version, loader }),
    importModpack: (version, loader) => ipcRenderer.invoke('mods:importPack', { version, loader }),

    // Servers
    getServers: () => ipcRenderer.invoke('servers:list'),
    addServer: (server) => ipcRenderer.invoke('servers:add', server),
    removeServer: (serverId) => ipcRenderer.invoke('servers:remove', serverId),
    pingServer: (address) => ipcRenderer.invoke('servers:ping', address),

    // Snapshots
    listSnapshots: () => ipcRenderer.invoke('snapshot:list'),
    createSnapshot: (name) => ipcRenderer.invoke('snapshot:create', { name }),
    restoreSnapshot: (snapshotId) => ipcRenderer.invoke('snapshot:restore', { snapshotId }),
    deleteSnapshot: (snapshotId) => ipcRenderer.invoke('snapshot:delete', { snapshotId }),

    // Social
    getFriends: () => ipcRenderer.invoke('social:getFriends'),
    addFriend: (friend) => ipcRenderer.invoke('social:addFriend', friend),
    removeFriend: (uuid) => ipcRenderer.invoke('social:removeFriend', uuid),

    // Bedrock
    bedrockCheckInstalled: () => ipcRenderer.invoke('bedrock:checkInstalled'),
    bedrockGetInfo: () => ipcRenderer.invoke('bedrock:getInfo'),
    bedrockLaunch: (options) => ipcRenderer.invoke('bedrock:launch', options),

    // Import
    importScan: () => ipcRenderer.invoke('import:scan'),
    importGetInstances: (path) => ipcRenderer.invoke('import:getInstances', path),
    importExecute: (options) => ipcRenderer.invoke('import:execute', options),

    // Events from main process
    onProgress: (callback) => registerListener('launch:progress', callback),
    onSpeed: (callback) => registerListener('launch:speed', callback),
    onEstimated: (callback) => registerListener('launch:estimated', callback),
    onData: (callback) => registerListener('launch:data', callback),
    onPatch: (callback) => registerListener('launch:patch', callback),
    onClose: (callback) => registerListener('launch:close', callback),
    onError: (callback) => registerListener('launch:error', callback),
    onLaunchStatus: (callback) => registerListener('launch:status', callback),
    onStatsUpdate: (callback) => registerListener('stats:update', callback),
    onAccountsUpdated: (callback) => registerListener('accounts:updated', callback),
    onImportProgress: (callback) => registerListener('import:progress', callback),
    onUpdaterState: (callback) => registerListener('updater:state', callback),
    
    // Features & HUD
    getFeaturesList: () => ipcRenderer.invoke('features:get-list'),
    getFeaturesConfig: () => ipcRenderer.invoke('features:get-config'),
    saveFeaturesConfig: (config) => ipcRenderer.invoke('features:save-config', config),
    
    // Remove listeners
    removeAllListeners: (channel) => ipcRenderer.removeAllListeners(channel)
});
