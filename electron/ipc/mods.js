const { ipcMain } = require('electron');
const fs = require('fs');
const path = require('path');
const { getMinecraftPath, getInstancePath } = require('./launch');

let versionManifestPromise = null;
const ELYSIA_CLIENT_JAR = 'elysia-client-1.0.0.jar';

async function getVersionManifest() {
    if (!versionManifestPromise) {
        versionManifestPromise = fetch('https://launchermeta.mojang.com/mc/game/version_manifest.json')
            .then(async (response) => {
                if (!response.ok) throw new Error(`Failed to fetch versions (${response.status})`);
                return response.json();
            })
            .catch((error) => {
                versionManifestPromise = null;
                throw error;
            });
    }
    return versionManifestPromise;
}

async function resolveGameVersion(version) {
    if (!version || (version !== 'latest_release' && version !== 'latest_snapshot')) return version;
    const manifest = await getVersionManifest();
    if (!manifest?.latest) return version;
    return version === 'latest_snapshot' ? manifest.latest.snapshot : manifest.latest.release;
}

const MODRINTH_HEADERS = {
    'User-Agent': 'Elysia-Launcher/1.0.0 (contact@elysiastudios.net)',
    'Accept': 'application/json'
};

const ELYSIA_CLIENT_FEATURES = [
    { id: 'fabric-api', name: 'Fabric API', aliases: ['fabric-api'] },
    { id: 'modmenu', name: 'Mod Menu', aliases: ['modmenu', 'mod-menu'] },
    { id: 'cloth-config', name: 'Cloth Config API', aliases: ['cloth-config', 'cloth_config'] },
    { id: 'sodium', name: 'Sodium', aliases: ['sodium'] },
    { id: 'lithium', name: 'Lithium', aliases: ['lithium'] },
    { id: 'ferrite-core', name: 'FerriteCore', aliases: ['ferritecore', 'ferrite-core'] },
    { id: 'entityculling', name: 'Entity Culling', aliases: ['entityculling', 'entity-culling'] },
    { id: 'immediatelyfast', name: 'ImmediatelyFast', aliases: ['immediatelyfast', 'immediately-fast'] },
    { id: 'dynamic-fps', name: 'Dynamic FPS', aliases: ['dynamic_fps', 'dynamic-fps'] },
    { id: 'clumps', name: 'Clumps', aliases: ['clumps'] },
    { id: 'mouse-tweaks', name: 'Mouse Tweaks', aliases: ['mouse-tweaks', 'mousetweaks'] },
    { id: 'shulkerboxtooltip', name: 'ShulkerBoxTooltip', aliases: ['shulkerboxtooltip', 'shulker-box-tooltip'] },
    { id: 'appleskin', name: 'AppleSkin', aliases: ['appleskin'] },
    { id: 'zoomify', name: 'Zoomify', aliases: ['zoomify'] },
    { id: 'freelook', name: 'Freelook', aliases: ['freelook'] },
    { id: 'jade', name: 'Jade', aliases: ['jade', 'waila'] },
    { id: 'toggle-sprint', name: 'Toggle Sprint', aliases: ['toggle-sprint', 'togglesprint'] },
    { id: 'not-enough-animations', name: 'Not Enough Animations', aliases: ['not-enough-animations', 'notenoughanimations'] },
    { id: 'continuity', name: 'Continuity', aliases: ['continuity'] },
    { id: 'capes', name: 'Capes', aliases: ['capes'] },
    { id: 'iris', name: 'Iris Shaders', aliases: ['iris'] },
    { id: 'simple-voice-chat', name: 'Simple Voice Chat', aliases: ['simple-voice-chat', 'voicechat'] },
    { id: 'xaeros-minimap', name: "Xaero's Minimap", aliases: ['xaeros-minimap', 'xaerosminimap'] },
    { id: 'xaeros-world-map', name: "Xaero's World Map", aliases: ['xaeros-world-map', 'xaerosworldmap'] },
    { id: 'controllable', name: 'Controllable', aliases: ['controllable'] },
    { id: 'framework', name: 'Framework', aliases: ['framework'] },
    { id: 'sodium-extra', name: 'Sodium Extra', aliases: ['sodium-extra', 'sodiumextra'] },
    { id: 'reeses-sodium-options', name: "Reese's Sodium Options", aliases: ['reeses-sodium-options', 'reeses-sodium-options'] },
    { id: 'better-f3', name: 'BetterF3', aliases: ['better-f3', 'betterf3'] }
];

function getModLoader(loader) {
    return loader === 'elysiaclient' ? 'fabric' : (loader || 'vanilla');
}

function getInstanceLoader(loader) {
    return loader || 'vanilla';
}

function getTargetDir(version, loader, type = 'mod') {
    const instancePath = getInstancePath({ version, loader: { type: getInstanceLoader(loader) } });
    const folderName = type === 'resourcepack' ? 'resourcepacks' : (type === 'shader' ? 'shaderpacks' : 'mods');
    return path.join(instancePath, folderName);
}

function uniquePaths(paths) {
    return [...new Set(paths.filter(Boolean).map(p => path.normalize(p)))];
}

function findBundledElysiaClientJar() {
    const resourcesPath = process.resourcesPath || '';
    const candidates = uniquePaths([
        path.join(__dirname, '../../Elysia-Client/build/libs', ELYSIA_CLIENT_JAR),
        path.join(process.cwd(), 'Elysia-Client/build/libs', ELYSIA_CLIENT_JAR),
        resourcesPath && path.join(resourcesPath, 'app.asar.unpacked/Elysia-Client/build/libs', ELYSIA_CLIENT_JAR),
        resourcesPath && path.join(resourcesPath, 'app/Elysia-Client/build/libs', ELYSIA_CLIENT_JAR),
        resourcesPath && path.join(resourcesPath, 'Elysia-Client/build/libs', ELYSIA_CLIENT_JAR)
    ]);

    return candidates.find(candidate => fs.existsSync(candidate)) || null;
}

function sameFileContent(sourcePath, targetPath) {
    if (!fs.existsSync(sourcePath) || !fs.existsSync(targetPath)) return false;
    const sourceStat = fs.statSync(sourcePath);
    const targetStat = fs.statSync(targetPath);
    if (sourceStat.size !== targetStat.size) return false;
    return fs.readFileSync(sourcePath).equals(fs.readFileSync(targetPath));
}

function cleanupOldElysiaClientJars(modsDir, targetPath) {
    if (!fs.existsSync(modsDir)) return 0;

    let removed = 0;
    const targetName = path.basename(targetPath).toLowerCase();
    const files = fs.readdirSync(modsDir).filter(file => {
        const lower = file.toLowerCase();
        return lower !== targetName && lower.endsWith('.jar') && /^elysia-client(?:[-_].*)?\.jar$/.test(lower);
    });

    for (const file of files) {
        fs.unlinkSync(path.join(modsDir, file));
        removed++;
    }

    return removed;
}

async function installElysiaClientJar({ version, loader = 'elysiaclient', mainWindow }) {
    const sourcePath = findBundledElysiaClientJar();
    const targetDir = getTargetDir(version, loader, 'mod');
    if (!fs.existsSync(targetDir)) fs.mkdirSync(targetDir, { recursive: true });

    const targetPath = path.join(targetDir, ELYSIA_CLIENT_JAR);
    if (!sourcePath) {
        if (fs.existsSync(targetPath)) {
            mainWindow?.webContents.send('launch:data', `[Elysia] Jar source introuvable, copie deja presente conservee: ${ELYSIA_CLIENT_JAR}.`);
            return { success: true, installed: false, removed: 0, sourcePath: null, targetPath };
        }

        const error = `Jar ${ELYSIA_CLIENT_JAR} introuvable. Lance d'abord le build du mod ou rebuild le launcher.`;
        mainWindow?.webContents.send('launch:data', `[Elysia] ${error}`);
        return { success: false, error };
    }

    const removed = cleanupOldElysiaClientJars(targetDir, targetPath);
    const alreadyCurrent = sameFileContent(sourcePath, targetPath);

    if (!alreadyCurrent) {
        fs.copyFileSync(sourcePath, targetPath);
    }

    const state = alreadyCurrent ? 'deja a jour' : 'installe';
    const cleanupText = removed > 0 ? `, ${removed} ancienne(s) copie(s) retiree(s)` : '';
    mainWindow?.webContents.send('launch:data', `[Elysia] Elysia Client ${state}: ${ELYSIA_CLIENT_JAR}${cleanupText}.`);

    return {
        success: true,
        installed: !alreadyCurrent,
        removed,
        sourcePath,
        targetPath
    };
}

function readInstalledModIndex(modsDir) {
    const index = {
        modIds: new Set(),
        fileNames: new Set()
    };

    if (!fs.existsSync(modsDir)) return index;

    const jars = fs.readdirSync(modsDir).filter(file => file.toLowerCase().endsWith('.jar'));
    for (const jar of jars) {
        index.fileNames.add(jar.toLowerCase());

        try {
            const AdmZip = require('adm-zip');
            const zip = new AdmZip(path.join(modsDir, jar));
            const fabricJson = zip.getEntries().find(entry => entry.entryName === 'fabric.mod.json');
            if (!fabricJson) continue;

            const metadata = JSON.parse(zip.readAsText(fabricJson));
            if (metadata?.id) index.modIds.add(String(metadata.id).toLowerCase());
        } catch (error) {
            // Filename matching below still gives us a useful duplicate guard.
        }
    }

    return index;
}

function isFeatureInstalled(feature, installedIndex) {
    const aliases = [feature.id, feature.name, ...(feature.aliases || [])]
        .filter(Boolean)
        .map(alias => String(alias).toLowerCase().replace(/\s+/g, '-'));

    return aliases.some(alias => {
        const normalizedAlias = alias.replace(/[-_]/g, '');
        if (installedIndex.modIds.has(alias) || installedIndex.modIds.has(normalizedAlias)) return true;

        for (const fileName of installedIndex.fileNames) {
            const normalizedFile = fileName.replace(/[-_]/g, '');
            if (fileName.includes(alias) || normalizedFile.includes(normalizedAlias)) return true;
        }

        return false;
    });
}

// CurseForge API
const CF_API_KEY = '$2a$10$QePo4Knwx86iLSKHDVZw9ehffCfNOUpm8j9zC259JJGQRTw7koWLa';
const CF_HEADERS = {
    'Accept': 'application/json',
    'x-api-key': CF_API_KEY
};
const CF_BASE = 'https://api.curseforge.com/v1';

// CurseForge class IDs
const CF_CLASS_IDS = {
    mod: 6,
    resourcepack: 12,
    shader: 6552
};

// CurseForge loader type IDs
const CF_LOADER_IDS = {
    forge: 1,
    fabric: 4,
    quilt: 5,
    neoforge: 6
};

function normalizeVersionTag(version) {
    return String(version || '')
        .trim()
        .toLowerCase()
        .replace(/^v/, '')
        .replace(/[_-]/g, '.');
}

function getVersionFamily(version) {
    const normalized = normalizeVersionTag(version);
    const parts = normalized.split('.');
    return parts.length >= 2 ? parts.slice(0, 2).join('.') : normalized;
}

function extractVersionTokens(text) {
    const matches = String(text || '').match(/\d+(?:[._-]\d+)+/g);
    return matches ? matches.map(normalizeVersionTag) : [];
}

function curseForgeFileHasLoader(file, modLoader) {
    if (!modLoader || !CF_LOADER_IDS[modLoader]) return true;
    return (file.gameVersions || []).some(v => String(v).toLowerCase() === modLoader.toLowerCase());
}

function scoreCurseForgeFile(file, resolvedVersion, modLoader) {
    if (!curseForgeFileHasLoader(file, modLoader)) return Number.NEGATIVE_INFINITY;

    const targetVersion = normalizeVersionTag(resolvedVersion);
    if (!targetVersion) {
        return typeof file.id === 'number' ? file.id : 0;
    }

    const targetFamily = getVersionFamily(targetVersion);
    const declaredVersions = (file.gameVersions || []).map(normalizeVersionTag);
    const filenameVersions = extractVersionTokens(file.fileName);

    const exactMatch =
        declaredVersions.includes(targetVersion) ||
        filenameVersions.includes(targetVersion);

    if (exactMatch) {
        return 10000 + (typeof file.id === 'number' ? file.id : 0);
    }

    const hasFamilyMatch = declaredVersions.includes(targetFamily);
    if (!hasFamilyMatch) {
        return Number.NEGATIVE_INFINITY;
    }

    const hasConflictingSpecificPatch = filenameVersions.some(token =>
        token.startsWith(`${targetFamily}.`) && token !== targetVersion
    );

    if (hasConflictingSpecificPatch) {
        return Number.NEGATIVE_INFINITY;
    }

    return 1000 + (typeof file.id === 'number' ? file.id : 0);
}

function selectBestCurseForgeFile(files, resolvedVersion, modLoader) {
    return (files || [])
        .map(file => ({ file, score: scoreCurseForgeFile(file, resolvedVersion, modLoader) }))
        .filter(entry => Number.isFinite(entry.score) && entry.score > Number.NEGATIVE_INFINITY)
        .sort((a, b) => b.score - a.score)
        .map(entry => entry.file)[0] || null;
}

function normalizeCFResult(mod) {
    return {
        project_id: `cf-${mod.id}`,
        slug: mod.slug,
        title: mod.name,
        description: mod.summary,
        icon_url: mod.logo?.thumbnailUrl || '',
        downloads: mod.downloadCount || 0,
        author: mod.authors?.[0]?.name || 'Inconnu',
        source: 'curseforge'
    };
}

async function searchCurseForge(query, version, loader, type = 'mod') {
    try {
        const modLoader = getModLoader(loader);
        const classId = CF_CLASS_IDS[type] || 6;
        const params = new URLSearchParams({
            gameId: '432', // Minecraft
            classId: String(classId),
            searchFilter: query,
            pageSize: '20',
            sortField: '2', // Popularity
            sortOrder: 'desc'
        });

        if (version) params.set('gameVersion', version);
        if (modLoader && CF_LOADER_IDS[modLoader]) params.set('modLoaderType', String(CF_LOADER_IDS[modLoader]));

        const res = await fetch(`${CF_BASE}/mods/search?${params}`, { headers: CF_HEADERS });
        if (!res.ok) return null;
        const json = await res.json();
        return json.data?.map(normalizeCFResult) || [];
    } catch (e) {
        console.error('CurseForge search error:', e.message);
        return null;
    }
}

async function installFromCurseForge(cfId, version, loader, type = 'mod', mainWindow) {
    const resolvedVersion = await resolveGameVersion(version);
    const modLoader = getModLoader(loader);
    
    // Get mod files
    const res = await fetch(`${CF_BASE}/mods/${cfId}/files?gameVersion=${resolvedVersion}&pageSize=50`, { headers: CF_HEADERS });
    if (!res.ok) throw new Error(`CurseForge API Error: ${res.status}`);
    
    const json = await res.json();
    let files = json.data || [];

    // Filter by loader
    if (modLoader && CF_LOADER_IDS[modLoader]) {
        const filtered = files.filter(f => f.gameVersions?.some(v => 
            v.toLowerCase() === modLoader.toLowerCase()
        ));
        if (filtered.length > 0) files = filtered;
    }

    if (files.length === 0) {
        // Try without version filter
        const res2 = await fetch(`${CF_BASE}/mods/${cfId}/files?pageSize=10`, { headers: CF_HEADERS });
        if (res2.ok) {
            const json2 = await res2.json();
            files = json2.data || [];
        }
    }

    if (files.length === 0) throw new Error('Aucun fichier compatible trouvé sur CurseForge.');

    const file = files[0]; // Most recent compatible
    const downloadUrl = file.downloadUrl;
    
    if (!downloadUrl) throw new Error('Ce mod CurseForge ne permet pas le téléchargement direct.');

    const targetDir = getTargetDir(version, loader, type);
    if (!fs.existsSync(targetDir)) fs.mkdirSync(targetDir, { recursive: true });

    const filePath = path.join(targetDir, file.fileName);

    mainWindow?.webContents.send('launch:progress', {
        progress: 0, size: file.fileLength, type: `CurseForge: ${file.fileName}`, percent: 0
    });

    const dl = await fetch(downloadUrl, { headers: CF_HEADERS });
    if (!dl.ok) throw new Error(`Download failed: ${dl.status}`);
    const buffer = Buffer.from(await dl.arrayBuffer());
    fs.writeFileSync(filePath, buffer);

    mainWindow?.webContents.send('launch:progress', {
        progress: file.fileLength, size: file.fileLength, type: `Installé: ${file.fileName}`, percent: 100
    });

    return { success: true };
}

async function installFromCurseForgeStrict(cfId, version, loader, type = 'mod', mainWindow) {
    const resolvedVersion = await resolveGameVersion(version);
    const modLoader = getModLoader(loader);

    const fetchFiles = async (query = '') => {
        const res = await fetch(`${CF_BASE}/mods/${cfId}/files?pageSize=200${query}`, { headers: CF_HEADERS });
        if (!res.ok) throw new Error(`CurseForge API Error: ${res.status}`);
        const json = await res.json();
        return json.data || [];
    };

    let files = await fetchFiles(resolvedVersion ? `&gameVersion=${encodeURIComponent(resolvedVersion)}` : '');
    let file = selectBestCurseForgeFile(files, resolvedVersion, modLoader);

    if (!file) {
        files = await fetchFiles();
        file = selectBestCurseForgeFile(files, resolvedVersion, modLoader);
    }

    if (!file) {
        const loaderLabel = modLoader && modLoader !== 'vanilla' ? ` ${modLoader}` : '';
        throw new Error(`Aucun fichier CurseForge compatible trouve pour ${resolvedVersion}${loaderLabel}.`);
    }

    const downloadUrl = file.downloadUrl;
    if (!downloadUrl) throw new Error('Ce mod CurseForge ne permet pas le telechargement direct.');

    const targetDir = getTargetDir(version, loader, type);
    if (!fs.existsSync(targetDir)) fs.mkdirSync(targetDir, { recursive: true });

    const filePath = path.join(targetDir, file.fileName);

    mainWindow?.webContents.send('launch:progress', {
        progress: 0, size: file.fileLength, type: `CurseForge: ${file.fileName}`, percent: 0
    });

    const dl = await fetch(downloadUrl, { headers: CF_HEADERS });
    if (!dl.ok) throw new Error(`Download failed: ${dl.status}`);
    const buffer = Buffer.from(await dl.arrayBuffer());
    fs.writeFileSync(filePath, buffer);

    mainWindow?.webContents.send('launch:progress', {
        progress: file.fileLength, size: file.fileLength, type: `Installe: ${file.fileName}`, percent: 100
    });

    return { success: true };
}

async function internalInstallMod(projectId, version, loader, type = 'mod', installedSet = new Set(), mainWindow) {
    if (installedSet.has(projectId)) return { success: true, skipped: true };
    installedSet.add(projectId);

    // CurseForge install
    if (typeof projectId === 'string' && projectId.startsWith('cf-')) {
        const cfId = projectId.replace('cf-', '');
        return await installFromCurseForgeStrict(cfId, version, loader, type, mainWindow);
    }

    const resolvedVersion = await resolveGameVersion(version);
    const modLoader = getModLoader(loader);
    
    // 1. Get Versions from Modrinth
    const versionsUrl = `https://api.modrinth.com/v2/project/${projectId}/version`;
    const res = await fetch(versionsUrl, { headers: MODRINTH_HEADERS });
    if (!res.ok) throw new Error(`API Error: ${res.status} for ${projectId}`);
    
    const allVersions = await res.json();
    const compatibleVersions = allVersions.filter(v => {
        const hasLoader = type !== 'mod' || v.loaders.includes(modLoader.toLowerCase());
        if (!hasLoader) return false;
        if (v.game_versions.includes(resolvedVersion)) return true;
        const major = resolvedVersion.split('.').slice(0, 2).join('.');
        return v.game_versions.includes(major) || v.game_versions.some(gv => gv.includes('*') && resolvedVersion.startsWith(gv.replace('.*', '')));
    });

    if (compatibleVersions.length === 0) return { success: false, error: `Incompatible: ${projectId}` };
    
    const latest = compatibleVersions[0];
    const file = latest.files.find(f => f.primary) || latest.files[0];
    if (!file) return { success: false, error: 'No files' };

    // 2. Download Dependencies FIRST
    if (latest.dependencies) {
        for (const dep of latest.dependencies) {
            if (dep.dependency_type === 'required') {
                const depProjId = dep.project_id;
                if (depProjId) {
                    await internalInstallMod(depProjId, version, loader, type, installedSet, mainWindow).catch(e => console.warn('Dep failed:', e));
                }
            }
        }
    }

    // 3. Download actual mod
    const targetDir = getTargetDir(version, loader, type);
    if (!fs.existsSync(targetDir)) fs.mkdirSync(targetDir, { recursive: true });

    const filePath = path.join(targetDir, file.filename);
    
    mainWindow?.webContents.send('launch:progress', {
        progress: 0, size: file.size, type: `Installation: ${file.filename}`, percent: 0
    });

    const dl = await fetch(file.url, { headers: MODRINTH_HEADERS });
    if (!dl.ok) throw new Error(`Download failed: ${dl.status}`);
    const buffer = Buffer.from(await dl.arrayBuffer());
    fs.writeFileSync(filePath, buffer);

    mainWindow?.webContents.send('launch:progress', {
        progress: file.size, size: file.size, type: `Installé: ${file.filename}`, percent: 100
    });

    return { success: true };
}

async function installElysiaFeaturePack({ version, loader = 'elysiaclient', mainWindow }) {
    const targetDir = getTargetDir(version, loader, 'mod');
    if (!fs.existsSync(targetDir)) fs.mkdirSync(targetDir, { recursive: true });

    const installedSet = new Set();
    const summary = {
        installed: 0,
        skipped: 0,
        failed: []
    };

    mainWindow?.webContents.send('launch:data', `[Elysia] Préparation du pack ElysiaClient (${ELYSIA_CLIENT_FEATURES.length} features)...`);

    // --- OPTIMIZATION: Read index ONLY ONCE before the loop ---
    const installedIndex = readInstalledModIndex(targetDir);

    for (const feature of ELYSIA_CLIENT_FEATURES) {
        if (isFeatureInstalled(feature, installedIndex)) {
            summary.skipped++;
            // Only send log for important skipped features to reduce IPC traffic
            if (feature.id === 'fabric-api' || feature.id === 'sodium') {
                mainWindow?.webContents.send('launch:data', `[Elysia] ${feature.name} déjà présent.`);
            }
            continue;
        }

        mainWindow?.webContents.send('launch:data', `[Elysia] Installation auto: ${feature.name}...`);

        try {
            const result = await internalInstallMod(feature.id, version, loader, 'mod', installedSet, mainWindow);
            if (result.success) {
                summary.installed++;
            } else {
                summary.failed.push({ name: feature.name, error: result.error || 'Incompatible' });
                mainWindow?.webContents.send('launch:data', `[Elysia] ${feature.name} ignoré: ${result.error || 'incompatible'}.`);
            }
        } catch (error) {
            summary.failed.push({ name: feature.name, error: error.message });
            mainWindow?.webContents.send('launch:data', `[Elysia] ${feature.name} ignoré: ${error.message}.`);
        }
    }

    mainWindow?.webContents.send('launch:data', `[Elysia] Pack prêt: ${summary.installed} installé(s), ${summary.skipped} déjà présent(s), ${summary.failed.length} ignoré(s).`);
    return { success: true, ...summary };
}

async function installControllerMods({ version, loader, mainWindow }) {
    const targetDir = getTargetDir(version, loader, 'mod');
    if (!fs.existsSync(targetDir)) fs.mkdirSync(targetDir, { recursive: true });

    // --- OPTIMIZATION: Read index ONLY ONCE ---
    const installedIndex = readInstalledModIndex(targetDir);
    const installedSet = new Set();

    const modsToInstall = [
        { id: 'cf-317269', name: 'Controllable', feature: { id: 'controllable', name: 'Controllable' } },
        { id: 'cf-549225', name: 'Framework', feature: { id: 'framework', name: 'Framework' } }
    ];

    mainWindow?.webContents.send('launch:data', `[Elysia] Vérification du Mode Manette pour ${loader}...`);

    for (const mod of modsToInstall) {
        if (isFeatureInstalled(mod.feature, installedIndex)) {
            continue;
        }

        try {
            mainWindow?.webContents.send('launch:data', `[Elysia] Installation de ${mod.name}...`);
            const res = await internalInstallMod(mod.id, version, loader, 'mod', installedSet, mainWindow);
            if (res.success) {
                mainWindow?.webContents.send('launch:data', `[Elysia] ${mod.name} installé.`);
            }
        } catch (e) {
            mainWindow?.webContents.send('launch:data', `[Elysia] Erreur pour ${mod.name}: ${e.message}`);
        }
    }

    return { success: true };
}

function setupModsIpc(mainWindow) {
    ipcMain.handle('mods:search', async (_, { query, version, loader, type = 'mod' }) => {
        try {
            const resolvedVersion = await resolveGameVersion(version);
            const modLoader = getModLoader(loader);
            
            // --- MODRINTH SEARCH ---
            const performQuery = async (v) => {
                const facets = [[`project_type:${type}`]];
                if (type === 'mod' && modLoader && modLoader !== 'vanilla') facets.push([`categories:${modLoader}`]);
                if (v) facets.push([`versions:${v}`]);
                const url = `https://api.modrinth.com/v2/search?query=${encodeURIComponent(query)}&limit=20&facets=${encodeURIComponent(JSON.stringify(facets))}`;
                const res = await fetch(url, { headers: MODRINTH_HEADERS });
                return res.ok ? await res.json() : null;
            };

            let data = await performQuery(resolvedVersion);
            if ((!data || data.hits.length === 0) && resolvedVersion?.includes('.')) {
                data = await performQuery(resolvedVersion.split('.').slice(0, 2).join('.'));
            }
            if (!data || data.hits.length === 0) data = await performQuery(null);

            // Tag modrinth results
            if (data?.hits) {
                data.hits = data.hits.map(h => ({ ...h, source: 'modrinth' }));
            }

            // --- CURSEFORGE FALLBACK ---
            const modrinthCount = data?.hits?.length || 0;
            if (modrinthCount < 5) {
                const cfResults = await searchCurseForge(query, resolvedVersion, modLoader, type);
                if (cfResults && cfResults.length > 0) {
                    // Deduplicate by title (case-insensitive)
                    const existingTitles = new Set((data?.hits || []).map(h => h.title?.toLowerCase()));
                    const unique = cfResults.filter(cf => !existingTitles.has(cf.title?.toLowerCase()));
                    
                    if (!data) data = { hits: [], total_hits: 0 };
                    data.hits.push(...unique);
                    data.total_hits = data.hits.length;
                }
            }

            return { success: true, data };
        } catch (error) {
            return { success: false, error: error.message };
        }
    });

    ipcMain.handle('mods:install', async (_, { projectId, version, loader, type = 'mod' }) => {
        try {
            const modLoader = getModLoader(loader);
            if (type === 'mod' && (!modLoader || modLoader === 'vanilla')) return { success: false, error: 'Select a Loader first.' };
            return await internalInstallMod(projectId, version, loader, type, new Set(), mainWindow);
        } catch (error) {
            return { success: false, error: error.message };
        }
    });

    ipcMain.handle('mods:list', async (_, { version, loader, type = 'mod' }) => {
        try {
            const targetDir = getTargetDir(version, loader, type);
            if (!fs.existsSync(targetDir)) return { success: true, mods: [] };
            const mods = fs.readdirSync(targetDir).filter(f => f.endsWith('.jar') || f.endsWith('.zip'));
            return { success: true, mods };
        } catch (error) {
            return { success: false, error: error.message };
        }
    });

    ipcMain.handle('mods:delete', async (_, { filename, version, loader, type = 'mod' }) => {
        try {
            const filePath = path.join(getTargetDir(version, loader, type), filename);
            if (fs.existsSync(filePath)) fs.unlinkSync(filePath);
            return { success: true };
        } catch (error) {
            return { success: false, error: error.message };
        }
    });
    ipcMain.handle('mods:clean', async (_, { version, loader, type = 'mod' }) => {
        try {
            const targetDir = getTargetDir(version, loader, type);
            if (!fs.existsSync(targetDir)) return { success: true, deleted: 0 };
            const files = fs.readdirSync(targetDir).filter(f => f.endsWith('.jar') || f.endsWith('.zip'));
            files.forEach(f => fs.unlinkSync(path.join(targetDir, f)));
            return { success: true, deleted: files.length };
        } catch (error) {
            return { success: false, error: error.message };
        }
    });

    ipcMain.handle('mods:exportPack', async (event, { version, loader }) => {
        try {
            const { dialog } = require('electron');
            const { exec } = require('child_process');
            const instancePath = getInstancePath({ version, loader: { type: loader || 'vanilla' } });
            const modsDir = path.join(instancePath, 'mods');
            
            if (!fs.existsSync(modsDir) || fs.readdirSync(modsDir).length === 0) {
                return { success: false, error: 'Aucun mod à exporter.' };
            }

            const win = require('electron').BrowserWindow.fromWebContents(event.sender);
            const savePath = dialog.showSaveDialogSync(win, {
                title: 'Exporter le Modpack',
                defaultPath: path.join(require('os').homedir(), 'Desktop', `Elysia_Modpack_${loader}_${version}.zip`),
                filters: [{ name: 'ZIP Archives', extensions: ['zip'] }]
            });

            if (!savePath) return { success: false, skipped: true };

            return new Promise((resolve) => {
                // Remove existing if any
                if (fs.existsSync(savePath)) fs.unlinkSync(savePath);
                
                // Use PowerShell Compress-Archive (built-in Windows)
                const cmd = `powershell.exe -NoProfile -Command "Compress-Archive -Path '${modsDir}\\*' -DestinationPath '${savePath}' -Force"`;
                exec(cmd, (error) => {
                    if (error) resolve({ success: false, error: error.message });
                    else resolve({ success: true, path: savePath });
                });
            });
        } catch (error) {
            return { success: false, error: error.message };
        }
    });

    ipcMain.handle('mods:importPack', async (event, { version, loader }) => {
        try {
            const { dialog } = require('electron');
            const { exec } = require('child_process');
            const instancePath = getInstancePath({ version, loader: { type: loader || 'vanilla' } });
            const modsDir = path.join(instancePath, 'mods');

            const win = require('electron').BrowserWindow.fromWebContents(event.sender);
            const filePaths = dialog.showOpenDialogSync(win, {
                title: 'Importer un Modpack',
                filters: [{ name: 'ZIP Archives', extensions: ['zip'] }],
                properties: ['openFile']
            });

            if (!filePaths || filePaths.length === 0) return { success: false, skipped: true };
            const zipPath = filePaths[0];

            if (!fs.existsSync(modsDir)) {
                fs.mkdirSync(modsDir, { recursive: true });
            }

            return new Promise((resolve) => {
                // Use PowerShell Expand-Archive
                const cmd = `powershell.exe -NoProfile -Command "Expand-Archive -Path '${zipPath}' -DestinationPath '${modsDir}' -Force"`;
                exec(cmd, (error) => {
                    if (error) resolve({ success: false, error: error.message });
                    else resolve({ success: true });
                });
            });
        } catch (error) {
            return { success: false, error: error.message };
        }
    });
}

module.exports = { setupModsIpc, internalInstallMod, installElysiaClientJar, installElysiaFeaturePack, installControllerMods };
