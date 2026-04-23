const { ipcMain } = require('electron');
const fs = require('fs');
const path = require('path');
const { store } = require('./store');
const { getInstancePath } = require('./launch');

function setupFeaturesIpc(mainWindow) {
    ipcMain.handle('features:get-config', async () => {
        try {
            const configPath = getFeaturesConfigPath();
            if (fs.existsSync(configPath)) {
                return JSON.parse(fs.readFileSync(configPath, 'utf8'));
            }
        } catch (e) {
            console.error('Failed to read features config:', e);
        }
        return getDefaultConfig();
    });

    ipcMain.handle('features:save-config', async (event, config) => {
        try {
            const configPath = getFeaturesConfigPath();
            const configDir = path.dirname(configPath);
            if (!fs.existsSync(configDir)) {
                fs.mkdirSync(configDir, { recursive: true });
            }
            fs.writeFileSync(configPath, JSON.stringify(config, null, 2));
            return { success: true };
        } catch (e) {
            console.error('Failed to save features config:', e);
            return { success: false, error: e.message };
        }
    });

    ipcMain.handle('features:get-list', async () => {
        return ELYSIA_MODULES;
    });
}

function getFeaturesConfigPath() {
    // Get version from store
    const settings = store.get('settings') || {};
    const version = settings.version === 'latest_release' ? '1.21.4' : (settings.version || '1.21.4');
    
    const instancePath = getInstancePath({ version, loader: { type: 'elysiaclient' } });
    return path.join(instancePath, 'config', 'elysia-client', 'features.json');
}

const ELYSIA_MODULES = [
    {
        id: 'hud',
        category: 'HUD',
        name: 'Éditeur de HUD',
        description: 'Positionnez vos éléments de HUD comme vous le souhaitez.',
        icon: '🎯',
        isHud: true
    },
    {
        id: 'cps',
        category: 'HUD',
        name: 'CPS Counter',
        description: 'Affiche vos clics par seconde en temps réel.',
        icon: '🖱️',
        isHud: true,
        defaultPos: { x: 10, y: 10 }
    },
    {
        id: 'fps',
        category: 'HUD',
        name: 'FPS Counter',
        description: 'Affiche vos images par seconde.',
        icon: '🚀',
        isHud: true,
        defaultPos: { x: 10, y: 35 }
    },
    {
        id: 'armor-hud',
        category: 'HUD',
        name: 'Armor Status',
        description: 'Affiche l\'état de votre armure et de vos objets.',
        icon: '🛡️',
        isHud: true,
        defaultPos: { x: 500, y: 400 }
    },
    {
        id: 'potion-effects',
        category: 'HUD',
        name: 'Status Effects',
        description: 'Affiche vos effets de potion actifs.',
        icon: '🧪',
        isHud: true,
        defaultPos: { x: 800, y: 10 }
    },
    {
        id: 'scoreboard',
        category: 'Visual',
        name: 'Scoreboard',
        description: 'Personnalisez la position et la taille du tableau des scores.',
        icon: '📊',
        isHud: true,
        defaultPos: { x: 800, y: 150 }
    },
    {
        id: 'bossbar',
        category: 'Visual',
        name: 'Boss Bar',
        description: 'Modifiez la position de la barre de boss.',
        icon: '👑',
        isHud: true,
        defaultPos: { x: 400, y: 20 }
    },
    {
        id: 'keystrokes',
        category: 'HUD',
        name: 'Keystrokes',
        description: 'Affiche les touches ZQSD et les clics de souris.',
        icon: '⌨️',
        isHud: true,
        defaultPos: { x: 10, y: 300 }
    },
    {
        id: 'coords',
        category: 'HUD',
        name: 'Coordinates',
        description: 'Affiche vos coordonnées X, Y, Z.',
        icon: '📍',
        isHud: true,
        defaultPos: { x: 10, y: 60 }
    },
    {
        id: 'ping',
        category: 'HUD',
        name: 'Ping Display',
        description: 'Affiche votre latence vers le serveur.',
        icon: '📡',
        isHud: true,
        defaultPos: { x: 10, y: 85 }
    },
    {
        id: 'biom-hud',
        category: 'HUD',
        name: 'Biome HUD',
        description: 'Affiche le biome actuel.',
        icon: '🌳',
        isHud: true,
        defaultPos: { x: 10, y: 110 }
    },
    {
        id: 'direction-hud',
        category: 'HUD',
        name: 'Direction HUD',
        description: 'Affiche une boussole en haut de l\'écran.',
        icon: '🧭',
        isHud: true,
        defaultPos: { x: 400, y: 60 }
    },
    {
        id: 'zoom',
        category: 'Gameplay',
        name: 'Zoomify',
        description: 'Permet de zoomer en jeu (Touche C par défaut).',
        icon: '🔍',
        isHud: false
    },
    {
        id: 'freelook',
        category: 'Gameplay',
        name: 'Perspective (Freelook)',
        description: 'Regardez autour de vous sans changer la direction de votre personnage.',
        icon: '👁️',
        isHud: false
    },
    {
        id: 'toggle-sprint',
        category: 'Gameplay',
        name: 'Toggle Sprint',
        description: 'Permet de sprinter automatiquement.',
        icon: '🏃',
        isHud: false
    },
    {
        id: 'motion-blur',
        category: 'Visual',
        name: 'Motion Blur',
        description: 'Ajoute un effet de flou de mouvement fluide.',
        icon: '🌊',
        isHud: false
    },
    {
        id: 'time-changer',
        category: 'Visual',
        name: 'Time Changer',
        description: 'Changez l\'heure du monde localement.',
        icon: '☀️',
        isHud: false
    },
    {
        id: 'clear-glass',
        category: 'Visual',
        name: 'Clear Glass',
        description: 'Rend le verre plus transparent et propre.',
        icon: '💎',
        isHud: false
    },
    {
        id: 'item-physic',
        category: 'Visual',
        name: 'Item Physics',
        description: 'Les objets au sol ont une physique réaliste.',
        icon: '📦',
        isHud: false
    }
];

function getDefaultConfig() {
    const config = {
        modules: {},
        hud: {}
    };
    ELYSIA_MODULES.forEach(m => {
        config.modules[m.id] = { enabled: true };
        if (m.isHud && m.defaultPos) {
            config.hud[m.id] = {
                x: m.defaultPos.x,
                y: m.defaultPos.y,
                scale: 1.0,
                visible: true
            };
        }
    });
    return config;
}

module.exports = { setupFeaturesIpc };
