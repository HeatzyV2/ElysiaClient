const { app } = require('electron');
const crypto = require('crypto');
const fs = require('fs');
const os = require('os');
const path = require('path');
const { execFileSync } = require('child_process');

const SIGNING_SALT = 'elysia-local-playtime-v1';
const STATS_DIR_NAME = 'elysia-state';
const LEGACY_STATS_DIR_NAME = 'Local State';
const STATS_FILE_NAME = 'playtime.elysia';
const DEFAULT_STATS = {
    playtime: 0,
    lastPlayed: null,
    sessions: 0,
    updatedAt: null
};

function ensureDirectory(dirPath) {
    if (!fs.existsSync(dirPath)) {
        fs.mkdirSync(dirPath, { recursive: true });
        return;
    }

    const stat = fs.statSync(dirPath);
    if (!stat.isDirectory()) {
        throw new Error(`Stats path conflict: "${dirPath}" exists and is not a directory.`);
    }
}

function getStatsDirectory() {
    const dir = path.join(app.getPath('userData'), STATS_DIR_NAME);
    ensureDirectory(dir);
    return dir;
}

function getStatsPath() {
    return path.join(getStatsDirectory(), STATS_FILE_NAME);
}

function getLegacyStatsPath() {
    const legacyBasePath = path.join(app.getPath('userData'), LEGACY_STATS_DIR_NAME);
    if (!fs.existsSync(legacyBasePath)) {
        return null;
    }

    try {
        if (!fs.statSync(legacyBasePath).isDirectory()) {
            return null;
        }
    } catch (error) {
        return null;
    }

    return path.join(legacyBasePath, STATS_FILE_NAME);
}

function getMachineKey() {
    const user = os.userInfo().username || 'user';
    return `${SIGNING_SALT}|${os.hostname()}|${user}|${app.getPath('userData')}`;
}

function normalizeStats(stats) {
    return {
        playtime: Math.max(0, Number(stats?.playtime) || 0),
        lastPlayed: stats?.lastPlayed || null,
        sessions: Math.max(0, Number(stats?.sessions) || 0),
        updatedAt: stats?.updatedAt || null
    };
}

function signPayload(payload) {
    return crypto.createHmac('sha256', getMachineKey()).update(payload).digest('hex');
}

function encodeStats(stats) {
    const normalized = normalizeStats(stats);
    const payload = Buffer.from(JSON.stringify(normalized), 'utf8').toString('base64');
    return JSON.stringify({
        version: 1,
        payload,
        signature: signPayload(payload)
    });
}

function decodeStats(raw) {
    const envelope = JSON.parse(raw);
    if (!envelope?.payload || !envelope?.signature) {
        throw new Error('Invalid stats envelope');
    }

    const expected = signPayload(envelope.payload);
    if (expected !== envelope.signature) {
        throw new Error('Invalid stats signature');
    }

    return normalizeStats(JSON.parse(Buffer.from(envelope.payload, 'base64').toString('utf8')));
}

function unlockStatsFile(filePath) {
    if (!fs.existsSync(filePath)) {
        return;
    }

    try {
        fs.chmodSync(filePath, 0o600);
    } catch (error) {
        // Ignore: Windows ACLs can differ, the next write will surface real failures.
    }

    if (process.platform === 'win32') {
        try {
            execFileSync('attrib', ['-R', '-H', filePath], { windowsHide: true });
        } catch (error) {
            // Best-effort hardening only.
        }
    }
}

function lockStatsFile(filePath) {
    try {
        fs.chmodSync(filePath, 0o444);
    } catch (error) {
        // Best-effort hardening only.
    }

    if (process.platform === 'win32') {
        try {
            execFileSync('attrib', ['+R', '+H', filePath], { windowsHide: true });
        } catch (error) {
            // Best-effort hardening only.
        }
    }
}

function readStatsFile(filePath) {
    if (!filePath || !fs.existsSync(filePath)) {
        return { ...DEFAULT_STATS };
    }

    try {
        return decodeStats(fs.readFileSync(filePath, 'utf8'));
    } catch (error) {
        const corruptPath = `${filePath}.corrupt-${Date.now()}`;
        try {
            unlockStatsFile(filePath);
            fs.renameSync(filePath, corruptPath);
        } catch (renameError) {
            // If quarantine fails, keep going with a clean counter rather than crashing the launcher.
        }
        return { ...DEFAULT_STATS };
    }
}

function migrateLegacyStatsFile() {
    const legacyPath = getLegacyStatsPath();
    if (!legacyPath || !fs.existsSync(legacyPath)) {
        return null;
    }

    const legacyStats = readStatsFile(legacyPath);
    if (
        legacyStats.playtime <= 0
        && !legacyStats.lastPlayed
        && legacyStats.sessions <= 0
        && !legacyStats.updatedAt
    ) {
        return null;
    }

    writeStats(legacyStats);
    return legacyStats;
}

function readStats() {
    const filePath = getStatsPath();
    if (fs.existsSync(filePath)) {
        return readStatsFile(filePath);
    }

    return migrateLegacyStatsFile() || { ...DEFAULT_STATS };
}

function writeStats(stats) {
    const filePath = getStatsPath();
    ensureDirectory(path.dirname(filePath));
    unlockStatsFile(filePath);
    fs.writeFileSync(filePath, encodeStats(stats), 'utf8');
    lockStatsFile(filePath);
}

function getStats() {
    return readStats();
}

function migrateStatsFromStore(store) {
    const current = readStats();
    if (current.playtime > 0 || !store) {
        return current;
    }

    const nested = store.get('stats');
    const legacyPlaytime = Number(store.get('stats.playtime')) || 0;
    const legacyLastPlayed = store.get('stats.lastPlayed') || null;
    const migrated = normalizeStats({
        playtime: Math.max(Number(nested?.playtime) || 0, legacyPlaytime),
        lastPlayed: nested?.lastPlayed || legacyLastPlayed,
        sessions: Number(nested?.sessions) || 0,
        updatedAt: Date.now()
    });

    if (migrated.playtime > 0 || migrated.lastPlayed) {
        writeStats(migrated);
    }

    return migrated;
}

function addPlaytime(deltaMs) {
    const delta = Math.max(0, Math.floor(Number(deltaMs) || 0));
    if (delta <= 0) {
        return readStats();
    }

    const stats = readStats();
    const now = Date.now();
    const updated = normalizeStats({
        ...stats,
        playtime: stats.playtime + delta,
        lastPlayed: now,
        updatedAt: now
    });
    writeStats(updated);
    return updated;
}

function recordSessionStart() {
    const stats = readStats();
    const now = Date.now();
    const updated = normalizeStats({
        ...stats,
        sessions: stats.sessions + 1,
        updatedAt: now
    });
    writeStats(updated);
    return updated;
}

module.exports = {
    addPlaytime,
    getStats,
    migrateStatsFromStore,
    recordSessionStart
};
