import { notify } from '../core/utils.js';

let bedrockInfo = null;
let isInstalled = false;

export async function initBedrock() {
    const page = document.getElementById('page-bedrock');
    if (!page) return;

    const statusCard = document.getElementById('bedrock-status-card');
    const launchBtn = document.getElementById('btn-bedrock-launch');
    const serverIPInput = document.getElementById('bedrock-server-ip');
    const serverPortInput = document.getElementById('bedrock-server-port');
    const serverNameInput = document.getElementById('bedrock-server-name');
    const versionLabel = document.getElementById('bedrock-version');
    const statusDot = document.getElementById('bedrock-status-dot');
    const statusText = document.getElementById('bedrock-status-text');

    // Check installation
    try {
        const result = await window.electronAPI.bedrockCheckInstalled();
        isInstalled = result.installed;

        if (isInstalled) {
            statusDot.classList.add('online');
            statusText.textContent = 'Installé';
            launchBtn.disabled = false;

            // Get detailed info
            const info = await window.electronAPI.bedrockGetInfo();
            if (info.success) {
                bedrockInfo = info;
                if (versionLabel) versionLabel.textContent = info.version || 'Inconnue';
            }
        } else {
            statusDot.classList.remove('online');
            statusText.textContent = 'Non installé';
            launchBtn.disabled = true;
            if (versionLabel) versionLabel.textContent = 'N/A';
            showNotInstalledState();
        }
    } catch (e) {
        console.error('Bedrock check failed:', e);
        statusText.textContent = 'Erreur de détection';
        launchBtn.disabled = true;
    }

    // Launch button
    launchBtn?.addEventListener('click', async () => {
        if (!isInstalled) {
            notify('Minecraft Bedrock n\'est pas installé.', 'error');
            return;
        }

        const originalHTML = launchBtn.innerHTML;
        launchBtn.disabled = true;
        launchBtn.innerHTML = `
            <div class="spinner" style="width:18px;height:18px;border-width:2px;"></div>
            <span>Lancement...</span>
        `;

        const options = {};
        const serverIP = serverIPInput?.value.trim();
        if (serverIP) {
            options.serverIP = serverIP;
            options.serverPort = parseInt(serverPortInput?.value) || 19132;
            options.serverName = serverNameInput?.value.trim() || 'Elysia Server';
        }

        try {
            const result = await window.electronAPI.bedrockLaunch(options);
            if (result.success) {
                notify('Minecraft Bedrock lancé avec succès !', 'success');
                
                // Switch button to "playing" state
                launchBtn.innerHTML = `
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="width:20px;height:20px;">
                        <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path>
                        <polyline points="22 4 12 14.01 9 11.01"></polyline>
                    </svg>
                    <span>En cours...</span>
                `;
                launchBtn.style.background = 'linear-gradient(135deg, #059669, #10b981)';
                
                setTimeout(() => {
                    launchBtn.innerHTML = originalHTML;
                    launchBtn.disabled = false;
                    launchBtn.style.background = '';
                }, 10000);
            } else {
                notify('Erreur: ' + (result.error || 'Impossible de lancer Bedrock.'), 'error');
                launchBtn.innerHTML = originalHTML;
                launchBtn.disabled = false;
            }
        } catch (e) {
            notify('Erreur lors du lancement.', 'error');
            launchBtn.innerHTML = originalHTML;
            launchBtn.disabled = false;
        }
    });

    // Microsoft Store link
    const btnInstall = document.getElementById('btn-bedrock-install');
    btnInstall?.addEventListener('click', () => {
        window.open('ms-windows-store://pdp/?productid=9NBLGGH2JHXJ', '_blank');
    });
}

function showNotInstalledState() {
    const container = document.getElementById('bedrock-main-content');
    if (!container) return;
    
    container.innerHTML = `
        <div class="empty-state" style="margin-top: 40px;">
            <svg viewBox="0 0 24 24" fill="none" stroke="var(--warning)" stroke-width="1.5" style="width: 56px; height: 56px; margin-bottom: 16px;">
                <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"></path>
                <line x1="12" y1="9" x2="12" y2="13"></line>
                <line x1="12" y1="17" x2="12.01" y2="17"></line>
            </svg>
            <h3 style="color: var(--warning);">Minecraft Bedrock non détecté</h3>
            <p style="color: var(--text-secondary); margin-top: 8px; max-width: 400px;">
                Minecraft pour Windows (Bedrock Edition) n'est pas installé sur cet ordinateur.
                Vous pouvez l'obtenir via le Microsoft Store.
            </p>
            <button id="btn-bedrock-install" class="btn btn-primary" style="margin-top: 20px; gap: 8px;">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="width:16px;height:16px;">
                    <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
                    <polyline points="7 10 12 15 17 10"></polyline>
                    <line x1="12" y1="15" x2="12" y2="3"></line>
                </svg>
                Ouvrir le Microsoft Store
            </button>
        </div>
    `;
    
    document.getElementById('btn-bedrock-install')?.addEventListener('click', () => {
        window.open('ms-windows-store://pdp/?productid=9NBLGGH2JHXJ', '_blank');
    });
}
