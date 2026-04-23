// modpacks.js — Smart version-aware mod packs
import { state } from '../core/state.js';
import { notify } from '../core/utils.js';

// Presets are now version-agnostic: the backend downloads the right version for each mod
const MOD_PACKS = [
    // ═══════════ RECOMMANDÉ PAR ELYSIA ═══════════
    {
        id: 'elysia_ultimate_fabric',
        name: '👑 Elysia Ultimate',
        loader: 'fabric',
        description: 'Le pack ultime Fabric. Performance, Shaders, VoiceChat, Map et tout le nécessaire.',
        icon: '👑',
        color: '#facc15',
        isPremium: true,
        mods: [
            // ── Core ──
            { id: 'fabric-api', name: 'Fabric API', required: true },
            { id: 'modmenu', name: 'Mod Menu' },
            { id: 'cloth-config', name: 'Cloth Config API' },
            // ── Performance ──
            { id: 'sodium', name: 'Sodium' },
            { id: 'lithium', name: 'Lithium' },
            { id: 'ferrite-core', name: 'FerriteCore' },
            { id: 'entityculling', name: 'Entity Culling' },
            { id: 'immediatelyfast', name: 'ImmediatelyFast' },
            { id: 'dynamic-fps', name: 'Dynamic FPS' },
            { id: 'clumps', name: 'Clumps' },
            // ── Shaders ──
            { id: 'iris', name: 'Iris Shaders' },
            // ── PvP / Combat ──
            { id: 'armor-status', name: 'Armor Status HUD' },
            { id: 'toggle-sprint-display', name: 'Toggle Sprint' },
            { id: 'zoomify', name: 'Zoomify' },
            { id: 'freelook', name: 'Freelook' },
            // ── HUD / Info ──
            { id: 'jade', name: 'Jade (WAILA)' },
            { id: 'appleskin', name: 'AppleSkin' },
            { id: 'better-ping-display-fabric', name: 'Better Ping Display' },
            { id: 'shulkerboxtooltip', name: 'ShulkerBox Tooltip' },
            { id: 'status-effect-bars', name: 'Status Effect Bars' },
            // ── Inventory ──
            { id: 'mouse-tweaks', name: 'Mouse Tweaks' },
            // ── Visuals ──
            { id: 'not-enough-animations', name: 'Not Enough Animations' },
            { id: 'continuity', name: 'Continuity' },
            { id: 'capes', name: 'Capes' },
            // ── Social ──
            { id: 'simple-voice-chat', name: 'Simple Voice Chat' },
            { id: 'xaeros-minimap', name: 'Xaero\'s Minimap' },
            { id: 'xaeros-world-map', name: 'Xaero\'s World Map' }
        ]
    },
    {
        id: 'elysia_ultimate_neoforge',
        name: '👑 Elysia Ultimate',
        loader: 'neoforge',
        description: 'Le pack ultime NeoForge. Embeddium, Shaders, Voice Chat et survie parfaite.',
        icon: '👑',
        color: '#facc15',
        isPremium: true,
        mods: [
            { id: 'embeddium', name: 'Embeddium' },
            { id: 'modernfix', name: 'ModernFix' },
            { id: 'ferrite-core', name: 'FerriteCore' },
            { id: 'entityculling', name: 'Entity Culling' },
            { id: 'immediatelyfast', name: 'ImmediatelyFast' },
            { id: 'dynamic-fps', name: 'Dynamic FPS' },
            { id: 'simple-voice-chat', name: 'Simple Voice Chat' },
            { id: 'not-enough-animations', name: 'Not Enough Animations' },
            { id: 'appleskin', name: 'AppleSkin' },
            { id: 'xaeros-minimap', name: 'Xaero\'s Minimap' },
            { id: 'xaeros-world-map', name: 'Xaero\'s World Map' },
            { id: 'oculus', name: 'Oculus (Shaders)' },
            { id: 'cloth-config', name: 'Cloth Config API' }
        ]
    },

    // ═══════════ PERFORMANCE ═══════════
    {
        id: 'perf_fabric',
        name: '⚡ Performance Max',
        loader: 'fabric',
        description: 'Sodium, Lithium et optimisation extrême pour Fabric.',
        icon: '⚡',
        color: '#34d399',
        mods: [
            { id: 'fabric-api', name: 'Fabric API', required: true },
            { id: 'sodium', name: 'Sodium' },
            { id: 'lithium', name: 'Lithium' },
            { id: 'ferrite-core', name: 'FerriteCore' },
            { id: 'entityculling', name: 'Entity Culling' },
            { id: 'modmenu', name: 'Mod Menu' }
        ]
    },
    {
        id: 'perf_neoforge',
        name: '⚡ Performance Max',
        loader: 'neoforge',
        description: 'Embeddium et ModernFix pour un boost NeoForge.',
        icon: '⚡',
        color: '#f59e0b',
        mods: [
            { id: 'embeddium', name: 'Embeddium' },
            { id: 'modernfix', name: 'ModernFix' },
            { id: 'ferrite-core', name: 'FerriteCore' },
            { id: 'entityculling', name: 'Entity Culling' }
        ]
    }
];

export function initModPacks() {
    const container = document.getElementById('modpacks-container');
    if (!container) return;

    renderPacks(container);

    // Re-render when settings change (version/loader change)
    window.addEventListener('settings-changed', () => renderPacks(container));
}

async function renderPacks(container) {
    const settings = await window.electronAPI.getSettings();
    const currentLoader = settings?.loader?.type || 'vanilla';
    const currentVersion = settings?.version || '1.21.4';

    // ElysiaClient runs on Fabric internally, while keeping its own isolated instance.
    const filtered = MOD_PACKS.filter(p => p.loader === currentLoader || (currentLoader === 'elysiaclient' && p.loader === 'fabric'));
    
    if (filtered.length === 0) {
        container.innerHTML = `
            <div class="mods-empty" style="text-align:center; padding: 40px;">
                <div style="font-size: 40px; margin-bottom: 12px;">📦</div>
                <div>Aucun preset disponible pour <strong>${currentLoader.toUpperCase()}</strong>.</div>
                <div style="font-size: 12px; color: var(--text-muted); margin-top: 8px;">Changez de loader dans les paramètres ou le gestionnaire d'instances.</div>
            </div>
        `;
        return;
    }

    const discordProfile = window.electronAPI.storeGet ? await window.electronAPI.storeGet('discordProfile') : null;
    const isVIP = !!discordProfile;

    container.innerHTML = '';
    filtered.forEach(pack => {
        container.appendChild(createPackCard(pack, currentVersion, isVIP));
    });
}

window.refreshVIPMods = function() {
    const container = document.getElementById('modpacks-container');
    if (container) renderPacks(container);
};

function createPackCard(pack, currentVersion, isVIP) {
    const card = document.createElement('div');
    card.className = `modpack-card ${pack.isPremium ? 'modpack-card-premium' : ''}`;
    card.style.setProperty('--pack-color', pack.color);

    const loaderBadgeColor = pack.loader === 'fabric' ? '#eab308' : (pack.loader === 'neoforge' ? '#ec4899' : '#ef4444');
    
    let actionButtonHTML = '';
    if (pack.isPremium && !isVIP) {
        actionButtonHTML = `
            <button class="btn btn-secondary" style="flex:1; border-color: #5865F2; color: #5865F2;" onclick="window.switchPage('settings')">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="width:16px;height:16px;margin-right:6px;"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect><path d="M7 11V7a5 5 0 0 1 10 0v4"></path></svg>
                Lier Discord pour Débloquer
            </button>
        `;
    } else {
        actionButtonHTML = `
            <button class="btn btn-primary modpack-install-btn" style="flex:1;" data-id="${pack.id}">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/></svg>
                Installer
            </button>
        `;
    }

    card.innerHTML = `
        ${pack.isPremium ? '<div class="premium-badge">VIP DISCORD</div>' : ''}
        <div class="modpack-header">
            <span class="modpack-icon">${pack.icon}</span>
            <div class="modpack-title" style="flex:1;">
                <div style="display:flex; justify-content:space-between; align-items:flex-start;">
                    <h3>${pack.name}</h3>
                    <span style="font-size:10px; background:${loaderBadgeColor}33; color:${loaderBadgeColor}; padding:2px 6px; border-radius:4px; text-transform:uppercase; font-weight:bold; border:1px solid ${loaderBadgeColor}66;">${pack.loader}</span>
                </div>
                <p>${pack.description}</p>
                <div style="font-size:11px; color:var(--text-muted); margin-top:4px;">
                    📦 ${pack.mods.length} mods • Adapté pour <strong>${currentVersion}</strong>
                </div>
            </div>
        </div>
        <div class="modpack-mods">
            ${pack.mods.map(m => `
                <div class="modpack-mod-item">
                    <span class="modpack-mod-name">${m.name}</span>
                </div>
            `).join('')}
        </div>
        <div class="modpack-actions" style="display:flex; gap:8px;">
            ${actionButtonHTML}
            <button class="btn btn-secondary modpack-uninstall-btn" style="padding: 0 12px;" title="Désinstaller ce pack">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="width:16px;height:16px;margin:0;"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg>
            </button>
        </div>
        <div class="modpack-progress" style="display:none; margin-top:12px;">
            <div style="display:flex; justify-content:space-between; margin-bottom:6px;">
                <span class="progress-mod-name" style="font-size:12px; color:var(--text-secondary);">Préparation...</span>
                <span class="progress-count" style="font-size:12px; color:var(--text-muted);">0/${pack.mods.length}</span>
            </div>
            <div style="height:4px; background:rgba(255,255,255,0.1); border-radius:4px; overflow:hidden;">
                <div class="progress-bar" style="height:100%; width:0%; background:var(--accent); transition:width 0.3s;"></div>
            </div>
            <div class="progress-skipped" style="font-size:11px; color:#f59e0b; margin-top:6px; display:none;"></div>
        </div>
    `;

    card.querySelector('.modpack-install-btn').addEventListener('click', () => {
        installPack(pack, card);
    });

    card.querySelector('.modpack-uninstall-btn').addEventListener('click', () => {
        uninstallPack(pack);
    });

    return card;
}

async function installPack(pack, card) {
    let settings = await window.electronAPI.getSettings();
    const version = settings.version || '1.21.4';
    const activeLoader = settings.loader?.type || 'vanilla';
    const isElysiaFabric = activeLoader === 'elysiaclient' && pack.loader === 'fabric';
    const installLoader = isElysiaFabric ? 'elysiaclient' : pack.loader;
    
    if (activeLoader !== pack.loader && !isElysiaFabric) {
        if (!confirm(`Ce pack nécessite ${pack.loader.toUpperCase()}.\nVoulez-vous changer votre Mod Loader vers ${pack.loader} ?`)) {
            return;
        }
        
        settings.loader = { type: pack.loader, build: 'latest', enable: true };
        state.set('settings', settings);
        await window.electronAPI.saveSettings(settings);
    }

    // Ask for clean install
    const doClean = confirm(`Voulez-vous supprimer tous les anciens mods avant d'installer le pack ?\n\n✅ Recommandé pour éviter les conflits et crashs.\n❌ Cliquez "Annuler" pour garder vos mods existants.`);

    const btn = card.querySelector('.modpack-install-btn');
    const progressArea = card.querySelector('.modpack-progress');
    const progressBar = card.querySelector('.progress-bar');
    const progressName = card.querySelector('.progress-mod-name');
    const progressCount = card.querySelector('.progress-count');
    const progressSkipped = card.querySelector('.progress-skipped');

    btn.disabled = true;
    btn.style.display = 'none';
    progressArea.style.display = 'block';

    // Step 1: Clean old mods if requested
    if (doClean) {
        progressName.textContent = '🧹 Nettoyage des anciens mods...';
        const cleanRes = await window.electronAPI.cleanMods(version, installLoader, 'mod');
        if (cleanRes.success) {
            progressName.textContent = `🧹 ${cleanRes.deleted} ancien(s) mod(s) supprimé(s)`;
        }
        await new Promise(r => setTimeout(r, 500));
    }

    let installed = 0;
    let skipped = [];

    for (let i = 0; i < pack.mods.length; i++) {
        const mod = pack.mods[i];
        progressName.textContent = `${mod.name}...`;
        progressCount.textContent = `${i}/${pack.mods.length}`;
        progressBar.style.width = `${((i) / pack.mods.length) * 100}%`;

        // Rate limit delay
        await new Promise(r => setTimeout(r, 300));

        const res = await window.electronAPI.installMod(mod.id, version, installLoader, 'mod');
        if (res.success && !res.skipped) {
            installed++;
        } else if (!res.success) {
            skipped.push(mod.name);
            console.warn(`[Pack] Skipped ${mod.name} for ${version}: ${res.error}`);
        }
    }

    // Done
    progressBar.style.width = '100%';
    progressCount.textContent = `${installed}/${pack.mods.length}`;
    progressName.textContent = 'Terminé !';

    if (skipped.length > 0) {
        progressSkipped.style.display = 'block';
        progressSkipped.textContent = `⚠️ ${skipped.length} mod(s) incompatible(s) avec ${version}: ${skipped.join(', ')}`;
    }

    setTimeout(() => {
        btn.style.display = '';
        btn.disabled = false;
        progressArea.style.display = 'none';
        progressSkipped.style.display = 'none';
    }, 4000);

    notify(`Pack "${pack.name}" installé (${installed}/${pack.mods.length} mods pour ${version})`, installed === pack.mods.length ? 'success' : 'warning');
    document.getElementById('tab-mods-installed')?.click();
}

async function uninstallPack(pack) {
    const choice = confirm(`Voulez-vous supprimer TOUS les mods de cette instance ?\n\n✅ OK = Supprime tout (mods du pack + dépendances)\n❌ Annuler = Ne rien faire`);
    if (!choice) return;

    const settings = await window.electronAPI.getSettings();
    const version = settings.version || '1.21.4';
    const loader = settings.loader?.type || 'vanilla';

    const cleanRes = await window.electronAPI.cleanMods(version, loader, 'mod');
    if (cleanRes.success) {
        notify(`${cleanRes.deleted} mod(s) supprimé(s) (pack + dépendances)`, 'success');
    } else {
        notify('Erreur lors de la suppression.', 'error');
    }
    document.getElementById('tab-mods-installed')?.click();
}
