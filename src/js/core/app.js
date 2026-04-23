import { state } from './state.js';
import { notify } from './utils.js';
import { initParticles } from './particles.js';
import { initHome } from '../features/home.js';
import { initAccounts, refreshAccountsList } from '../features/accounts.js';
import { initSettings } from '../features/settings.js';
import { initModsUI, initInstanceManager, loadInstalledMods } from '../features/mods-manager.js';
import { initModPacks } from '../features/modpacks.js';
import { initServers } from '../features/servers.js';
import { initConsole } from '../features/console.js';
import { initSocial } from '../features/social.js';
import { initBedrock } from '../features/bedrock.js';

document.addEventListener('DOMContentLoaded', async () => {
    try {
        // Animated particles background
        initParticles();

        // Top Bar Window Controls
        document.getElementById('btn-minimize')?.addEventListener('click', () => window.electronAPI.minimize());
        document.getElementById('btn-maximize')?.addEventListener('click', () => window.electronAPI.maximize());
        document.getElementById('btn-close')?.addEventListener('click', () => window.electronAPI.close());

        // Clone Mods Page for Resource Packs and Shaders
        const modsPage = document.getElementById('page-mods');
        if (modsPage) {
            // Generate resourcepacks page
            const rpPage = modsPage.cloneNode(true);
            rpPage.id = 'page-resourcepacks';
            let rpHTML = rpPage.innerHTML;
            rpHTML = rpHTML.replace(/Mods/g, 'Ressources')
                           .replace(/mods/g, 'resourcepacks')
                           .replace(/modpacks/g, 'resourcepacks-packs')
                           .replace(/le mod parfait \(ex: Sodium, Voice Chat, Iris\)/g, 'la ressource parfaite (ex: Faithful, Sphax, Bare Bones)')
                           .replace(/nouveaux mods/g, 'nouveaux resource packs')
                           .replace(/des mods/g, 'des resource packs')
                           .replace(/un mod/g, 'une ressource');
            rpPage.innerHTML = rpHTML;
            modsPage.parentNode.insertBefore(rpPage, modsPage.nextSibling);

            // Generate shaders page
            const shPage = modsPage.cloneNode(true);
            shPage.id = 'page-shaders';
            let shHTML = shPage.innerHTML;
            shHTML = shHTML.replace(/Mods/g, 'Shaders')
                           .replace(/mods/g, 'shaders')
                           .replace(/modpacks/g, 'shaders-packs')
                           .replace(/le mod parfait \(ex: Sodium, Voice Chat, Iris\)/g, 'le shader parfait (ex: BSL, Complementary, Sildur\'s)')
                           .replace(/nouveaux mods/g, 'nouveaux shaders')
                           .replace(/des mods/g, 'des shaders')
                           .replace(/un mod/g, 'un shader');
            shPage.innerHTML = shHTML;
            modsPage.parentNode.insertBefore(shPage, modsPage.nextSibling);
        }

        // Initialize Features
        await initSettings().catch(e => console.error('Failed to init settings:', e));
        await initAccounts().catch(e => console.error('Failed to init accounts:', e));
        initHome();
        initModsUI('mods', 'mod');
        initModsUI('resourcepacks', 'resourcepack');
        initModsUI('shaders', 'shader');
        initInstanceManager();
        initModPacks();
        initServers();
        initConsole();
        initSocial();
        initBedrock();

        // Navigation System
        setupNavigation();
        
        // Keyboard Shortcuts
        setupShortcuts();

        // Sidebar tooltips
        setupTooltips();
    } catch (error) {
        console.error('Core app initialization failed:', error);
    }
});

const PAGE_ORDER = ['home', 'mods', 'servers', 'settings', 'accounts', 'console', 'social', 'bedrock'];

function setupNavigation() {
    const navButtons = document.querySelectorAll('.nav-btn');
    const pages = document.querySelectorAll('.page');

    window.switchPage = function(pageId) {
        navButtons.forEach(b => b.classList.remove('active'));
        pages.forEach(p => p.classList.remove('active'));

        const targetBtn = document.querySelector(`.nav-btn[data-page="${pageId}"]`);
        const targetPage = document.getElementById(`page-${pageId}`);

        if (targetBtn) targetBtn.classList.add('active');
        if (targetPage) {
            targetPage.classList.add('active');
            state.set('activePage', pageId);
        }
    };

    navButtons.forEach(btn => {
        btn.addEventListener('click', () => {
            window.switchPage(btn.dataset.page);
        });
    });

    // Subscriptions
    state.subscribe('activePage', (pageId) => {
        if (pageId === 'accounts') refreshAccountsList();
        if (pageId === 'mods') loadInstalledMods();
    });

    // Default Page
    window.switchPage('home');
}

function setupShortcuts() {
    document.addEventListener('keydown', (e) => {
        // Ctrl+1 to Ctrl+5 — quick page nav
        if (e.ctrlKey && e.key >= '1' && e.key <= '5') {
            e.preventDefault();
            const idx = parseInt(e.key) - 1;
            if (PAGE_ORDER[idx]) window.switchPage(PAGE_ORDER[idx]);
        }
        // Escape — back to home
        if (e.key === 'Escape') {
            e.preventDefault();
            window.switchPage('home');
        }
        // Ctrl+, — Settings
        if (e.ctrlKey && e.key === ',') {
            e.preventDefault();
            window.switchPage('settings');
        }
    });
}

function setupTooltips() {
    document.querySelectorAll('.nav-btn').forEach(btn => {
        const label = btn.querySelector('span')?.textContent;
        if (label) btn.setAttribute('title', label);
    });
}

window.updateSidebarDiscordAvatar = function(profile) {
    const avatarImg = document.getElementById('header-avatar');
    if (!avatarImg) return;
    
    // Only apply discord avatar if no Minecraft account is linked, or if we want to override it.
    // Wait, the user wants it as a VIP feature. Let's make it so Discord avatar overrides if exists!
    if (profile && profile.avatarUrl) {
        avatarImg.src = profile.avatarUrl;
        avatarImg.style.border = '2px solid #5865F2';
        avatarImg.title = `Discord VIP: ${profile.username}`;
    } else {
        // Fallback to default, accounts.js will update it if there's a MC account
        avatarImg.src = 'data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="%23888" stroke-width="2"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>';
        avatarImg.style.border = 'none';
        avatarImg.title = '';
    }
};

// Check on load
window.electronAPI.storeGet('discordProfile').then(profile => {
    window.updateSidebarDiscordAvatar(profile);
});
