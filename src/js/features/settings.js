import { state } from '../core/state.js';
import { notify } from '../core/utils.js';

const LOW_END_JVM_ARGS = '-XX:+UseG1GC -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=200 -XX:+DisableExplicitGC -XX:+PerfDisableSharedMem -XX:G1HeapRegionSize=4M -Dsun.java2d.opengl=false';

function getLowEndRam(totalMem) {
    if (totalMem <= 4) return Math.max(1, Math.min(totalMem, 2));
    return Math.min(totalMem, 3);
}

function applyBackground(filePath) {
    const bgLayer = document.getElementById('bg-layer');
    if (!bgLayer || !filePath) return;
    
    bgLayer.innerHTML = '';
    const ext = filePath.split('.').pop().toLowerCase();
    
    if (['mp4', 'webm'].includes(ext)) {
        const video = document.createElement('video');
        video.src = filePath;
        video.autoplay = true;
        video.loop = true;
        video.muted = true;
        video.playsInline = true;
        bgLayer.appendChild(video);
    } else {
        const img = document.createElement('img');
        img.src = filePath;
        img.alt = 'Background';
        bgLayer.appendChild(img);
    }
    
    bgLayer.classList.add('active');
}

function clearBackground() {
    const bgLayer = document.getElementById('bg-layer');
    if (!bgLayer) return;
    bgLayer.innerHTML = '';
    bgLayer.classList.remove('active');
}

if (!window.bgAudio) {
    window.bgAudio = new Audio();
    window.bgAudio.loop = true;
}

async function applyMusic(filePath, volume = 50) {
    if (!filePath) {
        window.bgAudio.pause();
        window.bgAudio.src = '';
        return;
    }
    
    if (filePath.startsWith('youtube:')) {
        // Extract audio stream URL via ytdl-core and play natively
        const videoId = filePath.split(':')[1];
        
        try {
            const result = await window.electronAPI.getStreamUrl(videoId);
            if (result.success && result.url) {
                window.bgAudio.src = result.url;
                window.bgAudio.volume = volume / 100;
                window.bgAudio.load();
                window.bgAudio.play().catch(e => console.log('Audio autoplay blocked', e));
            } else {
                console.error('Failed to get stream:', result.error);
            }
        } catch (e) {
            console.error('Stream extraction failed:', e);
        }
    } else {
        // Local File
        if (window.bgAudio.src !== 'file://' + filePath.replace(/\\/g, '/')) {
            window.bgAudio.src = filePath;
            window.bgAudio.load();
        }
        window.bgAudio.volume = volume / 100;
        window.bgAudio.play().catch(e => console.log('Audio autoplay blocked', e));
    }
}

function formatUpdaterDate(value) {
    if (!value) return 'Jamais';

    try {
        return new Date(value).toLocaleString('fr-FR', {
            day: '2-digit',
            month: '2-digit',
            year: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
        });
    } catch (error) {
        return 'Jamais';
    }
}

function formatUpdaterSpeed(bytesPerSecond) {
    const speed = Number(bytesPerSecond || 0);
    if (speed <= 0) return '';

    const mbPerSecond = speed / 1024 / 1024;
    if (mbPerSecond >= 1) {
        return `${mbPerSecond.toFixed(1)} Mo/s`;
    }

    const kbPerSecond = speed / 1024;
    return `${kbPerSecond.toFixed(0)} Ko/s`;
}

function getUpdaterBadge(status) {
    switch (status) {
        case 'checking':
            return { label: 'Analyse', background: 'rgba(59, 130, 246, 0.15)', color: '#93c5fd', border: 'rgba(59, 130, 246, 0.3)' };
        case 'available':
            return { label: 'Disponible', background: 'rgba(245, 158, 11, 0.15)', color: '#fcd34d', border: 'rgba(245, 158, 11, 0.3)' };
        case 'downloading':
            return { label: 'Téléchargement', background: 'rgba(16, 185, 129, 0.15)', color: '#6ee7b7', border: 'rgba(16, 185, 129, 0.3)' };
        case 'downloaded':
            return { label: 'Prête', background: 'rgba(52, 211, 153, 0.15)', color: '#6ee7b7', border: 'rgba(52, 211, 153, 0.3)' };
        case 'not_available':
            return { label: 'À jour', background: 'rgba(52, 211, 153, 0.15)', color: '#6ee7b7', border: 'rgba(52, 211, 153, 0.3)' };
        case 'error':
            return { label: 'Erreur', background: 'rgba(239, 68, 68, 0.15)', color: '#fca5a5', border: 'rgba(239, 68, 68, 0.3)' };
        case 'unsupported':
            return { label: 'Indispo', background: 'rgba(107, 114, 128, 0.15)', color: '#cbd5e1', border: 'rgba(107, 114, 128, 0.3)' };
        default:
            return { label: 'Prêt', background: 'rgba(139, 92, 246, 0.15)', color: '#c4b5fd', border: 'rgba(139, 92, 246, 0.25)' };
    }
}

export async function initSettings() {
    // Selectors
    const versionSelect = document.getElementById('setting-version');
    const loaderSelect = document.getElementById('setting-loader');
    const ramSlider = document.getElementById('setting-ram');
    const ramValue = document.getElementById('ram-value');
    const javaPathInput = document.getElementById('setting-java-path');
    const btnSelectJava = document.getElementById('btn-select-java');
    const closeOnLaunchCheck = document.getElementById('setting-close-launch');
    const gameBoosterCheck = document.getElementById('setting-game-booster');
    const controllerModeCheck = document.getElementById('setting-controller-mode');
    const cheatModeCheck = document.getElementById('setting-cheat-mode');
    const jvmArgsInput = document.getElementById('setting-jvm-args');
    const discordToggle = document.getElementById('setting-discord-toggle');
    const themeButtons = document.querySelectorAll('.theme-color-btn');
    const bgPathInput = document.getElementById('setting-bg-path');
    const btnSelectBg = document.getElementById('btn-select-bg');
    const btnClearBg = document.getElementById('btn-clear-bg');

    const musicPathInput = document.getElementById('setting-music-path');
    const btnSelectMusic = document.getElementById('btn-select-music');
    const btnClearMusic = document.getElementById('btn-clear-music');
    const btnToggleMusic = document.getElementById('btn-toggle-music');
    const iconMusicPlay = document.getElementById('icon-music-play');
    const iconMusicPause = document.getElementById('icon-music-pause');
    const musicVolumeGroup = document.getElementById('music-volume-group');
    const musicVolumeSlider = document.getElementById('setting-music-volume');
    const musicVolumeValue = document.getElementById('music-volume-value');
    const lowPerfCheck = document.getElementById('setting-low-perf');

    const musicSearchInput = document.getElementById('input-music-search');
    const btnMusicSearch = document.getElementById('btn-music-search');
    const musicSearchResults = document.getElementById('music-search-results');
    const updaterCurrentVersion = document.getElementById('updater-current-version');
    const updaterAvailableVersion = document.getElementById('updater-available-version');
    const updaterLastChecked = document.getElementById('updater-last-checked');
    const updaterStatusBadge = document.getElementById('updater-status-badge');
    const updaterStatusText = document.getElementById('updater-status-text');
    const updaterError = document.getElementById('updater-error');
    const updaterProgressWrap = document.getElementById('updater-progress-wrap');
    const updaterProgressBar = document.getElementById('updater-progress-bar');
    const updaterProgressText = document.getElementById('updater-progress-text');
    const updaterReleaseBox = document.getElementById('updater-release-box');
    const updaterReleaseName = document.getElementById('updater-release-name');
    const updaterReleaseDate = document.getElementById('updater-release-date');
    const updaterReleaseNotes = document.getElementById('updater-release-notes');
    const btnUpdaterCheck = document.getElementById('btn-updater-check');
    const btnUpdaterDownload = document.getElementById('btn-updater-download');
    const btnUpdaterInstall = document.getElementById('btn-updater-install');

    // 1. Fetch versions
    const versions = await window.electronAPI.getVersions();
    versionSelect.innerHTML = versions.map(v => `<option value="${v}">${v}</option>`).join('');

    // 2. Fetch System Memory
    const memInfo = await window.electronAPI.getMemoryInfo();
    const totalMem = memInfo.totalMemGB;
    ramSlider.max = totalMem;
    
    // 3. Load Settings from Store
    const savedSettings = await window.electronAPI.getSettings();
    state.set('settings', savedSettings);

    // Populate UI
    if (savedSettings.version) versionSelect.value = savedSettings.version;
    if (savedSettings.loader?.type) loaderSelect.value = savedSettings.loader.type;
    
    const savedMaxRam = parseInt(savedSettings.memory?.max) || 4;
    ramSlider.value = savedMaxRam > totalMem ? totalMem : savedMaxRam;
    ramValue.textContent = `${ramSlider.value} Go`;

    if (savedSettings.java?.path) javaPathInput.value = savedSettings.java.path;
    if (savedSettings.closeOnLaunch !== undefined) closeOnLaunchCheck.checked = savedSettings.closeOnLaunch;
    if (savedSettings.gameBooster !== undefined) gameBoosterCheck.checked = savedSettings.gameBooster;
    if (savedSettings.controllerMode !== undefined && controllerModeCheck) controllerModeCheck.checked = savedSettings.controllerMode;
    if (savedSettings.cheatMode && cheatModeCheck) cheatModeCheck.checked = savedSettings.cheatMode;
    if (savedSettings.jvmArgs) jvmArgsInput.value = savedSettings.jvmArgs;
    if (savedSettings.lowPerfMode !== undefined && lowPerfCheck) {
        lowPerfCheck.checked = savedSettings.lowPerfMode;
        if (savedSettings.lowPerfMode) document.body.classList.add('low-perf');
    }

    // Background setup
    if (savedSettings.backgroundPath) {
        if (bgPathInput) bgPathInput.value = savedSettings.backgroundPath;
        applyBackground(savedSettings.backgroundPath);
    }

    // Music setup
    if (savedSettings.musicPath) {
        if (musicPathInput) musicPathInput.value = savedSettings.musicPath.startsWith('youtube:') ? 'YouTube: ' + savedSettings.musicPath.split(':')[1] : savedSettings.musicPath;
        if (musicVolumeSlider) musicVolumeSlider.value = savedSettings.musicVolume || 50;
        if (musicVolumeValue) musicVolumeValue.textContent = `${savedSettings.musicVolume || 50}%`;
        if (musicVolumeGroup) musicVolumeGroup.style.display = 'block';
        if (btnToggleMusic) btnToggleMusic.disabled = false;
        
        applyMusic(savedSettings.musicPath, savedSettings.musicVolume || 50);
    }

    // Theme setup
    const savedTheme = savedSettings.theme || '139, 92, 246'; // Default purple
    document.documentElement.style.setProperty('--accent', `rgba(${savedTheme}, 1)`);
    document.documentElement.style.setProperty('--accent-glow', `rgba(${savedTheme}, 0.5)`);
    document.documentElement.style.setProperty('--accent-light', `rgba(${savedTheme}, 0.8)`);
    document.documentElement.style.setProperty('--accent-dark', `rgba(${savedTheme}, 0.3)`);
    themeButtons.forEach(btn => {
        if (btn.dataset.color === savedTheme) {
            btn.classList.add('active');
        } else {
            btn.classList.remove('active');
        }
    });

    // Discord
    const storeGet = window.electronAPI.storeGet;
    if (storeGet) {
        const discordConfig = await storeGet('discordRpc');
        if (discordToggle && discordConfig) {
            discordToggle.checked = discordConfig.enabled;
        }
    }

    let latestUpdaterState = {};

    function syncUpdaterUI(updater = {}) {
        latestUpdaterState = updater || {};
        const badge = getUpdaterBadge(updater.status);
        const progress = Number(updater.progressPercent || 0);
        const speed = formatUpdaterSpeed(updater.bytesPerSecond);
        const canCheck = updater.supported && !['checking', 'downloading'].includes(updater.status);
        const canDownload = updater.supported && updater.status === 'available';
        const canInstall = updater.supported && updater.status === 'downloaded';
        const hasReleaseInfo = Boolean(updater.availableVersion || updater.releaseNotes || updater.releaseName);

        if (updaterCurrentVersion) updaterCurrentVersion.textContent = updater.currentVersion || '-';
        if (updaterAvailableVersion) updaterAvailableVersion.textContent = updater.availableVersion || 'Aucune';
        if (updaterLastChecked) updaterLastChecked.textContent = formatUpdaterDate(updater.lastCheckedAt);

        if (updaterStatusBadge) {
            updaterStatusBadge.textContent = badge.label;
            updaterStatusBadge.style.background = badge.background;
            updaterStatusBadge.style.color = badge.color;
            updaterStatusBadge.style.borderColor = badge.border;
        }

        if (updaterStatusText) {
            updaterStatusText.textContent = updater.message || 'Prêt à vérifier les mises à jour.';
        }

        if (updaterError) {
            const hasError = Boolean(updater.error);
            updaterError.textContent = hasError ? updater.error : '';
            updaterError.classList.toggle('hidden', !hasError);
        }

        if (updaterProgressWrap) {
            updaterProgressWrap.classList.toggle('hidden', updater.status !== 'downloading');
        }

        if (updaterProgressBar) {
            updaterProgressBar.style.width = `${Math.max(0, Math.min(progress, 100))}%`;
        }

        if (updaterProgressText) {
            updaterProgressText.textContent = updater.status === 'downloading'
                ? `${progress.toFixed(1)}%${speed ? ` • ${speed}` : ''}`
                : '0%';
        }

        if (updaterReleaseBox) {
            updaterReleaseBox.classList.toggle('hidden', !hasReleaseInfo);
        }

        if (updaterReleaseName) {
            updaterReleaseName.textContent = updater.releaseName || (updater.availableVersion ? `Version ${updater.availableVersion}` : 'Nouvelle version');
        }

        if (updaterReleaseDate) {
            updaterReleaseDate.textContent = updater.releaseDate ? formatUpdaterDate(updater.releaseDate) : '';
        }

        if (updaterReleaseNotes) {
            updaterReleaseNotes.textContent = updater.releaseNotes || 'Aucune note de version fournie.';
        }

        if (btnUpdaterCheck) btnUpdaterCheck.disabled = !canCheck;
        if (btnUpdaterDownload) {
            btnUpdaterDownload.disabled = !canDownload;
            btnUpdaterDownload.classList.toggle('hidden', !canDownload);
        }
        if (btnUpdaterInstall) {
            btnUpdaterInstall.disabled = !canInstall;
            btnUpdaterInstall.classList.toggle('hidden', !canInstall);
        }
    }

    if (window.electronAPI.getUpdaterState) {
        const updaterState = await window.electronAPI.getUpdaterState();
        syncUpdaterUI(updaterState || {});
        window.electronAPI.onUpdaterState?.(syncUpdaterUI);
    }

    async function runUpdaterAction(button, pendingLabel, action) {
        if (!button || typeof action !== 'function') return null;

        const originalHTML = button.innerHTML;
        button.innerHTML = `<div class="spinner" style="width:14px;height:14px;border-width:2px;"></div> ${pendingLabel}`;
        button.disabled = true;

        try {
            return await action();
        } finally {
            button.innerHTML = originalHTML;
            syncUpdaterUI(latestUpdaterState || {});
        }
    }

    // Event Listeners for immediate save (Zero Friction)
    const saveSetting = async () => {
        const currentSettings = state.get('settings') || savedSettings || {};
        const currentMusicPath = currentSettings.musicPath || savedSettings.musicPath;
        const newSettings = {
            version: versionSelect.value,
            loader: {
                type: loaderSelect.value,
                build: 'latest',
                enable: loaderSelect.value !== 'vanilla'
            },
            memory: {
                min: Math.max(1, Math.floor(parseInt(ramSlider.value) / 2)),
                max: parseInt(ramSlider.value)
            },
            java: {
                path: javaPathInput.value || null,
                type: 'jre'
            },
            screen: currentSettings.screen || savedSettings.screen || {
                width: null,
                height: null,
                fullscreen: false
            },
            closeOnLaunch: closeOnLaunchCheck.checked,
            gameBooster: gameBoosterCheck.checked,
            controllerMode: controllerModeCheck ? controllerModeCheck.checked : false,
            cheatMode: cheatModeCheck ? cheatModeCheck.checked : false,
            jvmArgs: jvmArgsInput.value,
            gameArgs: currentSettings.gameArgs || savedSettings.gameArgs || '',
            quickJoin: currentSettings.quickJoin || savedSettings.quickJoin || null,
            lowPerfMode: lowPerfCheck ? lowPerfCheck.checked : false,
            theme: document.querySelector('.theme-color-btn.active')?.dataset.color || '139, 92, 246',
            backgroundPath: bgPathInput?.value || null,
            bgBlur: bgBlurSlider ? parseInt(bgBlurSlider.value) : 0,
            musicPath: currentMusicPath || null,
            musicVolume: musicVolumeSlider ? parseInt(musicVolumeSlider.value) : 50
        };

        state.set('settings', newSettings);
        
        if (newSettings.lowPerfMode) document.body.classList.add('low-perf');
        else document.body.classList.remove('low-perf');

        await window.electronAPI.saveSettings(newSettings);
    };

    const applyLowEndProfile = async () => {
        const lowRam = getLowEndRam(totalMem);

        if (ramSlider) {
            ramSlider.value = lowRam;
            ramValue.textContent = `${lowRam} Go`;
        }

        if (jvmArgsInput) jvmArgsInput.value = LOW_END_JVM_ARGS;
        if (gameBoosterCheck) gameBoosterCheck.checked = true;
        if (lowPerfCheck) lowPerfCheck.checked = true;

        document.body.classList.add('low-perf');
        await saveSetting();
        notify('Mode PC faible activé : Minecraft sera lancé en profil léger.', 'success');
    };

    const checkLoader = () => {
        const isVanilla = loaderSelect.value === 'vanilla';
        if (controllerModeCheck) {
            controllerModeCheck.disabled = isVanilla;
            if (isVanilla) controllerModeCheck.checked = false;
        }
    };
    checkLoader();

    versionSelect?.addEventListener('change', saveSetting);
    loaderSelect?.addEventListener('change', () => {
        checkLoader();
        saveSetting();
    });
    closeOnLaunchCheck?.addEventListener('change', saveSetting);
    gameBoosterCheck?.addEventListener('change', saveSetting);
    lowPerfCheck?.addEventListener('change', async () => {
        if (lowPerfCheck.checked) {
            await applyLowEndProfile();
        } else {
            document.body.classList.remove('low-perf');
            await saveSetting();
            notify('Mode PC faible désactivé.', 'info');
        }
    });
    controllerModeCheck?.addEventListener('change', saveSetting);
    
    // Cheat Mode Modal Logic
    if (cheatModeCheck) {
        cheatModeCheck.addEventListener('change', (e) => {
            const modal = document.getElementById('modal-cheat-warning');
            if (e.target.checked && modal) {
                // Show warning
                modal.classList.remove('hidden');
                // Temporarily uncheck it until they accept
                e.target.checked = false;
            } else {
                saveSetting();
                notify('Mode Legit activé. Les cheats sont bloqués.', 'success');
            }
        });
    }

    const btnCheatAccept = document.getElementById('btn-cheat-accept');
    const btnCheatCancel = document.getElementById('btn-cheat-cancel');
    const modalCheatWarning = document.getElementById('modal-cheat-warning');

    if (btnCheatAccept && cheatModeCheck) {
        btnCheatAccept.addEventListener('click', () => {
            modalCheatWarning.classList.add('hidden');
            cheatModeCheck.checked = true;
            saveSetting();
            notify('Mode Anarchie / Cheat activé. Sécurité désactivée.', 'error');
        });
    }

    if (btnCheatCancel) {
        btnCheatCancel.addEventListener('click', () => {
            modalCheatWarning.classList.add('hidden');
            if (cheatModeCheck) cheatModeCheck.checked = false;
        });
    }
    
    // Debounce for inputs
    let timeout;
    jvmArgsInput?.addEventListener('input', () => {
        clearTimeout(timeout);
        timeout = setTimeout(saveSetting, 500);
    });

    ramSlider?.addEventListener('input', (e) => {
        ramValue.textContent = `${e.target.value} Go`;
    });
    
    ramSlider?.addEventListener('change', saveSetting);

    btnSelectJava?.addEventListener('click', async () => {
        const result = await window.electronAPI.selectJavaPath();
        if (result.success) {
            javaPathInput.value = result.path;
            await saveSetting();
            notify('Chemin Java mis à jour', 'success');
        }
    });

    // Background Selector
    const bgBlurSlider = document.getElementById('setting-bg-blur');
    const bgBlurValue = document.getElementById('bg-blur-value');
    const bgBlurGroup = document.getElementById('bg-blur-group');

    function showBlurSlider(show) {
        if (bgBlurGroup) bgBlurGroup.style.display = show ? 'block' : 'none';
    }

    function applyBlur(val) {
        document.documentElement.style.setProperty('--bg-blur', `${val}px`);
        if (bgBlurValue) bgBlurValue.textContent = `${val}px`;
    }

    // Restore blur on load
    if (savedSettings.backgroundPath) {
        showBlurSlider(true);
        const savedBlur = savedSettings.bgBlur || 0;
        if (bgBlurSlider) bgBlurSlider.value = savedBlur;
        applyBlur(savedBlur);
    }

    btnSelectBg?.addEventListener('click', async () => {
        const result = await window.electronAPI.selectBackground();
        if (result.success) {
            if (bgPathInput) bgPathInput.value = result.path;
            applyBackground(result.path);
            showBlurSlider(true);
            await saveSetting();
            notify('Fond d\'écran mis à jour', 'success');
        }
    });

    btnClearBg?.addEventListener('click', async () => {
        if (bgPathInput) bgPathInput.value = '';
        clearBackground();
        showBlurSlider(false);
        if (bgBlurSlider) bgBlurSlider.value = 0;
        applyBlur(0);
        await saveSetting();
        notify('Fond d\'écran réinitialisé', 'info');
    });

    bgBlurSlider?.addEventListener('input', (e) => {
        applyBlur(e.target.value);
    });
    bgBlurSlider?.addEventListener('change', saveSetting);

    // --- Music Selector ---
    function updateMusicToggleIcon() {
        if (!iconMusicPlay || !iconMusicPause) return;
        if (window.bgAudio.paused) {
            iconMusicPlay.style.display = 'block';
            iconMusicPause.style.display = 'none';
        } else {
            iconMusicPlay.style.display = 'none';
            iconMusicPause.style.display = 'block';
        }
    }

    btnSelectMusic?.addEventListener('click', async () => {
        const result = await window.electronAPI.selectMusic();
        if (result.success) {
            if (musicPathInput) musicPathInput.value = result.path;
            if (musicVolumeGroup) musicVolumeGroup.style.display = 'block';
            if (btnToggleMusic) btnToggleMusic.disabled = false;
            
            const vol = musicVolumeSlider ? parseInt(musicVolumeSlider.value) : 50;
            const settings = state.get('settings');
            settings.musicPath = result.path;
            state.set('settings', settings);
            
            applyMusic(result.path, vol);
            updateMusicToggleIcon();
            
            await saveSetting();
            notify('Musique de fond définie', 'success');
        }
    });

    // --- YouTube Music Search ---
    async function searchMusic() {
        const query = musicSearchInput.value.trim();
        if (!query) return;

        btnMusicSearch.disabled = true;
        btnMusicSearch.textContent = '...';
        musicSearchResults.style.display = 'flex';
        musicSearchResults.innerHTML = '<p class="form-hint" style="text-align:center;padding:10px;">Chargement...</p>';

        const res = await window.electronAPI.searchMusic(query);
        btnMusicSearch.disabled = false;
        btnMusicSearch.textContent = 'Chercher';

        if (res.success && res.results.length > 0) {
            musicSearchResults.innerHTML = '';
            res.results.slice(0, 8).forEach(track => {
                const item = document.createElement('div');
                item.style.cssText = 'display:flex;align-items:center;gap:10px;padding:6px;background:rgba(255,255,255,0.03);border-radius:4px;cursor:pointer;transition:0.2s;';
                item.innerHTML = `
                    <img src="${track.thumbnail}" style="width:40px;height:40px;border-radius:4px;object-fit:cover;">
                    <div style="flex:1;min-width:0;">
                        <div style="font-size:12px;font-weight:600;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;color:#fff;">${track.title}</div>
                        <div style="font-size:10px;color:var(--text-muted);">${track.author} • ${track.duration}</div>
                    </div>
                `;
                item.onclick = async () => {
                    const videoId = track.id;
                    const path = `youtube:${videoId}`;
                    musicPathInput.value = '🎵 ' + track.title;
                    if (musicVolumeGroup) musicVolumeGroup.style.display = 'block';
                    if (btnToggleMusic) btnToggleMusic.disabled = false;

                    const settings = state.get('settings');
                    settings.musicPath = path;
                    state.set('settings', settings);

                    await applyMusic(path, musicVolumeSlider ? parseInt(musicVolumeSlider.value) : 50);
                    updateMusicToggleIcon();
                    await saveSetting();
                    notify(`Lecture de : ${track.title}`, 'success');
                };
                item.onmouseenter = () => item.style.background = 'rgba(255,255,255,0.08)';
                item.onmouseleave = () => item.style.background = 'rgba(255,255,255,0.03)';
                musicSearchResults.appendChild(item);
            });
        } else {
            musicSearchResults.innerHTML = '<p class="form-hint" style="text-align:center;padding:10px;">Aucun résultat trouvé.</p>';
        }
    }

    btnMusicSearch?.addEventListener('click', searchMusic);
    musicSearchInput?.addEventListener('keydown', (e) => {
        if (e.key === 'Enter') searchMusic();
    });

    btnClearMusic?.addEventListener('click', async () => {
        if (musicPathInput) musicPathInput.value = '';
        if (musicVolumeGroup) musicVolumeGroup.style.display = 'none';
        if (btnToggleMusic) btnToggleMusic.disabled = true;
        
        const settings = state.get('settings');
        settings.musicPath = null;
        state.set('settings', settings);

        applyMusic(null);
        updateMusicToggleIcon();
        
        await saveSetting();
        notify('Musique désactivée', 'info');
    });

    btnToggleMusic?.addEventListener('click', () => {
        if (!window.bgAudio.src) return;
        if (window.bgAudio.paused) window.bgAudio.play();
        else window.bgAudio.pause();
        updateMusicToggleIcon();
    });


    musicVolumeSlider?.addEventListener('input', (e) => {
        if (musicVolumeValue) musicVolumeValue.textContent = `${e.target.value}%`;
        window.bgAudio.volume = parseInt(e.target.value) / 100;
    });
    musicVolumeSlider?.addEventListener('change', saveSetting);

    // Initial icon state
    window.bgAudio.addEventListener('play', updateMusicToggleIcon);
    window.bgAudio.addEventListener('pause', updateMusicToggleIcon);
    updateMusicToggleIcon();

    // Discord RPC Reconnect
    const btnDiscordReconnect = document.getElementById('btn-discord-reconnect');
    btnDiscordReconnect?.addEventListener('click', async () => {
        const originalHTML = btnDiscordReconnect.innerHTML;
        btnDiscordReconnect.innerHTML = '<div class="spinner" style="width:14px;height:14px;border-width:2px;"></div> Reconnexion...';
        btnDiscordReconnect.disabled = true;

        // Auto-enable if disabled
        if (discordToggle && !discordToggle.checked) {
            discordToggle.checked = true;
            await window.electronAPI.discordToggle(true);
        }

        const connected = await window.electronAPI.discordReconnect();
        
        btnDiscordReconnect.innerHTML = originalHTML;
        btnDiscordReconnect.disabled = false;

        if (connected) {
            notify('Discord RPC reconnecté avec succès !', 'success');
        } else {
            notify('Impossible de se connecter à Discord. Vérifie que Discord est ouvert.', 'error');
        }
    });

    // Discord Toggle
    discordToggle?.addEventListener('change', async (e) => {
        const enabled = e.target.checked;
        await window.electronAPI.discordToggle(enabled);
        notify(enabled ? 'Discord RPC Activé' : 'Discord RPC Désactivé', 'info');
    });

    btnUpdaterCheck?.addEventListener('click', async () => {
        const result = await runUpdaterAction(
            btnUpdaterCheck,
            'Vérification...',
            () => window.electronAPI.checkForLauncherUpdates?.()
        );

        if (result?.success === false && result.error) {
            notify(`Impossible de vérifier les mises à jour : ${result.error}`, 'error');
        }
    });

    btnUpdaterDownload?.addEventListener('click', async () => {
        const result = await runUpdaterAction(
            btnUpdaterDownload,
            'Téléchargement...',
            () => window.electronAPI.downloadLauncherUpdate?.()
        );

        if (result?.success === false) {
            notify(result.error || 'Le téléchargement de la mise à jour a échoué.', 'error');
        }
    });

    btnUpdaterInstall?.addEventListener('click', async () => {
        const result = await runUpdaterAction(
            btnUpdaterInstall,
            'Installation...',
            () => window.electronAPI.installLauncherUpdate?.()
        );

        if (result?.success) {
            notify('Redémarrage du launcher pour installer la mise à jour...', 'info');
            return;
        }

        if (result?.error) {
            notify(result.error, 'error');
        }
    });

    // Theme Switcher
    themeButtons.forEach(btn => {
        btn.addEventListener('click', async () => {
            themeButtons.forEach(b => b.classList.remove('active'));
            btn.classList.add('active');
            
            const color = btn.dataset.color;
            document.documentElement.style.setProperty('--accent', `rgba(${color}, 1)`);
            document.documentElement.style.setProperty('--accent-glow', `rgba(${color}, 0.5)`);
            document.documentElement.style.setProperty('--accent-light', `rgba(${color}, 0.8)`);
            document.documentElement.style.setProperty('--accent-dark', `rgba(${color}, 0.3)`);
            
            await saveSetting();
        });
    });

    // Clear Cache
    const btnClearCache = document.getElementById('btn-clear-cache');
    if (btnClearCache) {
        btnClearCache.addEventListener('click', async () => {
            const originalText = btnClearCache.innerHTML;
            btnClearCache.innerHTML = '<div class="spinner" style="width:14px;height:14px;border-width:2px;"></div> Nettoyage...';
            btnClearCache.disabled = true;

            const res = await window.electronAPI.clearCache();
            
            btnClearCache.innerHTML = originalText;
            btnClearCache.disabled = false;

            if (res.success) {
                notify('Cache et logs nettoyés avec succès !', 'success');
            } else {
                notify('Erreur lors du nettoyage : ' + res.error, 'error');
            }
        });
    }

    // ── Smart Diagnostics & Auto-Optimize ──
    const btnRunDiag = document.getElementById('btn-run-diag');
    const btnAutoOptimize = document.getElementById('btn-auto-optimize');
    const optimizeResult = document.getElementById('optimize-result');

    async function runDiagnostics() {
        const specs = await window.electronAPI.getSystemSpecs();
        const memInfo = await window.electronAPI.getMemoryInfo();

        const gpuEl = document.getElementById('diag-gpu');
        if (gpuEl) {
            gpuEl.textContent = specs.gpu || 'Inconnu';
            gpuEl.title = specs.gpu || 'Inconnu'; // Show full name on hover if truncated
        }

        document.getElementById('diag-cpu').textContent = specs.cpu.length > 30 ? specs.cpu.substring(0, 30) + '…' : specs.cpu;
        document.getElementById('diag-cores').textContent = `${specs.cores} cœurs`;
        document.getElementById('diag-ram').textContent = `${specs.ram} Go`;
        document.getElementById('diag-ram-free').textContent = `${memInfo.totalMemGB - Math.floor(specs.ram * 0.6)} Go libre (estimé)`;

        // Calculate performance score
        let score = 0;
        score += Math.min(specs.cores * 10, 80); // max 80 for cores
        score += Math.min(specs.ram * 3, 60); // max 60 for RAM
        if (specs.cpu.toLowerCase().includes('i9') || specs.cpu.toLowerCase().includes('ryzen 9')) score += 30;
        else if (specs.cpu.toLowerCase().includes('i7') || specs.cpu.toLowerCase().includes('ryzen 7')) score += 25;
        else if (specs.cpu.toLowerCase().includes('i5') || specs.cpu.toLowerCase().includes('ryzen 5')) score += 15;
        score = Math.min(score, 100);

        const scoreEl = document.getElementById('diag-score');
        if (score >= 75) {
            scoreEl.textContent = `${score}/100 🟢 Excellent`;
            scoreEl.style.color = 'rgb(16, 185, 129)';
        } else if (score >= 45) {
            scoreEl.textContent = `${score}/100 🟡 Correct`;
            scoreEl.style.color = 'rgb(245, 158, 11)';
        } else {
            scoreEl.textContent = `${score}/100 🔴 Faible`;
            scoreEl.style.color = 'var(--danger)';
        }

        return { specs, memInfo, score };
    }

    btnRunDiag?.addEventListener('click', async () => {
        btnRunDiag.disabled = true;
        btnRunDiag.innerHTML = '<div class="spinner" style="width:14px;height:14px;border-width:2px;"></div> Analyse...';
        
        await runDiagnostics();
        
        btnRunDiag.innerHTML = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="width:14px;height:14px;"><path d="M22 12h-4l-3 9L9 3l-3 9H2"></path></svg> Analyser mon PC';
        btnRunDiag.disabled = false;
        notify('Diagnostic terminé !', 'success');
    });

    btnAutoOptimize?.addEventListener('click', async () => {
        btnAutoOptimize.disabled = true;
        btnAutoOptimize.innerHTML = '<div class="spinner" style="width:14px;height:14px;border-width:2px;"></div> Optimisation...';

        const { specs, memInfo, score } = await runDiagnostics();
        const totalRam = specs.ram;
        const cores = specs.cores;
        const changes = [];

        // 1. Smart RAM allocation
        let optimalRam;
        if (totalRam >= 32) optimalRam = 8;
        else if (totalRam >= 16) optimalRam = 6;
        else if (totalRam >= 8) optimalRam = 4;
        else if (totalRam >= 4) optimalRam = 2;
        else optimalRam = 1;

        ramSlider.value = optimalRam;
        ramValue.textContent = `${optimalRam} Go`;
        changes.push(`✅ RAM → ${optimalRam} Go (sur ${totalRam} Go)`);

        // 2. Smart JVM Flags based on specs
        let flags = [];
        flags.push('-XX:+UseG1GC');
        flags.push('-XX:+UnlockExperimentalVMOptions');
        
        if (totalRam >= 8) {
            flags.push('-XX:G1NewSizePercent=30');
            flags.push('-XX:G1MaxNewSizePercent=40');
            flags.push('-XX:G1HeapRegionSize=8M');
            flags.push('-XX:G1ReservePercent=20');
        } else {
            flags.push('-XX:G1NewSizePercent=20');
            flags.push('-XX:G1MaxNewSizePercent=30');
            flags.push('-XX:G1HeapRegionSize=4M');
            flags.push('-XX:G1ReservePercent=15');
        }

        flags.push(`-XX:ParallelGCThreads=${Math.max(2, Math.min(cores - 1, 8))}`);
        flags.push(`-XX:ConcGCThreads=${Math.max(1, Math.floor(cores / 4))}`);
        flags.push('-XX:+ParallelRefProcEnabled');
        flags.push('-XX:MaxGCPauseMillis=50');
        flags.push('-XX:+DisableExplicitGC');
        flags.push('-XX:+AlwaysPreTouch');
        flags.push('-XX:-UseAdaptiveSizePolicy');

        if (jvmArgsInput) jvmArgsInput.value = flags.join(' ');
        changes.push(`✅ JVM → ${flags.length} flags optimisés (${cores} cœurs)`);

        // 3. Enable Game Booster
        if (gameBoosterCheck) {
            gameBoosterCheck.checked = true;
            changes.push('✅ Game Booster → Activé');
        }

        // 4. Enable low-end game profile on weak machines
        if (score < 45 && lowPerfCheck) {
            lowPerfCheck.checked = true;
            document.body.classList.add('low-perf');
            changes.push('✅ Mode PC faible → Activé');
        }

        // 5. Show results
        if (optimizeResult) {
            optimizeResult.style.display = 'block';
            optimizeResult.innerHTML = `<strong>⚡ Optimisation appliquée :</strong><br>${changes.join('<br>')}`;
        }

        await saveSetting();

        btnAutoOptimize.innerHTML = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="width:14px;height:14px;"><polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"></polygon></svg> Optimiser Automatiquement';
        btnAutoOptimize.disabled = false;
        notify('🚀 Optimisation terminée ! Config ajustée à ton PC.', 'success');
    });

    const btnOpenMcFolder = document.getElementById('btn-open-mc-folder');
    btnOpenMcFolder?.addEventListener('click', async () => {
        await window.electronAPI.openMcFolder();
    });

    // Export Logs
    const btnExportLogs = document.getElementById('btn-export-logs');
    btnExportLogs?.addEventListener('click', async () => {
        const result = await window.electronAPI.exportLogs();
        if (result?.success) {
            notify('Logs exportés avec succès !', 'success');
        } else {
            notify('Aucun log à exporter.', 'info');
        }
    });

    // Dev Console Toggle
    const devConsoleToggle = document.getElementById('setting-dev-console');
    if (devConsoleToggle) {
        const devEnabled = await window.electronAPI.storeGet?.('devConsole');
        if (devEnabled) devConsoleToggle.checked = true;

        devConsoleToggle.addEventListener('change', async (e) => {
            await window.electronAPI.storeSet?.('devConsole', e.target.checked);
            if (e.target.checked) {
                notify('Console Dev activée au prochain démarrage.', 'info');
            } else {
                notify('Console Dev désactivée.', 'info');
            }
        });
    }

    // Reset All Settings
    const btnReset = document.getElementById('btn-reset-settings');
    btnReset?.addEventListener('click', async () => {
        if (!confirm('⚠️ Es-tu sûr ? Tous tes paramètres seront réinitialisés.')) return;
        
        await window.electronAPI.saveSettings({});
        await window.electronAPI.storeSet?.('devConsole', false);
        notify('Paramètres réinitialisés. Redémarrez le launcher.', 'success');
        setTimeout(() => location.reload(), 1500);
    });

    // Discord VIP Linking
    const btnDiscordLink = document.getElementById('btn-discord-link');
    const btnDiscordUnlink = document.getElementById('btn-discord-unlink');
    const vipLinkedUI = document.getElementById('discord-vip-linked');
    const vipUnlinkedUI = document.getElementById('discord-vip-unlinked');
    const themeColorDiscord = document.getElementById('theme-color-discord');

    function updateDiscordVIPUI(profile) {
        if (profile) {
            vipUnlinkedUI.style.display = 'none';
            vipLinkedUI.style.display = 'flex';
            document.getElementById('discord-vip-avatar').src = profile.avatarUrl || 'data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="%235865F2" stroke-width="2"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>';
            document.getElementById('discord-vip-username').textContent = profile.username;
            if (themeColorDiscord) themeColorDiscord.style.display = 'block'; // Unlock blurple
            if (window.setVIPParticles) window.setVIPParticles(true);
        } else {
            vipUnlinkedUI.style.display = 'flex';
            vipLinkedUI.style.display = 'none';
            if (themeColorDiscord) {
                themeColorDiscord.style.display = 'none';
                if (themeColorDiscord.classList.contains('active')) {
                    // Reset theme if they unlink
                    themeButtons[0].click();
                }
            }
            if (window.setVIPParticles) window.setVIPParticles(false);
        }
        // Also trigger sidebar update
        if (window.updateSidebarDiscordAvatar) window.updateSidebarDiscordAvatar(profile);
        // Refresh mods UI if it's open to reveal the VIP pack
        if (window.refreshVIPMods) window.refreshVIPMods(!!profile);
    }

    if (storeGet) {
        storeGet('discordProfile').then(profile => {
            updateDiscordVIPUI(profile);
        });
    }

    btnDiscordLink?.addEventListener('click', async () => {
        const ogHtml = btnDiscordLink.innerHTML;
        btnDiscordLink.innerHTML = '<div class="spinner" style="width:14px;height:14px;border-width:2px;border-top-color:#fff;"></div> Connexion...';
        btnDiscordLink.disabled = true;

        const res = await window.electronAPI.linkDiscord();
        
        btnDiscordLink.innerHTML = ogHtml;
        btnDiscordLink.disabled = false;

        if (res.success) {
            notify(`Compte Discord lié : ${res.profile.username} ! 🎉`, 'success');
            updateDiscordVIPUI(res.profile);
            
            // Auto switch to blurple theme to show them the reward!
            if (themeColorDiscord) themeColorDiscord.click();
        } else {
            notify(res.error, 'error');
        }
    });

    btnDiscordUnlink?.addEventListener('click', async () => {
        await window.electronAPI.storeSet('discordProfile', null);
        updateDiscordVIPUI(null);
        notify('Compte Discord délié.', 'info');
    });

    // Snapshots
    const btnCreateSnapshot = document.getElementById('btn-create-snapshot');
    const inputSnapshotName = document.getElementById('input-snapshot-name');
    const snapshotsList = document.getElementById('snapshots-list');

    async function loadSnapshots() {
        if (!snapshotsList) return;
        const res = await window.electronAPI.listSnapshots();
        if (!res.success || !res.snapshots || res.snapshots.length === 0) {
            snapshotsList.innerHTML = '<p class="form-hint">Aucun snapshot</p>';
            return;
        }

        snapshotsList.innerHTML = '';
        res.snapshots.forEach(snap => {
            const row = document.createElement('div');
            row.style.cssText = 'display:flex;align-items:center;gap:8px;padding:8px 12px;background:rgba(255,255,255,0.03);border:1px solid var(--border-glass);border-radius:var(--radius-sm);';
            const date = snap.date ? new Date(snap.date).toLocaleDateString('fr-FR', { day:'2-digit', month:'short', year:'numeric', hour:'2-digit', minute:'2-digit' }) : '';
            row.innerHTML = `
                <div style="flex:1;min-width:0;">
                    <div style="font-weight:600;font-size:13px;">${snap.name || snap.id}</div>
                    <div style="font-size:11px;color:var(--text-muted);">${snap.version || ''} ${snap.loader || ''} — ${date}</div>
                </div>
                <button class="btn btn-sm btn-secondary btn-restore" style="font-size:11px;padding:4px 10px;">Restaurer</button>
                <button class="btn-icon btn-snap-delete" title="Supprimer" style="padding:4px;">
                    <svg viewBox="0 0 24 24" fill="none" stroke="var(--danger)" stroke-width="2" style="width:14px;height:14px;"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg>
                </button>
            `;

            row.querySelector('.btn-restore').addEventListener('click', async () => {
                if (!confirm(`Restaurer "${snap.name}" ? Cela remplacera votre instance actuelle.`)) return;
                const r = await window.electronAPI.restoreSnapshot(snap.id);
                if (r.success) {
                    notify('Snapshot restauré avec succès', 'success');
                } else {
                    notify(r.error || 'Erreur de restauration', 'error');
                }
            });

            row.querySelector('.btn-snap-delete').addEventListener('click', async () => {
                if (!confirm(`Supprimer le snapshot "${snap.name}" ?`)) return;
                const r = await window.electronAPI.deleteSnapshot(snap.id);
                if (r.success) {
                    notify('Snapshot supprimé', 'info');
                    loadSnapshots();
                }
            });

            snapshotsList.appendChild(row);
        });
    }

    btnCreateSnapshot?.addEventListener('click', async () => {
        const name = inputSnapshotName?.value.trim() || `Snapshot ${new Date().toLocaleDateString('fr-FR')}`;
        btnCreateSnapshot.disabled = true;
        btnCreateSnapshot.textContent = 'Création...';
        
        const res = await window.electronAPI.createSnapshot(name);
        
        btnCreateSnapshot.disabled = false;
        btnCreateSnapshot.textContent = 'Créer';
        
        if (res.success) {
            if (inputSnapshotName) inputSnapshotName.value = '';
            notify('Snapshot créé avec succès', 'success');
            loadSnapshots();
        } else {
            notify(res.error || 'Erreur lors de la création', 'error');
        }
    });

    loadSnapshots();

    // --- Configuration Presets ---
    const btnPresetCompetitive = document.getElementById('preset-competitive');
    const btnPresetChill = document.getElementById('preset-chill');
    const btnPresetLowEnd = document.getElementById('preset-low-end');
    const btnPresetSmart = document.getElementById('preset-smart');

    const applyPreset = async (type) => {
        if (type === 'lowEnd') {
            await applyLowEndProfile();
            return;
        }

        const specs = await window.electronAPI.getSystemSpecs();
        let newRam = 4;
        let newJvmArgs = '';

        if (type === 'competitive') {
            // High performance flags (Aikar's flags style)
            newRam = Math.min(8, Math.max(4, Math.floor(specs.ram / 2)));
            newJvmArgs = '-XX:+UseG1GC -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=200 -XX:+UnlockExperimentalVMOptions -XX:+DisableExplicitGC -XX:+AlwaysPreTouch -XX:G1NewSizePercent=30 -XX:G1MaxNewSizePercent=40 -XX:G1HeapRegionSize=8M -XX:G1ReservePercent=20 -XX:G1HeapWastePercent=5 -XX:G1MixedGCCountTarget=4 -XX:InitiatingHeapOccupancyPercent=15 -XX:G1MixedGCLiveThresholdPercent=90 -XX:G1RSetUpdatingPauseTimePercent=5 -XX:SurvivorRatio=32 -XX:+PerfDisableSharedMem -XX:MaxTenuringThreshold=1';
            notify('Mode Compétitif appliqué : Flags optimisés & RAM ajustée.', 'success');
        } else if (type === 'chill') {
            newRam = 4;
            newJvmArgs = '-XX:+UseG1GC';
            notify('Mode Normal appliqué : Configuration équilibrée.', 'info');
        } else if (type === 'smart') {
            // Intelligent logic based on specs
            if (specs.ram >= 32) {
                newRam = 12;
            } else if (specs.ram >= 16) {
                newRam = 8;
            } else if (specs.ram >= 8) {
                newRam = 4;
            } else {
                newRam = 2;
            }
            
            // Flags based on CPU cores
            if (specs.cores >= 12) {
                newJvmArgs = '-XX:+UseG1GC -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=50 -XX:G1HeapRegionSize=16M';
            } else if (specs.cores >= 6) {
                newJvmArgs = '-XX:+UseG1GC -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=100';
            } else {
                newJvmArgs = '-XX:+UseG1GC -XX:MaxGCPauseMillis=200';
            }
            
            // Special tweak for older CPUs
            if (specs.cpu.toLowerCase().includes('pentium') || specs.cpu.toLowerCase().includes('celeron')) {
                newJvmArgs += ' -Xincgc';
                newRam = Math.min(newRam, 2);
            }

            notify(`Mode Intelligent : Config détectée (${specs.cpu.split('@')[0].trim()}), réglages optimisés !`, 'success');
        }

        // Update UI
        if (ramSlider) {
            ramSlider.value = newRam;
            ramValue.textContent = `${newRam} Go`;
        }
        if (jvmArgsInput) {
            jvmArgsInput.value = newJvmArgs;
        }
        if (lowPerfCheck) {
            lowPerfCheck.checked = false;
            document.body.classList.remove('low-perf');
        }
        
        await saveSetting();
    };

    btnPresetCompetitive?.addEventListener('click', () => applyPreset('competitive'));
    btnPresetChill?.addEventListener('click', () => applyPreset('chill'));
    btnPresetLowEnd?.addEventListener('click', () => applyPreset('lowEnd'));
    btnPresetSmart?.addEventListener('click', () => applyPreset('smart'));

    // --- Import Assistant Integration ---
    initImportAssistant();
}

async function initImportAssistant() {
    const btnOpenImport = document.getElementById('btn-open-import');
    const modalImport = document.getElementById('modal-import-assistant');
    const step1 = document.getElementById('import-step-1');
    const step2 = document.getElementById('import-step-2');
    const step3 = document.getElementById('import-step-3');
    const footer2 = document.getElementById('import-footer-2');
    const launchersList = document.getElementById('import-launchers-list');
    const launchersLoading = document.getElementById('import-launchers-loading');
    const btnBack = document.getElementById('btn-import-back');
    const instancesContainer = document.getElementById('import-instances-container');
    const instanceSelect = document.getElementById('import-instance-select');
    const btnConfirm = document.getElementById('btn-import-confirm');

    const progressBar = document.getElementById('import-progress-bar');
    const statusMsg = document.getElementById('import-status-msg');
    const statusTitle = document.getElementById('import-status-title');
    const spinner = document.getElementById('import-spinner');
    const successIcon = document.getElementById('import-success-icon');
    const finishedActions = document.getElementById('import-finished-actions');
    const btnOpenFolder = document.getElementById('btn-import-open-folder');
    const btnCloseModal = document.getElementById('btn-import-close');

    if (!btnOpenImport || !modalImport) return;

    let selectedLauncher = null;

    // Listen for progress
    window.electronAPI.onImportProgress((data) => {
        if (progressBar) progressBar.style.width = `${data.percent}%`;
        if (statusMsg) statusMsg.textContent = data.message;
    });

    btnOpenImport.addEventListener('click', async () => {
        modalImport.classList.remove('hidden');
        step1.classList.remove('hidden');
        step2.classList.add('hidden');
        step3.classList.add('hidden');
        footer2.style.display = 'none';
        
        launchersList.innerHTML = '';
        launchersLoading.style.display = 'block';

        const launchers = await window.electronAPI.importScan();
        launchersLoading.style.display = 'none';

        if (launchers.length === 0) {
            launchersList.innerHTML = '<p class="form-hint" style="text-align:center;">Aucun launcher compatible trouvé sur votre PC.</p>';
            return;
        }

        launchers.forEach(l => {
            const btn = document.createElement('button');
            btn.className = 'btn btn-secondary';
            btn.style.cssText = 'width: 100%; justify-content: flex-start; padding: 14px 16px; gap: 16px; margin-bottom: 8px; position: relative; overflow: hidden;';
            
            let iconColor = 'var(--accent)';
            if (l.id === 'lunar') iconColor = '#5eead4';
            if (l.id === 'feather') iconColor = '#60a5fa';
            if (l.id === 'curseforge') iconColor = '#fb923c';
            if (l.id === 'prism') iconColor = '#c084fc';
            if (l.id === 'modrinth') iconColor = '#10b981';

            btn.innerHTML = `
                <div style="width:42px;height:42px;background:${iconColor}15;border:1px solid ${iconColor}30;border-radius:12px;display:flex;align-items:center;justify-content:center;color:${iconColor};flex-shrink:0;">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" style="width:20px;height:20px;"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/></svg>
                </div>
                <div style="text-align: left; flex: 1; min-width: 0;">
                    <div style="font-weight: 700; font-size: 14px; color: #fff; margin-bottom: 2px;">${l.name}</div>
                    <div style="font-size: 11px; color: var(--text-muted); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; opacity: 0.7;">${l.path}</div>
                </div>
                <div style="color: var(--text-muted); opacity: 0.5;">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" style="width:16px;height:16px;"><polyline points="9 18 15 12 9 6"></polyline></svg>
                </div>
            `;
            btn.onclick = () => selectLauncher(l);
            launchersList.appendChild(btn);
        });
    });

    btnBack?.addEventListener('click', () => {
        step2.classList.add('hidden');
        footer2.style.display = 'none';
        step1.classList.remove('hidden');
    });

    async function selectLauncher(launcher) {
        selectedLauncher = launcher;
        step1.classList.add('hidden');
        step2.classList.remove('hidden');
        footer2.style.display = 'block';

        instancesContainer.style.display = 'none';
        instanceSelect.innerHTML = '';

        // All launchers can potentially have sub-instances or be the instance itself
        const instances = await window.electronAPI.importGetInstances(launcher.path);
        
        if (instances && instances.length > 0) {
            instancesContainer.style.display = 'block';
            instances.forEach(inst => {
                const opt = document.createElement('option');
                opt.value = inst.path;
                opt.textContent = inst.name;
                instanceSelect.appendChild(opt);
            });
        } else {
            // Root path as default instance
            instancesContainer.style.display = 'block';
            const opt = document.createElement('option');
            opt.value = launcher.path;
            opt.textContent = 'Dossier racine par défaut';
            instanceSelect.appendChild(opt);
        }
    }

    btnConfirm?.addEventListener('click', async () => {
        const items = [];
        if (document.getElementById('import-chk-options').checked) items.push('options');
        if (document.getElementById('import-chk-resourcepacks').checked) items.push('resourcepacks');
        if (document.getElementById('import-chk-mods').checked) items.push('mods');
        if (document.getElementById('import-chk-config').checked) items.push('config');

        if (items.length === 0) {
            notify('Veuillez sélectionner au moins un élément à importer.', 'error');
            return;
        }

        let sourcePath = selectedLauncher.path;
        if (instanceSelect.value) {
            sourcePath = instanceSelect.value;
        }

        // Switch to progress step
        step2.classList.add('hidden');
        footer2.style.display = 'none';
        step3.classList.remove('hidden');
        
        // Reset progress state
        progressBar.style.width = '0%';
        statusTitle.textContent = 'Importation en cours...';
        statusMsg.textContent = 'Préparation...';
        spinner.classList.remove('hidden');
        successIcon.classList.add('hidden');
        finishedActions.classList.add('hidden');

        const result = await window.electronAPI.importExecute({ sourcePath, items });

        if (result.success) {
            statusTitle.textContent = 'Importation terminée !';
            statusMsg.textContent = 'Tous les fichiers ont été copiés avec succès.';
            spinner.classList.add('hidden');
            successIcon.classList.remove('hidden');
            finishedActions.classList.remove('hidden');
            
            notify('Importation terminée ! 🎉', 'success');
        } else {
            statusTitle.textContent = 'Erreur lors de l\'importation';
            statusMsg.textContent = result.error;
            spinner.classList.add('hidden');
            notify('Erreur lors de l\'importation : ' + result.error, 'error');
        }
    });

    btnOpenFolder?.addEventListener('click', () => {
        window.electronAPI.openFolder();
    });

    btnCloseModal?.addEventListener('click', () => {
        modalImport.classList.add('hidden');
    });
}
