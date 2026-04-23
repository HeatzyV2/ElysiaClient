const fs = require('fs');
const path = require('path');
const { ipcMain } = require('electron');
const { store } = require('./store');

let AdmZip;
try {
    AdmZip = require('adm-zip');
} catch (e) {
    console.error("adm-zip is not installed. Anti-cheat will fail.");
}

const CHEAT_MODS_BLACKLIST = [
    'meteor', 'meteor-client', 'meteorclient',
    'wurst', 'liquidbounce', 'bleachhack', 
    'aristois', 'mathax', 'impact', 'sigma', 
    'future', 'inertia', 'freecam', 'xray', 
    'baritone', 'seedcracker', 'kami', 'salhack',
    'ares', 'boze', 'doomsday', 'espresso', 
    'exodus', 'ghost', 'jigsaw', 'koks', 
    'lambda', 'novoline', 'rusherhack', 
    'thx', 'vape', 'zeroday', 'zumy'
];

// Resource packs x-ray detection
const CHEAT_RP_BLACKLIST = [
    'xray', 'x-ray', 'x_ray', 'cave-finder', 'ore-esp', 'esp', 'wallhack'
];

function isCheatKeyword(text) {
    if (!text) return false;
    const lower = text.toLowerCase();
    // Regex to match exact words or hyphenated words
    return CHEAT_MODS_BLACKLIST.some(keyword => {
        // Simple includes check, but we can make it stricter if needed
        return lower.includes(keyword);
    });
}

function scanModJar(jarPath) {
    if (!AdmZip) return null;
    try {
        const zip = new AdmZip(jarPath);
        const zipEntries = zip.getEntries();
        
        let metadata = null;
        
        // Find metadata files
        const fabricModJson = zipEntries.find(e => e.entryName === 'fabric.mod.json');
        const modsToml = zipEntries.find(e => e.entryName === 'META-INF/mods.toml');
        const quiltModJson = zipEntries.find(e => e.entryName === 'quilt.mod.json');
        
        if (fabricModJson) {
            const content = zip.readAsText(fabricModJson);
            metadata = JSON.parse(content);
            if (isCheatKeyword(metadata.id) || isCheatKeyword(metadata.name) || isCheatKeyword(metadata.description)) {
                return metadata.name || metadata.id || "Unknown Cheat Mod";
            }
        }
        
        if (quiltModJson) {
            const content = zip.readAsText(quiltModJson);
            metadata = JSON.parse(content);
            const id = metadata.quilt_loader?.id;
            const name = metadata.quilt_loader?.metadata?.name;
            const desc = metadata.quilt_loader?.metadata?.description;
            if (isCheatKeyword(id) || isCheatKeyword(name) || isCheatKeyword(desc)) {
                return name || id || "Unknown Cheat Mod";
            }
        }

        if (modsToml) {
            const content = zip.readAsText(modsToml);
            // TOML parsing is annoying without a lib, just string match for cheat names
            if (isCheatKeyword(content)) {
                return "Forge Cheat Mod";
            }
        }

        // Also check file name as fallback
        const fileName = path.basename(jarPath);
        if (isCheatKeyword(fileName)) {
            return fileName;
        }

        return null;
    } catch (e) {
        console.error("Failed to scan jar:", jarPath, e);
        return null; // Ignore unreadable jars
    }
}

function scanResourcePack(packPath) {
    if (!AdmZip) return null;
    try {
        // First check file name
        const fileName = path.basename(packPath).toLowerCase();
        if (CHEAT_RP_BLACKLIST.some(k => fileName.includes(k))) {
            return fileName;
        }

        // Only try to read if it's a file
        if (!fs.statSync(packPath).isFile()) return null;

        const zip = new AdmZip(packPath);
        const mcmeta = zip.getEntries().find(e => e.entryName === 'pack.mcmeta');
        
        if (mcmeta) {
            try {
                const content = zip.readAsText(mcmeta);
                const metadata = JSON.parse(content);
                const desc = metadata?.pack?.description;
                if (desc) {
                    const lowerDesc = desc.toString().toLowerCase();
                    if (CHEAT_RP_BLACKLIST.some(k => lowerDesc.includes(k))) {
                        return "X-Ray Resource Pack";
                    }
                }
            } catch (jsonErr) {
                // Ignore invalid JSON in pack.mcmeta, common in some packs
            }
        }
        
        return null;
    } catch (e) {
        // Silently skip malformed or unreadable zips
        return null;
    }
}

async function performAntiCheatScan(instancePath) {
    if (!AdmZip) return { success: false, error: "adm-zip non installé, impossible de scanner." };

    const isCheatMode = store.get('settings')?.cheatMode === true;
    if (isCheatMode) return { success: true }; // Allowed

    const modsPath = path.join(instancePath, 'mods');
    const rpPath = path.join(instancePath, 'resourcepacks');

    const blockedItems = [];

    // Helper for non-blocking loop
    const nonBlockingScan = async (items, pathPrefix, scanFunc, typeLabel) => {
        for (const item of items) {
            const result = scanFunc(path.join(pathPrefix, item));
            if (result) {
                blockedItems.push(`${typeLabel}: ${result} (${item})`);
            }
            // Give back control to the event loop after each file
            await new Promise(resolve => setImmediate(resolve));
        }
    };

    // Scan Mods
    if (fs.existsSync(modsPath)) {
        const jars = fs.readdirSync(modsPath).filter(f => f.endsWith('.jar'));
        await nonBlockingScan(jars, modsPath, scanModJar, "Mod");
    }

    // Scan Resource Packs
    if (fs.existsSync(rpPath)) {
        const zips = fs.readdirSync(rpPath).filter(f => f.endsWith('.zip'));
        await nonBlockingScan(zips, rpPath, scanResourcePack, "ResourcePack");
    }

    if (blockedItems.length > 0) {
        return {
            success: false,
            error: "Le Mode Legit a détecté des modifications non autorisées.\n" + blockedItems.join('\n')
        };
    }

    return { success: true };
}

function setupAntiCheatIpc() {
    ipcMain.handle('anticheat:scan', async (_, instancePath) => {
        return await performAntiCheatScan(instancePath);
    });
}

module.exports = { setupAntiCheatIpc, performAntiCheatScan, isCheatKeyword };
