const { ipcMain, dialog, app, shell } = require('electron');
const os = require('os');
const fs = require('fs');
const path = require('path');
const { execFile } = require('child_process');
const { pipeline } = require('stream/promises');
const { store } = require('./store');
const { getMinecraftPath, getInstancePath } = require('./launch');
const { getStats, migrateStatsFromStore } = require('./stats');

function setupSettingsIpc(mainWindow) {
    ipcMain.handle('settings:get', async () => {
        return store.get('settings');
    });

    ipcMain.handle('stats:get', async () => {
        return migrateStatsFromStore(store) || getStats();
    });

    ipcMain.handle('settings:save', async (_, settings) => {
        store.set('settings', settings);
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

    ipcMain.handle('settings:selectBackground', async () => {
        const result = await dialog.showOpenDialog(mainWindow, {
            title: 'Choisir un fond d\'écran',
            properties: ['openFile'],
            filters: [
                { name: 'Images & Vidéos', extensions: ['png', 'jpg', 'jpeg', 'gif', 'webp', 'mp4', 'webm'] }
            ]
        });
        if (result.canceled) return { success: false };
        return { success: true, path: result.filePaths[0] };
    });

    ipcMain.handle('settings:selectMusic', async () => {
        const result = await dialog.showOpenDialog(mainWindow, {
            title: 'Choisir une musique de fond',
            properties: ['openFile'],
            filters: [
                { name: 'Fichiers Audio', extensions: ['mp3', 'wav', 'ogg', 'aac', 'flac'] }
            ]
        });
        if (result.canceled) return { success: false };
        return { success: true, path: result.filePaths[0] };
    });

    ipcMain.handle('settings:getMemoryInfo', async () => {
        const totalMem = Math.floor(os.totalmem() / (1024 * 1024 * 1024));
        return { totalMemGB: totalMem };
    });

    ipcMain.handle('settings:getSystemSpecs', async () => {
        const cpus = os.cpus();
        const totalMem = Math.floor(os.totalmem() / (1024 * 1024 * 1024));
        let gpuName = 'Non détecté';
        
        try {
            const gpuInfo = await app.getGPUInfo('complete');
            if (gpuInfo?.gpuDevice?.length > 0) {
                // Find the active GPU or fallback to the first one
                const activeGpu = gpuInfo.gpuDevice.find(g => g.active) || gpuInfo.gpuDevice[0];
                gpuName = activeGpu.deviceString || activeGpu.vendorString || 'Non détecté';
            }
        } catch (e) {
            console.error('Failed to get GPU info:', e);
        }

        return {
            cpu: cpus[0].model,
            cores: cpus.length,
            ram: totalMem,
            gpu: gpuName
        };
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
    ipcMain.handle('settings:clearCache', async () => {
        try {
            const mcPath = getMinecraftPath();
            const pathsToClear = [
                path.join(mcPath, 'logs'),
                path.join(mcPath, 'crash-reports'),
                path.join(app.getPath('userData'), 'Cache')
            ];

            // Also clear logs/crash-reports from all instances
            const instancesPath = path.join(mcPath, 'instances');
            if (fs.existsSync(instancesPath)) {
                const instances = fs.readdirSync(instancesPath);
                for (const instance of instances) {
                    pathsToClear.push(path.join(instancesPath, instance, 'logs'));
                    pathsToClear.push(path.join(instancesPath, instance, 'crash-reports'));
                }
            }

            for (const p of pathsToClear) {
                if (fs.existsSync(p)) {
                    fs.rmSync(p, { recursive: true, force: true });
                }
            }

            return { success: true };
        } catch (error) {
            console.error('Clear cache error:', error);
            return { success: false, error: error.message };
        }
    });

    ipcMain.handle('settings:searchMusic', async (_, query) => {
        try {
            const response = await fetch(`https://www.youtube.com/results?search_query=${encodeURIComponent(query)}`, {
                headers: {
                    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36'
                }
            });
            if (!response.ok) throw new Error('Search failed');
            
            const html = await response.text();
            const match = html.match(/var ytInitialData = ({.*?});/);
            
            if (!match) throw new Error('Could not parse search results');
            
            const data = JSON.parse(match[1]);
            const results = [];
            
            // Navigate through the complex YouTube JSON structure
            const contents = data.contents.twoColumnBrowseResultsRenderer?.tabs[0].content.sectionListRenderer?.contents[0].itemSectionRenderer?.contents
                || data.contents.twoColumnSearchResultsRenderer?.primaryContents.sectionListRenderer?.contents[0].itemSectionRenderer?.contents;

            if (contents) {
                contents.forEach(item => {
                    const video = item.videoRenderer;
                    if (video && video.videoId) {
                        results.push({
                            id: video.videoId,
                            title: video.title.runs[0].text,
                            author: video.ownerText?.runs[0]?.text || 'YouTube',
                            duration: video.lengthText?.simpleText || '0:00',
                            thumbnail: video.thumbnail.thumbnails[0].url
                        });
                    }
                });
            }

            return { success: true, results };
        } catch (error) {
            console.error('YouTube Scraper error:', error);
            return { success: false, error: 'Impossible de contacter YouTube. Vérifiez votre connexion.' };
        }
    });

    ipcMain.handle('settings:openMcFolder', async () => {
        const mcPath = getMinecraftPath();
        shell.openPath(mcPath);
        return { success: true };
    });

    ipcMain.handle('settings:exportLogs', async () => {
        try {
            const mcPath = getMinecraftPath();
            const latestLog = path.join(mcPath, 'logs', 'latest.log');
            
            if (!fs.existsSync(latestLog)) {
                return { success: false, error: 'Aucun log trouvé.' };
            }

            const dest = path.join(app.getPath('desktop'), `elysia-logs-${Date.now()}.log`);
            fs.copyFileSync(latestLog, dest);
            shell.showItemInFolder(dest);
            
            return { success: true };
        } catch (error) {
            return { success: false, error: error.message };
        }
    });

    ipcMain.handle('settings:getStreamUrl', async (_, videoId) => {
        const ytdlpPath = path.join(app.getPath('userData'), 'yt-dlp.exe');
        
        // Auto-download yt-dlp if not present
        if (!fs.existsSync(ytdlpPath)) {
            console.log('Downloading yt-dlp...');
            try {
                const res = await fetch('https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp.exe');
                if (!res.ok) throw new Error('Download failed');
                const fileStream = fs.createWriteStream(ytdlpPath);
                const { Readable } = require('stream');
                await pipeline(Readable.fromWeb(res.body), fileStream);
                console.log('yt-dlp downloaded successfully');
            } catch (e) {
                console.error('Failed to download yt-dlp:', e.message);
                return { success: false, error: 'Impossible de télécharger yt-dlp.' };
            }
        }
        
        // Extract audio URL using yt-dlp
        return new Promise((resolve) => {
            execFile(ytdlpPath, [
                '-f', 'bestaudio',
                '--get-url',
                '--no-warnings',
                `https://www.youtube.com/watch?v=${videoId}`
            ], { timeout: 15000 }, (error, stdout, stderr) => {
                if (error) {
                    console.error('yt-dlp error:', error.message);
                    return resolve({ success: false, error: 'Extraction échouée.' });
                }
                
                const url = stdout.trim();
                if (url && url.startsWith('http')) {
                    console.log('Stream extracted via yt-dlp');
                    return resolve({ success: true, url });
                }
                
                return resolve({ success: false, error: 'Aucun flux audio trouvé.' });
            });
        });
    });
}

module.exports = { setupSettingsIpc };
