// state.js - Reactive state management
class State {
    constructor() {
        this.listeners = new Map();
        this.data = {
            activePage: 'home',
            accounts: [],
            activeAccountId: null,
            settings: {},
            isLaunching: false,
            isPlaying: false
        };
    }

    get(key) {
        return this.data[key];
    }

    set(key, value) {
        this.data[key] = value;
        this.notify(key, value);
    }

    subscribe(key, callback) {
        if (!this.listeners.has(key)) {
            this.listeners.set(key, new Set());
        }
        this.listeners.get(key).add(callback);
    }

    notify(key, value) {
        if (this.listeners.has(key)) {
            this.listeners.get(key).forEach(callback => callback(value));
        }
    }
}

export const state = new State();
