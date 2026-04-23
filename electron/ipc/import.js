const { ipcMain } = require('electron');
const fs = require('fs');
const path = require('path');
const { getInstancePath } = require('./launch');
const { store } = require('./store');

function setupImportIpc(mainWindow) {
    ipcMain.handle('import:scan', async () => {
        const appData = process.env.APPDATA;
        const userProfile = process.env.USERPROFILE;
        const launchers = [];

        const checkPath = (id, name, p) => {
            if (fs.existsSync(p)) {
                launchers.push({ id, name, path: p });
            }
        };

        // Standard Paths
        checkPath('vanilla', 'Minecraft Vanilla / Badlion', path.join(appData, '.minecraft'));
        checkPath('lunar', 'Lunar Client', path.join(userProfile, '.lunarclient'));
        checkPath('feather', 'Feather Client', path.join(appData, '.feather'));
        
        // Special case: Feather inside .minecraft
        if (fs.existsSync(path.join(appData, '.minecraft', 'feather'))) {
            launchers.push({ id: 'feather_mc', name: 'Feather (.minecraft)', path: path.join(appData, '.minecraft') });
        }

        checkPath('modrinth', 'Modrinth App', path.join(appData, 'com.modrinth.theseus', 'profiles'));
        checkPath('prism', 'Prism Launcher', path.join(appData, 'PrismLauncher', 'instances'));
        checkPath('curseforge', 'CurseForge', path.join(userProfile, 'curseforge', 'minecraft', 'Instances'));
        checkPath('badlion', 'Badlion Client', path.join(appData, 'BadlionClient', 'minecraft'));

        return launchers;
    });

    ipcMain.handle('import:getInstances', async (_, launcherPath) => {
        const dirs = [];
        try {
            const scan = (dir, depth = 0) => {
                if (depth > 3) return; // Slightly deeper scan
                if (!fs.existsSync(dir)) return;

                const items = fs.readdirSync(dir, { withFileTypes: true });
                for (const item of items) {
                    if (item.isDirectory()) {
                        const subPath = path.join(dir, item.name);
                        
                        // Check for .minecraft or user-minecraft subfolder (Prism/Feather/MultiMC style)
                        const dotMcPath = path.join(subPath, '.minecraft');
                        const userMcPath = path.join(subPath, 'user-minecraft');
                        
                        let searchPath = subPath;
                        if (fs.existsSync(dotMcPath)) searchPath = dotMcPath;
                        else if (fs.existsSync(userMcPath)) searchPath = userMcPath;

                        const markers = ['options.txt', 'mods', 'resourcepacks', 'config', 'instance.cfg', 'mmc-pack.json'];
                        if (markers.some(m => fs.existsSync(path.join(searchPath, m)))) {
                            const displayName = item.name === 'user-minecraft' ? 'Feather (Par défaut)' : item.name;
                            dirs.push({ name: displayName, path: searchPath });
                        } else if (['offline', 'multiver', 'instances', 'Instances', 'profiles', 'user-minecraft', '.feather'].includes(item.name)) {
                            scan(subPath, depth + 1);
                        }
                    }
                }
            };
            scan(launcherPath);
        } catch (e) {
            console.error('[Import] Error scanning instances:', e);
        }
        return dirs;
    });

    ipcMain.handle('import:execute', async (_, { sourcePath, items }) => {
        const settings = store.get('settings') || {};
        const targetPath = getInstancePath(settings);
        
        console.log(`[Import] Starting import from ${sourcePath} to ${targetPath}`);

        try {
            if (!fs.existsSync(targetPath)) {
                fs.mkdirSync(targetPath, { recursive: true });
            }

            const sendProgress = (msg, percent) => {
                mainWindow?.webContents.send('import:progress', { message: msg, percent });
            };

            const copyFileOrDir = (src, dest) => {
                if (!fs.existsSync(src)) return false;
                
                const stats = fs.lstatSync(src);
                const fileName = path.basename(src).toLowerCase();

                // Skip temporary or cache files
                if (fileName.includes('.temp') || fileName.includes('.cache') || fileName.endsWith('.tmp')) {
                    return false;
                }

                if (stats.isSymbolicLink()) {
                    const realPath = fs.realpathSync(src);
                    return copyFileOrDir(realPath, dest);
                }

                if (stats.isDirectory()) {
                    if (!fs.existsSync(dest)) fs.mkdirSync(dest, { recursive: true });
                    const entries = fs.readdirSync(src);
                    for (let entry of entries) {
                        copyFileOrDir(path.join(src, entry), path.join(dest, entry));
                    }
                } else {
                    // Filter out non-essentials in mods folder
                    if (src.includes('mods')) {
                        // Skip launcher-specific internal mods or unwanted mods
                        if (fileName.includes('feather') || fileName.includes('lunar') || fileName.includes('badlion') ||
                            fileName.includes('bettercombat') || fileName.includes('inventoryprofilesnext') || fileName.includes('libipn') ||
                            fileName.includes('armorstatus')) {
                            console.log(`[Import] Skipping blacklisted mod: ${fileName}`);
                            return false;
                        }
                        // Skip metadata
                        if (fileName.endsWith('.json') || fileName.endsWith('.txt')) {
                            return false;
                        }
                    }
                    fs.copyFileSync(src, dest);
                }
                return true;
            };

            const totalItems = items.length;
            let completedItems = 0;
            let filesCopied = 0;

            for (const item of items) {
                const percent = Math.floor((completedItems / totalItems) * 100);

                if (item === 'options') {
                    sendProgress('Importation des paramètres...', percent);
                    let optSrc = path.join(sourcePath, 'options.txt');
                    
                    // Specific fallbacks
                    if (!fs.existsSync(optSrc)) {
                        if (sourcePath.includes('.feather') || fs.existsSync(path.join(sourcePath, 'feather'))) {
                            // Check for Feather's specific options file
                            const featherOpt = path.join(sourcePath, 'feather', 'mc_options_v2.txt');
                            if (fs.existsSync(featherOpt)) optSrc = featherOpt;
                            else optSrc = path.join(process.env.APPDATA, '.feather', 'user-minecraft', 'options.txt');
                        } else if (sourcePath.includes('.lunarclient')) {
                            optSrc = path.join(process.env.USERPROFILE, '.lunarclient', 'settings', 'game', 'options.txt');
                        }
                    }
                    
                    if (copyFileOrDir(optSrc, path.join(targetPath, 'options.txt'))) {
                        filesCopied++;
                    }

                    const optOfSrc = optSrc.replace('options.txt', 'optionsof.txt');
                    copyFileOrDir(optOfSrc, path.join(targetPath, 'optionsof.txt'));
                }

                if (item === 'resourcepacks') {
                    sendProgress('Importation des resource packs...', percent);
                    let rpSrc = path.join(sourcePath, 'resourcepacks');
                    
                    if (!fs.existsSync(rpSrc)) {
                        if (sourcePath.includes('.lunarclient')) {
                            rpSrc = path.join(process.env.USERPROFILE, '.lunarclient', 'offline', 'multiver', 'resourcepacks');
                        } else if (sourcePath.includes('.feather')) {
                            rpSrc = path.join(process.env.APPDATA, '.feather', 'user-minecraft', 'resourcepacks');
                        }
                    }

                    if (copyFileOrDir(rpSrc, path.join(targetPath, 'resourcepacks'))) {
                        filesCopied++;
                    }
                }

                if (item === 'mods') {
                    sendProgress('Importation des mods...', percent);
                    let modsSrc = path.join(sourcePath, 'mods');
                    // Feather sometimes uses feather-mods
                    if (!fs.existsSync(modsSrc) || fs.readdirSync(modsSrc).length === 0) {
                        const featherMods = path.join(sourcePath, 'feather-mods');
                        if (fs.existsSync(featherMods)) modsSrc = featherMods;
                    }

                    if (copyFileOrDir(modsSrc, path.join(targetPath, 'mods'))) {
                        filesCopied++;
                    }
                }

                if (item === 'config') {
                    sendProgress('Importation des configurations...', percent);
                    if (copyFileOrDir(path.join(sourcePath, 'config'), path.join(targetPath, 'config'))) {
                        filesCopied++;
                    }
                }

                completedItems++;
            }

            if (filesCopied === 0) {
                return { success: false, error: 'Aucun fichier Minecraft valide n\'a été trouvé.' };
            }

            sendProgress('Importation terminée !', 100);
            return { success: true };
        } catch (error) {
            console.error('[Import] Critical failure:', error);
            return { success: false, error: error.message };
        }
    });
}

module.exports = { setupImportIpc };
