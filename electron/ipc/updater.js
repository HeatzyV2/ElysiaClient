const { app, ipcMain } = require('electron');
const { autoUpdater } = require('electron-updater');

let updaterState = null;
let updaterInfo = null;
let setupDone = false;

function createInitialState() {
    const packaged = app.isPackaged;
    const supported = packaged && process.platform === 'win32';

    return {
        supported,
        packaged,
        platform: process.platform,
        currentVersion: app.getVersion(),
        availableVersion: null,
        downloadedVersion: null,
        releaseName: null,
        releaseDate: null,
        releaseNotes: '',
        lastCheckedAt: null,
        progressPercent: 0,
        bytesPerSecond: 0,
        transferred: 0,
        total: 0,
        status: supported ? 'idle' : 'unsupported',
        message: supported
            ? 'Prêt à vérifier les mises à jour.'
            : 'Les mises à jour sont disponibles uniquement dans la version Windows installée du launcher.',
        error: null
    };
}

function normalizeReleaseNotes(releaseNotes) {
    if (!releaseNotes) return '';
    if (typeof releaseNotes === 'string') return releaseNotes.trim();
    if (!Array.isArray(releaseNotes)) return '';

    return releaseNotes
        .map(note => {
            if (!note) return '';
            if (typeof note === 'string') return note.trim();

            const version = String(note.version || '').trim();
            const body = String(note.note || '').trim();
            if (version && body) return `Version ${version}\n${body}`;
            return body || version;
        })
        .filter(Boolean)
        .join('\n\n');
}

function updateState(mainWindow, patch = {}) {
    updaterState = {
        ...updaterState,
        ...patch,
        currentVersion: app.getVersion()
    };

    if (mainWindow && !mainWindow.isDestroyed()) {
        mainWindow.webContents.send('updater:state', updaterState);
    }

    return updaterState;
}

function updateStateFromInfo(mainWindow, info = {}) {
    return updateState(mainWindow, {
        availableVersion: info.version || updaterState.availableVersion || null,
        downloadedVersion: updaterState.downloadedVersion,
        releaseName: info.releaseName || info.version || null,
        releaseDate: info.releaseDate || null,
        releaseNotes: normalizeReleaseNotes(info.releaseNotes)
    });
}

function ensureUpdaterSupport() {
    return updaterState?.supported === true;
}

function setupUpdaterIpc(mainWindow) {
    if (setupDone) return;
    setupDone = true;

    updaterState = createInitialState();

    autoUpdater.autoDownload = false;
    autoUpdater.autoInstallOnAppQuit = false;
    autoUpdater.allowPrerelease = false;
    autoUpdater.fullChangelog = true;

    mainWindow?.webContents.on('did-finish-load', () => {
        updateState(mainWindow);
    });

    autoUpdater.on('checking-for-update', () => {
        updateState(mainWindow, {
            status: 'checking',
            message: 'Vérification des mises à jour en cours...',
            error: null,
            progressPercent: 0,
            bytesPerSecond: 0,
            transferred: 0,
            total: 0
        });
    });

    autoUpdater.on('update-available', info => {
        updaterInfo = info;
        updateStateFromInfo(mainWindow, info);
        updateState(mainWindow, {
            status: 'available',
            message: `La version ${info.version} est disponible.`,
            downloadedVersion: null,
            lastCheckedAt: Date.now(),
            error: null,
            progressPercent: 0,
            bytesPerSecond: 0,
            transferred: 0,
            total: 0
        });
    });

    autoUpdater.on('update-not-available', info => {
        updaterInfo = null;
        updateState(mainWindow, {
            status: 'not_available',
            message: 'Aucune mise à jour disponible.',
            availableVersion: null,
            downloadedVersion: null,
            releaseName: null,
            releaseDate: null,
            releaseNotes: '',
            lastCheckedAt: Date.now(),
            error: null,
            progressPercent: 0,
            bytesPerSecond: 0,
            transferred: 0,
            total: 0
        });

        if (info?.version) {
            updateState(mainWindow, {
                releaseName: info.version
            });
        }
    });

    autoUpdater.on('download-progress', progress => {
        updateState(mainWindow, {
            status: 'downloading',
            message: `Téléchargement de la mise à jour... ${Number(progress.percent || 0).toFixed(1)}%`,
            progressPercent: Number(progress.percent || 0),
            bytesPerSecond: Number(progress.bytesPerSecond || 0),
            transferred: Number(progress.transferred || 0),
            total: Number(progress.total || 0),
            error: null
        });
    });

    autoUpdater.on('update-downloaded', info => {
        updaterInfo = info;
        updateStateFromInfo(mainWindow, info);
        updateState(mainWindow, {
            status: 'downloaded',
            message: `La version ${info.version} est prête à être installée.`,
            downloadedVersion: info.version || updaterState.availableVersion,
            lastCheckedAt: Date.now(),
            error: null,
            progressPercent: 100
        });
    });

    autoUpdater.on('error', error => {
        updateState(mainWindow, {
            status: 'error',
            message: 'Impossible de vérifier ou télécharger la mise à jour.',
            error: error?.message || String(error),
            lastCheckedAt: Date.now()
        });
    });

    ipcMain.handle('updater:getState', () => {
        return updaterState;
    });

    ipcMain.handle('updater:check', async () => {
        if (!ensureUpdaterSupport()) {
            return { success: false, state: updaterState };
        }

        try {
            await autoUpdater.checkForUpdates();
            return { success: true, state: updaterState };
        } catch (error) {
            updateState(mainWindow, {
                status: 'error',
                message: 'La vérification des mises à jour a échoué.',
                error: error?.message || String(error),
                lastCheckedAt: Date.now()
            });
            return { success: false, state: updaterState, error: updaterState.error };
        }
    });

    ipcMain.handle('updater:download', async () => {
        if (!ensureUpdaterSupport()) {
            return { success: false, state: updaterState };
        }

        if (!updaterInfo || updaterState.status !== 'available') {
            return {
                success: false,
                state: updaterState,
                error: 'Aucune mise à jour n’est prête à être téléchargée.'
            };
        }

        try {
            await autoUpdater.downloadUpdate();
            return { success: true, state: updaterState };
        } catch (error) {
            updateState(mainWindow, {
                status: 'error',
                message: 'Le téléchargement de la mise à jour a échoué.',
                error: error?.message || String(error)
            });
            return { success: false, state: updaterState, error: updaterState.error };
        }
    });

    ipcMain.handle('updater:install', async () => {
        if (!ensureUpdaterSupport()) {
            return { success: false, state: updaterState };
        }

        if (updaterState.status !== 'downloaded') {
            return {
                success: false,
                state: updaterState,
                error: 'Aucune mise à jour téléchargée n’est prête à être installée.'
            };
        }

        setImmediate(() => autoUpdater.quitAndInstall(false, true));
        return { success: true, state: updaterState };
    });
}

module.exports = { setupUpdaterIpc };
