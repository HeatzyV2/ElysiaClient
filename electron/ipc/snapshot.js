const { ipcMain, dialog } = require('electron');
const { store } = require('./store');
const fs = require('fs');
const path = require('path');
const { getInstancePath, getDataPath } = require('./launch');

function getSnapshotsDir() {
    const dir = path.join(getDataPath(), 'snapshots');
    if (!fs.existsSync(dir)) fs.mkdirSync(dir, { recursive: true });
    return dir;
}

function copyDirSync(src, dest) {
    if (!fs.existsSync(src)) return;
    fs.mkdirSync(dest, { recursive: true });
    const entries = fs.readdirSync(src, { withFileTypes: true });
    for (const entry of entries) {
        const srcPath = path.join(src, entry.name);
        const destPath = path.join(dest, entry.name);
        if (entry.isDirectory()) {
            copyDirSync(srcPath, destPath);
        } else {
            fs.copyFileSync(srcPath, destPath);
        }
    }
}

function setupSnapshotIpc(mainWindow) {
    // List snapshots
    ipcMain.handle('snapshot:list', async () => {
        try {
            const dir = getSnapshotsDir();
            const folders = fs.readdirSync(dir, { withFileTypes: true })
                .filter(d => d.isDirectory())
                .map(d => {
                    const metaPath = path.join(dir, d.name, 'snapshot.json');
                    let meta = { name: d.name, date: null, version: '', loader: '' };
                    if (fs.existsSync(metaPath)) {
                        try { meta = JSON.parse(fs.readFileSync(metaPath, 'utf8')); } catch (e) {}
                    }
                    return { id: d.name, ...meta };
                })
                .sort((a, b) => (b.date || 0) - (a.date || 0));

            return { success: true, snapshots: folders };
        } catch (e) {
            return { success: false, error: e.message };
        }
    });

    // Create snapshot from current instance
    ipcMain.handle('snapshot:create', async (_, { name }) => {
        try {
            const settings = store.get('settings') || {};
            const instanceDir = getInstancePath(settings);

            if (!fs.existsSync(instanceDir)) {
                return { success: false, error: 'Aucune instance trouvée pour la version/loader actuel.' };
            }

            const id = `snapshot-${Date.now()}`;
            const destDir = path.join(getSnapshotsDir(), id);

            mainWindow?.webContents.send('launch:progress', { progress: 0, size: 100, type: 'Snapshot...', percent: 0 });

            copyDirSync(instanceDir, destDir);

            // Write metadata
            const meta = {
                name: name || `Snapshot ${new Date().toLocaleDateString('fr-FR')}`,
                date: Date.now(),
                version: settings.version || 'unknown',
                loader: settings.loader?.type || 'vanilla',
                id
            };
            fs.writeFileSync(path.join(destDir, 'snapshot.json'), JSON.stringify(meta, null, 2));

            mainWindow?.webContents.send('launch:progress', { progress: 100, size: 100, type: 'Snapshot...', percent: 100 });

            return { success: true, snapshot: meta };
        } catch (e) {
            return { success: false, error: e.message };
        }
    });

    // Restore snapshot
    ipcMain.handle('snapshot:restore', async (_, { snapshotId }) => {
        try {
            const settings = store.get('settings') || {};
            const instanceDir = getInstancePath(settings);
            const snapshotDir = path.join(getSnapshotsDir(), snapshotId);

            if (!fs.existsSync(snapshotDir)) {
                return { success: false, error: 'Snapshot introuvable.' };
            }

            // Remove existing instance
            if (fs.existsSync(instanceDir)) {
                fs.rmSync(instanceDir, { recursive: true, force: true });
            }

            copyDirSync(snapshotDir, instanceDir);

            // Remove the snapshot.json metadata from the restored instance
            const metaInInstance = path.join(instanceDir, 'snapshot.json');
            if (fs.existsSync(metaInInstance)) fs.unlinkSync(metaInInstance);

            return { success: true };
        } catch (e) {
            return { success: false, error: e.message };
        }
    });

    // Delete snapshot
    ipcMain.handle('snapshot:delete', async (_, { snapshotId }) => {
        try {
            const snapshotDir = path.join(getSnapshotsDir(), snapshotId);
            if (fs.existsSync(snapshotDir)) {
                fs.rmSync(snapshotDir, { recursive: true, force: true });
            }
            return { success: true };
        } catch (e) {
            return { success: false, error: e.message };
        }
    });
}

module.exports = { setupSnapshotIpc };
