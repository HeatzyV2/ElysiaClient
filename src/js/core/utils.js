// utils.js — Premium Toast Notifications with UI Sounds

const UI_SOUNDS = {
    success: [523.25, 659.25, 783.99], // C5 E5 G5 chord
    error: [311.13, 293.66],           // descending
    warning: [440, 440],               // A4 beep
    info: [587.33]                     // D5 ping
};

function playSound(type) {
    try {
        const ctx = new (window.AudioContext || window.webkitAudioContext)();
        const freqs = UI_SOUNDS[type] || UI_SOUNDS.info;
        
        freqs.forEach((freq, i) => {
            const osc = ctx.createOscillator();
            const gain = ctx.createGain();
            osc.type = 'sine';
            osc.frequency.value = freq;
            gain.gain.setValueAtTime(0.08, ctx.currentTime + i * 0.08);
            gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + i * 0.08 + 0.3);
            osc.connect(gain);
            gain.connect(ctx.destination);
            osc.start(ctx.currentTime + i * 0.08);
            osc.stop(ctx.currentTime + i * 0.08 + 0.3);
        });
        
        setTimeout(() => ctx.close(), 1000);
    } catch (e) {
        // Audio not available, silent fallback
    }
}

let toastCount = 0;
const MAX_TOASTS = 5;

export function notify(message, type = 'info') {
    const container = document.getElementById('notification-container');
    if (!container) return;

    // Limit max toasts — remove oldest if too many
    while (container.children.length >= MAX_TOASTS) {
        container.firstChild?.remove();
    }

    toastCount++;
    const id = `toast-${toastCount}`;

    const el = document.createElement('div');
    el.className = `notification notification-${type}`;
    el.id = id;
    
    let icon = '';
    if (type === 'success') icon = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="20 6 9 17 4 12"></polyline></svg>';
    else if (type === 'error') icon = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"></circle><line x1="15" y1="9" x2="9" y2="15"></line><line x1="9" y1="9" x2="15" y2="15"></line></svg>';
    else if (type === 'warning') icon = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"></path><line x1="12" y1="9" x2="12" y2="13"></line><line x1="12" y1="17" x2="12.01" y2="17"></line></svg>';
    else icon = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"></circle><line x1="12" y1="16" x2="12" y2="12"></line><line x1="12" y1="8" x2="12.01" y2="8"></line></svg>';

    el.innerHTML = `
        <div class="toast-icon">${icon}</div>
        <span class="toast-message">${message}</span>
        <button class="toast-close" aria-label="Fermer">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="width:12px;height:12px;">
                <line x1="18" y1="6" x2="6" y2="18"></line>
                <line x1="6" y1="6" x2="18" y2="18"></line>
            </svg>
        </button>
        <div class="toast-progress"><div class="toast-progress-bar"></div></div>
    `;

    container.appendChild(el);

    // Play sound
    playSound(type);

    // Slide in
    requestAnimationFrame(() => {
        el.classList.add('toast-visible');
    });

    // Close button
    el.querySelector('.toast-close').addEventListener('click', () => dismissToast(el));

    // Auto dismiss after 4s
    const timer = setTimeout(() => dismissToast(el), 4000);
    el._timer = timer;
}

function dismissToast(el) {
    if (!el || !el.parentNode) return;
    clearTimeout(el._timer);
    el.classList.remove('toast-visible');
    el.classList.add('toast-exit');
    setTimeout(() => el.remove(), 300);
}

export function showProgress(show) {
    const bar = document.getElementById('progress-container');
    if (!bar) return;

    bar.classList.toggle('hidden', !show);

    if (!show) {
        document.getElementById('progress-fill')?.style.setProperty('width', '0%');

        const progressLabel = document.getElementById('progress-label');
        const progressPercent = document.getElementById('progress-percent');
        const progressSpeed = document.getElementById('progress-speed');
        const progressEta = document.getElementById('progress-eta');

        if (progressLabel) progressLabel.textContent = 'Telechargement...';
        if (progressPercent) progressPercent.textContent = '0%';
        if (progressSpeed) progressSpeed.textContent = '';
        if (progressEta) progressEta.textContent = '';
    }
}
