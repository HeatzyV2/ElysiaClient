const { ipcMain, app } = require('electron');
const fs = require('fs');
const path = require('path');

class SimpleStore {
    constructor() {
        const userDataPath = app.getPath('userData');
        this.path = path.join(userDataPath, 'config.json');
        
        this.defaults = {
            accounts: [],
            activeAccountId: null,
            settings: {
                version: 'latest_release',
                loader: {
                    type: 'vanilla',
                    build: 'latest',
                    enable: false
                },
                memory: {
                    min: 2,
                    max: 4
                },
                java: {
                    path: null,
                    type: 'jre'
                },
                screen: {
                    width: null,
                    height: null,
                    fullscreen: false
                },
                closeOnLaunch: false,
                gameBooster: false,
                lowPerfMode: false,
                jvmArgs: '',
                gameArgs: ''
            },
            discordRpc: {
                enabled: true,
                details: 'Jouer à Elysia',
                state: '{version}',
                largeImageKey: 'elysia_logo',
                largeImageText: 'Elysia Launcher',
                smallImageKey: 'minecraft',
                smallImageText: 'Minecraft',
                buttons: [
                    { label: 'Télécharger', url: 'https://client.elysiastudios.net' },
                    { label: 'Discord', url: 'https://discord.gg/elysiastudios' }
                ]
            }
        };

        this.data = this.parseDataFile(this.path, this.defaults);
    }

    get(key) {
        return this.data[key] !== undefined ? this.data[key] : this.defaults[key];
    }

    set(key, val) {
        this.data[key] = val;
        this.queueWrite();
    }

    delete(key) {
        delete this.data[key];
        this.queueWrite();
    }

    queueWrite() {
        if (this.writeTimeout) {
            clearTimeout(this.writeTimeout);
        }

        // Debounce write to avoid excessive I/O during rapid updates (e.g. playtime)
        this.writeTimeout = setTimeout(() => {
            this.writeToDisk();
        }, 500);
    }

    async writeToDisk() {
        try {
            // Use async writeFile to avoid blocking the main thread
            const dataStr = JSON.stringify(this.data, null, 2);
            await fs.promises.writeFile(this.path, dataStr, 'utf8');
        } catch (e) {
            console.error('Failed to write store data:', e);
        }
    }

    parseDataFile(filePath, defaults) {
        try {
            if (!fs.existsSync(filePath)) {
                fs.writeFileSync(filePath, JSON.stringify(defaults, null, 2));
                return { ...defaults };
            }
            const data = JSON.parse(fs.readFileSync(filePath, 'utf8'));
            // Merge defaults for top level keys if missing
            return { ...defaults, ...data };
        } catch (error) {
            console.error('Error reading config file:', error);
            return { ...defaults };
        }
    }
}

let storeInstance = null;

function ensureStore() {
    if (!storeInstance) {
        storeInstance = new SimpleStore();
    }

    return storeInstance;
}

const store = {
    get(key) {
        return ensureStore().get(key);
    },
    set(key, value) {
        return ensureStore().set(key, value);
    },
    delete(key) {
        return ensureStore().delete(key);
    }
};

function setupStoreIpc() {
    ipcMain.handle('store:get', (_, key) => {
        return store.get(key);
    });

    ipcMain.handle('store:set', (_, key, value) => {
        store.set(key, value);
        return true;
    });

    ipcMain.handle('store:delete', (_, key) => {
        store.delete(key);
        return true;
    });
}

module.exports = { store, setupStoreIpc };
