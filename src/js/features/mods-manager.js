import { state } from '../core/state.js';
import { notify, showProgress } from '../core/utils.js';

// Anti-Cheat: Frontend blacklist (mirrors backend)
const CHEAT_KEYWORDS = [
    'meteor', 'meteor-client', 'wurst', 'liquidbounce', 'bleachhack',
    'aristois', 'mathax', 'impact', 'sigma', 'future', 'inertia',
    'freecam', 'xray', 'x-ray', 'baritone', 'seedcracker', 'kami',
    'salhack', 'ares', 'boze', 'doomsday', 'espresso', 'exodus',
    'ghost', 'jigsaw', 'koks', 'lambda', 'novoline', 'rusherhack',
    'thx', 'vape', 'zeroday', 'zumy', 'hacked client', 'cheat',
    'hack client', 'killaura', 'nuker', 'esp', 'wallhack'
];

function isCheatQuery(query) {
    const lower = query.toLowerCase();
    return CHEAT_KEYWORDS.some(k => lower.includes(k));
}

function isCheatMod(mod) {
    const slug = (mod.slug || '').toLowerCase();
    const title = (mod.title || '').toLowerCase();
    const desc = (mod.description || '').toLowerCase();
    return CHEAT_KEYWORDS.some(k => slug.includes(k) || title.includes(k) || desc.includes(k));
}

let modsSearchTimeout = null;

export function initModsUI(prefix = 'mods', modrinthType = 'mod') {
    const searchInput = document.getElementById(`input-${prefix}-search`);
    const tabPacks = document.getElementById(`tab-${prefix}-packs`);
    const tabSearch = document.getElementById(`tab-${prefix}-search`);
    const tabInstalled = document.getElementById(`tab-${prefix}-installed`);
    
    const viewPacks = document.getElementById(`${prefix}-packs-view`);
    const viewSearch = document.getElementById(`${prefix}-search-view`);
    const viewInstalled = document.getElementById(`${prefix}-installed-view`);

    // Hide packs tab if not on mods page
    if (prefix !== 'mods' && tabPacks) {
        tabPacks.style.display = 'none';
        if (tabSearch) setTimeout(() => tabSearch.click(), 10);
    }

    function showView(activeTab, activeView) {
        [tabPacks, tabSearch, tabInstalled].forEach(t => t?.classList.remove('active'));
        [viewPacks, viewSearch, viewInstalled].forEach(v => { if (v) v.style.display = 'none'; });
        activeTab?.classList.add('active');
        if (activeView) activeView.style.display = 'block';
    }

    tabPacks?.addEventListener('click', () => showView(tabPacks, viewPacks));
    tabSearch?.addEventListener('click', () => showView(tabSearch, viewSearch));
    tabInstalled?.addEventListener('click', () => {
        showView(tabInstalled, viewInstalled);
        loadInstalledMods(prefix, modrinthType);
    });

    // Pre-fetch count for the tab badge
    (async () => {
        const s = await window.electronAPI.getSettings();
        const v = s.version || '1.21.4';
        const l = s.loader?.type || 'vanilla';
        const r = await window.electronAPI.getInstalledMods(v, l, modrinthType);
        const countSpan = document.getElementById(`installed-${prefix}-count`) || document.getElementById('installed-mods-count');
        if (countSpan && r.success) countSpan.textContent = r.mods.length;
    })();

    // Search logic
    searchInput?.addEventListener('input', (e) => {
        const query = e.target.value.trim();
        if (modsSearchTimeout) clearTimeout(modsSearchTimeout);
        
        if (query) {
            showView(tabSearch, viewSearch);
            modsSearchTimeout = setTimeout(() => performSearch(query, prefix, modrinthType), 500);
        } else {
            const resultsContainer = document.getElementById(`${prefix}-search-results`);
            if (resultsContainer) {
                let term = prefix === 'resourcepacks' ? 'ressources' : (prefix === 'shaders' ? 'shaders' : 'mods');
                resultsContainer.innerHTML = `
                    <div class="mods-empty" style="padding: 40px; text-align: center;">
                        <div style="font-size: 40px; margin-bottom: 16px;">🔍</div>
                        <div style="font-weight: 600; color: #fff; margin-bottom: 8px;">Explorez l'univers des ${term}</div>
                        <div style="color: var(--text-muted); font-size: 13px;">Tapez un nom ci-dessus pour découvrir de nouveaux ${term} compatibles avec votre version.</div>
                    </div>`;
            }
        }
    });

    // Import / Export handlers
    const btnExport = document.getElementById('btn-export-pack');
    const btnImport = document.getElementById('btn-import-pack');

    if (btnExport) {
        btnExport.onclick = async () => {
            const s = await window.electronAPI.getSettings();
            const v = s.version || '1.21.4';
            const l = s.loader?.type || 'vanilla';
            
            const ogHtml = btnExport.innerHTML;
            btnExport.innerHTML = '<div class="spinner" style="width:14px;height:14px;border-width:2px;margin-right:8px;"></div> Exportation...';
            btnExport.disabled = true;

            const res = await window.electronAPI.exportModpack(v, l);
            
            btnExport.innerHTML = ogHtml;
            btnExport.disabled = false;

            if (res.success) {
                notify(`Modpack exporté avec succès !`, 'success');
            } else if (!res.skipped) {
                notify(`Erreur lors de l'export: ${res.error}`, 'error');
            }
        };
    }

    if (btnImport) {
        btnImport.onclick = async () => {
            const s = await window.electronAPI.getSettings();
            const v = s.version || '1.21.4';
            const l = s.loader?.type || 'vanilla';
            
            const ogHtml = btnImport.innerHTML;
            btnImport.innerHTML = '<div class="spinner" style="width:14px;height:14px;border-width:2px;margin-right:8px;"></div> Importation...';
            btnImport.disabled = true;

            const res = await window.electronAPI.importModpack(v, l);
            
            btnImport.innerHTML = ogHtml;
            btnImport.disabled = false;

            if (res.success) {
                notify(`Modpack importé avec succès !`, 'success');
                if (tabInstalled && tabInstalled.classList.contains('active')) {
                    loadInstalledMods(prefix, modrinthType);
                }
            } else if (!res.skipped) {
                notify(`Erreur lors de l'import: ${res.error}`, 'error');
            }
        };
    }
}

async function performSearch(query, prefix, type) {
    const resultsContainer = document.getElementById(`${prefix}-search-results`);
    if (!resultsContainer) return;

    // Anti-Cheat: Block searches for known cheat terms in Legit mode
    const currentSettings = state.get('settings') || {};
    const isLegitMode = !currentSettings.cheatMode;

    if (isLegitMode && isCheatQuery(query)) {
        resultsContainer.innerHTML = `
            <div class="mods-empty" style="padding: 40px; text-align: center;">
                <div style="font-size: 40px; margin-bottom: 16px;">🛡️</div>
                <div style="font-weight: 700; color: var(--danger); margin-bottom: 8px;">Bloqué par la Sécurité Elysia</div>
                <div style="color: var(--text-muted); font-size: 13px; max-width: 350px; margin: 0 auto;">Ce mod est considéré comme du cheat et est bloqué en Mode Legit.<br>Vous pouvez activer le Mode Anarchie dans les Paramètres si vous acceptez les risques.</div>
            </div>`;
        return;
    }

    resultsContainer.innerHTML = '<div class="spinner-container"><div class="spinner"></div></div>';

    const settings = await window.electronAPI.getSettings();
    const version = settings.version || '1.21.4';
    const loader = settings.loader?.type || 'vanilla';

    const res = await window.electronAPI.searchMods(query, version, loader, type);
    
    if (!res.success) {
        resultsContainer.innerHTML = `<div class="mods-empty">Erreur: ${res.error}</div>`;
        return;
    }

    let hits = res.data?.hits || [];

    // Anti-Cheat: Silently filter out cheat mods from results even if the query was legit
    if (isLegitMode) {
        hits = hits.filter(mod => !isCheatMod(mod));
    }

    if (hits.length === 0) {
        resultsContainer.innerHTML = '<div class="mods-empty">Aucun résultat trouvé.</div>';
        return;
    }

    resultsContainer.innerHTML = '';
    hits.forEach(mod => {
        const card = document.createElement('div');
        card.className = 'mod-card-search';
        const isCF = mod.source === 'curseforge';
        const sourceBadge = isCF
            ? '<span style="font-size:9px;background:#f16436;color:#fff;padding:1px 5px;border-radius:3px;font-weight:bold;margin-left:6px;">CurseForge</span>'
            : '<span style="font-size:9px;background:#1bd96a;color:#111;padding:1px 5px;border-radius:3px;font-weight:bold;margin-left:6px;">Modrinth</span>';
        card.innerHTML = `
            <div class="mod-search-icon">
                <img src="${mod.icon_url || 'https://cdn-raw.modrinth.com/placeholder.svg'}" alt="icon">
            </div>
            <div class="mod-search-info">
                <h4>${mod.title}${sourceBadge}</h4>
                <p>${mod.description}</p>
                <div class="mod-search-meta">Par ${mod.author} • ${mod.downloads.toLocaleString()} downloads</div>
            </div>
            <button class="btn-install-mod" data-id="${mod.project_id}">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="8 17 12 21 16 17"/><line x1="12" y1="12" x2="12" y2="21"/><path d="M20.88 18.09A5 5 0 0 0 18 9h-1.26A8 8 0 1 0 3 16.29"/></svg>
            </button>
        `;

        card.querySelector('.btn-install-mod').addEventListener('click', async (e) => {
            const btn = e.currentTarget;
            const projectId = mod.project_id || mod.project_id_or_slug;
            
            btn.disabled = true;
            btn.innerHTML = '<div class="spinner spinner-xs"></div>';
            
            const insRes = await window.electronAPI.installMod(projectId, version, loader, type);
            if (insRes.success) {
                notify(`${mod.title} installé !`, 'success');
                btn.innerHTML = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="20 6 9 17 4 12"></polyline></svg>';
            } else {
                notify(`Erreur: ${insRes.error}`, 'error');
                btn.disabled = false;
                btn.innerHTML = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="8 17 12 21 16 17"/><line x1="12" y1="12" x2="12" y2="21"/><path d="M20.88 18.09A5 5 0 0 0 18 9h-1.26A8 8 0 1 0 3 16.29"/></svg>';
            }
        });

        resultsContainer.appendChild(card);
    });
}

export async function initInstanceManager() {
    const modal = document.getElementById('modal-instance-selector');
    const list = document.getElementById('instances-list');
    const btnRefresh = document.getElementById('btn-refresh-instances');

    const refresh = async () => {
        const info = await window.electronAPI.getInstanceInfo();
        const pathEls = document.querySelectorAll('[id^="current-instance-path"]');
        const badgeEls = document.querySelectorAll('[id^="current-instance-badge"]');
        
        pathEls.forEach(el => {
            el.textContent = info.loader.toUpperCase() + ' — ' + info.version;
        });
        badgeEls.forEach(el => {
            el.title = info.path;
        });
    };

    const openSelector = async () => {
        if (!modal || !list) return;
        list.innerHTML = '<div class="spinner-container"><div class="spinner"></div></div>';
        modal.classList.remove('hidden');
        
        const instances = await window.electronAPI.listInstances();
        list.innerHTML = '';
        
        if (instances.length === 0) {
            list.innerHTML = '<div class="mods-empty">Aucune instance trouvée.</div>';
            return;
        }

        instances.forEach(name => {
            const item = document.createElement('div');
            item.className = 'instance-item';
            item.style = 'background: rgba(255,255,255,0.05); border: 1px solid rgba(255,255,255,0.1); padding: 12px; border-radius: 8px; cursor: pointer; transition: all 0.2s; display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px;';
            
            const [v, l] = name.includes('-') ? name.split('-') : [name, 'vanilla'];
            
            item.innerHTML = `
                <div style="display: flex; flex-direction: column;">
                    <span style="font-weight: 700; color: #fff;">${name}</span>
                    <span style="font-size: 11px; color: var(--text-muted);">${v} (${l})</span>
                </div>
                <div class="instance-select-btn" style="padding: 6px 12px; background: var(--accent); border-radius: 6px; font-size: 11px; font-weight: 600;">Selectionner</div>
            `;

            item.onclick = async () => {
                const settings = await window.electronAPI.getSettings();
                settings.version = v;
                settings.loader = { type: l.toLowerCase(), build: 'latest' };
                await window.electronAPI.saveSettings(settings);
                modal.classList.add('hidden');
                notify(`Instance activée : ${name}`, 'success');
                await refresh();
            };

            list.appendChild(item);
        });
    };

    document.addEventListener('click', (e) => {
        if (e.target.closest('#btn-switch-instance')) openSelector();
        else if (e.target.closest('#btn-open-instance-folder')) window.electronAPI.openFolder();
    });

    btnRefresh?.addEventListener('click', openSelector);
    await refresh();
}

export async function loadInstalledMods(prefix = 'mods', type = 'mod') {
    const container = document.getElementById(`${prefix}-installed-list`);
    if (!container) return;

    const settings = await window.electronAPI.getSettings();
    const version = settings.version || '1.21.4';
    const loader = settings.loader?.type || 'vanilla';

    container.innerHTML = '<div class="spinner-container"><div class="spinner"></div></div>';
    const res = await window.electronAPI.getInstalledMods(version, loader, type);

    // Update tab count
    const countSpan = document.getElementById(`installed-${prefix}-count`) || document.getElementById('installed-mods-count');
    if (countSpan && res.success) countSpan.textContent = res.mods.length;
    
    if (!res.success) {
        container.innerHTML = `<div class="mods-empty">Erreur: ${res.error}</div>`;
        return;
    }

    if (res.mods.length === 0) {
        container.innerHTML = `
            <div class="mods-empty" style="text-align:center; padding:40px;">
                <div style="font-size:40px; margin-bottom:12px;">📭</div>
                <div>Aucun ${type === 'mod' ? 'mod' : 'fichier'} installé.</div>
                <div style="font-size:12px; color:var(--text-muted); margin-top:8px;">Installez un pack ou recherchez des mods.</div>
            </div>`;
        return;
    }

    function parseModName(fn) {
        let n = fn.replace(/\.jar$|\.zip$/i, '');
        n = n.replace(/[-+](mc)?[\d.]+.*$/i, '');
        n = n.replace(/[-_](fabric|forge|neoforge|quilt).*$/i, '');
        n = n.replace(/[-_]/g, ' ').replace(/\s+/g, ' ').trim();
        return n.split(' ').map(w => w.charAt(0).toUpperCase() + w.slice(1)).join(' ');
    }

    function parseModVer(fn) {
        const m = fn.match(/[-+](\d+\.\d+[\d.]*)/);
        return m ? m[1] : '';
    }

    container.innerHTML = `
        <div class="installed-header">
            <div class="installed-count">
                <span class="count-number">${res.mods.length}</span>
                <span class="count-label">${type === 'mod' ? 'mod(s)' : 'fichier(s)'} installé(s)</span>
            </div>
            <button class="btn-delete-all" title="Tout supprimer">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="width:14px;height:14px;"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg>
                Tout supprimer
            </button>
        </div>
        <div class="installed-list"></div>
    `;

    container.querySelector('.btn-delete-all').addEventListener('click', async () => {
        if (confirm(`Supprimer les ${res.mods.length} fichiers ?`)) {
            const cleanRes = await window.electronAPI.cleanMods(version, loader, type);
            if (cleanRes.success) {
                notify(`${cleanRes.deleted} fichier(s) supprimé(s)`, 'success');
                loadInstalledMods(prefix, type);
            }
        }
    });

    const list = container.querySelector('.installed-list');
    res.mods.forEach(filename => {
        const modName = parseModName(filename);
        const modVersion = parseModVer(filename);
        const isJar = filename.endsWith('.jar');
        const card = document.createElement('div');
        card.className = 'mod-installed-card';
        card.innerHTML = `
            <div class="mod-installed-icon">
                <span class="mod-ext-badge ${isJar ? 'ext-jar' : 'ext-zip'}">${isJar ? 'JAR' : 'ZIP'}</span>
            </div>
            <div class="mod-installed-info">
                <div class="mod-installed-name">${modName}</div>
                <div class="mod-installed-meta">
                    ${modVersion ? `<span class="mod-version-tag">v${modVersion}</span>` : ''}
                    <span class="mod-file-tag">${filename}</span>
                </div>
            </div>
            <button class="btn-delete-single" title="Supprimer">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg>
            </button>
        `;
        card.querySelector('.btn-delete-single').addEventListener('click', async (e) => {
            e.stopPropagation();
            const btn = e.currentTarget;
            btn.innerHTML = '<div class="spinner" style="width:14px;height:14px;border-width:2px;"></div>';
            const delRes = await window.electronAPI.deleteMod(filename, version, loader, type);
            if (delRes.success) {
                card.style.transform = 'translateX(100%)';
                card.style.opacity = '0';
                setTimeout(() => {
                    card.remove();
                    const countEl = container.querySelector('.count-number');
                    if (countEl) countEl.textContent = list.children.length;
                    if (list.children.length === 0) loadInstalledMods(prefix, type);
                }, 300);
                notify(`${modName} supprimé`, 'info');
            }
        });
        list.appendChild(card);
    });
}
