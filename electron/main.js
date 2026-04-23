const { app, BrowserWindow, ipcMain, shell } = require('electron');
const path = require('path');

const { setupUpdaterIpc } = require('./ipc/updater');
const { setupStoreIpc } = require('./ipc/store');
const { setupAuthIpc } = require('./ipc/auth');
const { setupLaunchIpc } = require('./ipc/launch');
const { setupModsIpc } = require('./ipc/mods');
const { setupSettingsIpc } = require('./ipc/settings');
const { setupDiscordIpc } = require('./ipc/discord');
const { setupServersIpc } = require('./ipc/servers');
const { setupSkinIpc } = require('./ipc/skin');
const { setupSnapshotIpc } = require('./ipc/snapshot');
const { setupAntiCheatIpc } = require('./ipc/anticheat');
const { setupSocialIpc } = require('./ipc/social');
const { setupBedrockIpc } = require('./ipc/bedrock');
const { setupImportIpc } = require('./ipc/import');
const { setupFeaturesIpc } = require('./ipc/features');

// Fix for GPU stability and video decoding
app.commandLine.appendSwitch('disable-gpu-sandbox');
app.commandLine.appendSwitch('ignore-gpu-blacklist');
app.commandLine.appendSwitch('disable-features', 'GpuProcessHighPriority');

let mainWindow;

function createWindow() {
    mainWindow = new BrowserWindow({
        width: 1280,
        height: 760,
        minWidth: 1024,
        minHeight: 600,
        frame: false,
        transparent: false,
        backgroundColor: '#0a0a1a',
        resizable: true,
        icon: path.join(__dirname, '..', 'build', 'icon.ico'),
        webPreferences: {
            preload: path.join(__dirname, 'preload.js'),
            contextIsolation: true,
            nodeIntegration: false,
            sandbox: false
        }
    });

    mainWindow.loadFile(path.join(__dirname, '..', 'src', 'index.html'));

    // Bypass frame blocking (ERR_BLOCKED_BY_RESPONSE)
    mainWindow.webContents.session.webRequest.onHeadersReceived((details, callback) => {
        const headers = details.responseHeaders;
        ['x-frame-options', 'content-security-policy', 'frame-options'].forEach(h => {
            const key = Object.keys(headers).find(k => k.toLowerCase() === h);
            if (key) delete headers[key];
        });
        callback({ responseHeaders: headers });
    });

    // Open external links in browser
    mainWindow.webContents.setWindowOpenHandler(({ url }) => {
        shell.openExternal(url);
        return { action: 'deny' };
    });
}

app.whenReady().then(() => {
    createWindow();

    // Setup modular IPC Handlers
    setupStoreIpc();
    setupAuthIpc(mainWindow);
    setupLaunchIpc(mainWindow);
    setupModsIpc(mainWindow);
    setupSettingsIpc(mainWindow);
    setupUpdaterIpc(mainWindow);
    setupDiscordIpc();
    setupServersIpc();
    setupSkinIpc(mainWindow);
    setupSnapshotIpc(mainWindow);
    setupAntiCheatIpc();
    setupSocialIpc();
    setupBedrockIpc(mainWindow);
    setupImportIpc(mainWindow);
    setupFeaturesIpc(mainWindow);

    // Window controls IPC
    ipcMain.on('window:minimize', () => mainWindow?.minimize());
    ipcMain.on('window:maximize', () => {
        if (mainWindow?.isMaximized()) {
            mainWindow.unmaximize();
        } else {
            mainWindow?.maximize();
        }
    });
    ipcMain.on('window:close', () => mainWindow?.close());
});

app.on('window-all-closed', () => {
    if (process.platform !== 'darwin') app.quit();
});

app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) createWindow();
});
