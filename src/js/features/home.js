import { state } from '../core/state.js';
import { notify, showProgress } from '../core/utils.js';

export function initHome() {
    const btnPlay = document.getElementById('btn-play');
    const playText = document.getElementById('play-text');
    const statusText = document.getElementById('status-text');
    const infoVersion = document.getElementById('info-version');
    const infoLoader = document.getElementById('info-loader');
    const infoMemory = document.getElementById('info-memory');
    const progressFill = document.getElementById('progress-fill');
    const progressLabel = document.getElementById('progress-label');
    const progressPercent = document.getElementById('progress-percent');
    const progressSpeed = document.getElementById('progress-speed');
    const progressEta = document.getElementById('progress-eta');
    const infoPlaytime = document.getElementById('info-playtime');
    let playClickAnimationTimeout = null;

    function triggerPlayClickAnimation(event) {
        if (!btnPlay || btnPlay.disabled || state.get('isLaunching') || state.get('isPlaying')) return;

        const rect = btnPlay.getBoundingClientRect();
        const offsetX = typeof event?.clientX === 'number' ? event.clientX - rect.left : rect.width / 2;
        const offsetY = typeof event?.clientY === 'number' ? event.clientY - rect.top : rect.height / 2;

        btnPlay.style.setProperty('--click-x', `${offsetX}px`);
        btnPlay.style.setProperty('--click-y', `${offsetY}px`);
        btnPlay.classList.remove('click-burst');
        void btnPlay.offsetWidth;
        btnPlay.classList.add('click-burst');

        if (playClickAnimationTimeout) window.clearTimeout(playClickAnimationTimeout);
        playClickAnimationTimeout = window.setTimeout(() => {
            btnPlay.classList.remove('click-burst');
            playClickAnimationTimeout = null;
        }, 560);
    }

    // === Update info cards when settings change ===
    function refreshInfoCards() {
        const settings = state.get('settings') || {};
        const version = settings.version || 'latest_release';
        const loader = settings.loader?.type || 'vanilla';
        const memMax = settings.memory?.max || 4;
        const memMin = settings.memory?.min || 2;

        if (infoVersion) infoVersion.textContent = version;
        if (infoLoader) {
            const loaderNames = {
                'vanilla': 'Vanilla',
                'forge': 'Forge',
                'neoforge': 'NeoForge',
                'fabric': 'Fabric',
                'quilt': 'Quilt',
                'legacyfabric': 'Legacy Fabric',
                'elysiaclient': 'Elysia Client'
            };
            infoLoader.textContent = loaderNames[loader] || loader;
        }
        if (infoMemory) infoMemory.textContent = `${memMin} - ${memMax} GB`;
    }

    let livePlaytimeInterval = null;
    let basePlaytime = 0;
    
    async function refreshStats() {
        if (!infoPlaytime) return;
        const stats = await window.electronAPI.getStats();
        basePlaytime = stats?.playtime || 0;
        updatePlaytimeDisplay(basePlaytime);
    }

    function updatePlaytimeDisplay(totalMs) {
        if (!infoPlaytime) return;
        const hours = Math.floor(totalMs / 3600000);
        const minutes = Math.floor((totalMs % 3600000) / 60000);
        infoPlaytime.textContent = `${hours}h ${minutes}m`;
    }

    async function tickLivePlaytime() {
        if (state.get('isPlaying')) {
            const stats = await window.electronAPI.getStats();
            updatePlaytimeDisplay(stats?.playtime || 0);
        }
    }

    // === Update play button state based on account ===
    function refreshPlayState() {
        const accounts = state.get('accounts') || [];
        const activeId = state.get('activeAccountId');
        const activeAccount = accounts.find(a => a.id === activeId);
        const isLaunching = state.get('isLaunching');
        const isPlaying = state.get('isPlaying');

        if (!btnPlay) return;

        if (isLaunching) {
            btnPlay.disabled = true;
            btnPlay.classList.add('launching');
            if (playText) playText.textContent = 'LANCEMENT...';
            if (statusText) statusText.textContent = 'Préparation en cours...';
            return;
        }

        if (isPlaying) {
            btnPlay.disabled = true;
            btnPlay.classList.add('playing');
            btnPlay.classList.remove('launching');
            if (playText) playText.textContent = 'EN JEU';
            if (statusText) statusText.textContent = 'Minecraft est en cours d\'exécution';
            return;
        }

        btnPlay.classList.remove('launching', 'playing');

        if (activeAccount) {
            btnPlay.disabled = false;
            if (playText) playText.textContent = 'JOUER';
            if (statusText) statusText.textContent = `Connecté en tant que ${activeAccount.name}`;
        } else {
            btnPlay.disabled = true;
            if (playText) playText.textContent = 'JOUER';
            if (statusText) statusText.textContent = 'Connectez-vous pour jouer';
        }
    }

    // Subscribe to state changes
    state.subscribe('settings', refreshInfoCards);
    state.subscribe('accounts', refreshPlayState);
    state.subscribe('activeAccountId', refreshPlayState);
    state.subscribe('isLaunching', refreshPlayState);
    state.subscribe('isPlaying', (playing) => {
        refreshPlayState();
        if (playing) {
            if (livePlaytimeInterval) clearInterval(livePlaytimeInterval);
            livePlaytimeInterval = setInterval(tickLivePlaytime, 30000); // Check every 30s
            tickLivePlaytime();
        } else {
            if (livePlaytimeInterval) clearInterval(livePlaytimeInterval);
            refreshStats(); // Update playtime when game closes
        }
    });

    // Check if game is already running from previous session
    async function checkExistingGame() {
        if (window.electronAPI.checkLaunchStatus) {
            const isRunning = await window.electronAPI.checkLaunchStatus();
            if (isRunning) {
                state.set('isPlaying', true);
                state.set('isLaunching', false);
            } else if (state.get('isPlaying') && !state.get('isLaunching')) {
                // If it was playing but is no longer running, and we aren't currently launching
                state.set('isPlaying', false);
            }
        }
    }

    // Initial refresh
    refreshInfoCards();
    refreshPlayState();
    refreshStats();
    checkExistingGame();

    // Check again every 30 seconds just to be safe
    setInterval(checkExistingGame, 30000);

    // === Play button click ===
    btnPlay?.addEventListener('pointerdown', (event) => {
        triggerPlayClickAnimation(event);
    });

    btnPlay?.addEventListener('click', async (event) => {
        if (state.get('isLaunching') || state.get('isPlaying')) return;

        const accounts = state.get('accounts') || [];
        const activeId = state.get('activeAccountId');
        const activeAccount = accounts.find(a => a.id === activeId);

        if (!activeAccount) {
            notify('Veuillez vous connecter d\'abord', 'warning');
            return;
        }

        if (event.detail === 0) {
            triggerPlayClickAnimation();
        }

        state.set('isLaunching', true);
        showProgress(true);
        if (progressLabel) progressLabel.textContent = 'Préparation...';

        // OPTIMIZATION: Pause heavy UI elements
        if (window.toggleParticles) window.toggleParticles(false);
        if (window.bgAudio && !window.bgAudio.paused) {
            window.bgAudio.pause();
            state.set('musicPausedByLaunch', true);
        }

        try {
            const result = await window.electronAPI.launch();

            if (!result.success) {
                state.set('isLaunching', false);
                state.set('isPlaying', false);
                showProgress(false);
                notify(result.error || 'Erreur lors du lancement', 'error');
                return;
            }

            state.set('isLaunching', false);
            state.set('isPlaying', true);
            notify(result.alreadyRunning ? 'Minecraft est deja lance via Elysia.' : 'Minecraft lance !', 'success');
            return;
        } catch (error) {
            state.set('isLaunching', false);
            state.set('isPlaying', false);
            showProgress(false);
            
            // Resume UI if failed
            if (window.toggleParticles) window.toggleParticles(true);
            if (state.get('musicPausedByLaunch')) {
                window.bgAudio?.play();
                state.set('musicPausedByLaunch', false);
            }

            notify('Erreur: ' + error.message, 'error');
        }
    });

    // === Listen to main process events ===
    window.electronAPI.onLaunchStatus?.((data) => {
        if (!data?.status) return;

        if (data.status === 'downloading') {
            state.set('isLaunching', true);
            state.set('isPlaying', false);
            return;
        }

        if (data.status === 'playing') {
            state.set('isLaunching', false);
            state.set('isPlaying', true);
            showProgress(false);
            return;
        }

        if (data.status === 'idle') {
            state.set('isLaunching', false);
            state.set('isPlaying', false);
            showProgress(false);
        }
    });

    window.electronAPI.onStatsUpdate?.((stats) => {
        updatePlaytimeDisplay(stats?.playtime || 0);
    });

    let lastProgressUpdate = 0;
    window.electronAPI.onProgress?.((data) => {
        // Optimization: Throttle UI updates to 10 FPS during launch
        const now = Date.now();
        if (now - lastProgressUpdate < 100) return;
        lastProgressUpdate = now;

        showProgress(true);
        if (progressFill) progressFill.style.width = `${data.percent || 0}%`;
        if (progressLabel) progressLabel.textContent = `Téléchargement: ${data.type || 'Fichier'}`;
        if (progressPercent) progressPercent.textContent = `${data.percent || 0}%`;
    });

    window.electronAPI.onSpeed?.((speed) => {
        if (progressSpeed) progressSpeed.textContent = `${speed} MB/s`;
    });

    window.electronAPI.onEstimated?.((seconds) => {
        if (progressEta) progressEta.textContent = `~${seconds}s restantes`;
    });

    window.electronAPI.onClose?.((data) => {
        state.set('isLaunching', false);
        state.set('isPlaying', false);
        showProgress(false);
        
        const code = typeof data === 'object' ? data.code : data;
        const crashReport = typeof data === 'object' ? data.crashReport : null;

        if (code !== 0) {
            if (crashReport) {
                notify(`Crash détecté : ${crashReport}`, 'error');
            } else {
                notify(`Le jeu s'est arrêté (Code ${code})`, 'warning');
            }
        } else {
            notify('Jeu fermé', 'info');
        }

        // OPTIMIZATION: Resume UI elements
        if (window.toggleParticles) window.toggleParticles(true);
        if (state.get('musicPausedByLaunch')) {
            window.bgAudio?.play();
            state.set('musicPausedByLaunch', false);
        }
    });

    window.electronAPI.onError?.((err) => {
        state.set('isLaunching', false);
        state.set('isPlaying', false);
        showProgress(false);
        notify(err, 'error');
    });

    // Load news feed
    fetchNews();
}

async function fetchNews() {
    const feed = document.getElementById('news-feed');
    if (!feed) return;

    try {
        // Fetch Minecraft Launcher News API (public)
        const response = await fetch('https://launchercontent.mojang.com/news.json');
        const data = await response.json();
        
        feed.innerHTML = '';
        
        // Take the latest 5 news items
        const entries = data.entries.slice(0, 5);
        
        entries.forEach(entry => {
            const card = document.createElement('a');
            card.className = 'news-card';
            card.href = `https://www.minecraft.net/en-us/article/${entry.id}`;
            card.target = '_blank';
            
            const imageUrl = entry.playPageImage ? `https://launchercontent.mojang.com${entry.playPageImage.url}` : '';
            
            card.innerHTML = `
                <div class="news-image" style="background-image: url('${imageUrl}')"></div>
                <div class="news-content">
                    <h4>${entry.title}</h4>
                    <p>${entry.category}</p>
                </div>
            `;
            
            feed.appendChild(card);
        });
    } catch (e) {
        feed.innerHTML = '<div class="news-loading" style="color:var(--danger)">Impossible de charger les actualités</div>';
    }
}
