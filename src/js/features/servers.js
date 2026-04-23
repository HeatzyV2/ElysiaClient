import { state } from '../core/state.js';
import { notify } from '../core/utils.js';

export async function initServers() {
    const btnAdd = document.getElementById('btn-add-server');
    const modal = document.getElementById('modal-add-server');
    const btnCancel = document.getElementById('btn-cancel-server');
    const btnSave = document.getElementById('btn-save-server');
    const inputName = document.getElementById('input-server-name');
    const inputIp = document.getElementById('input-server-ip');
    const list = document.getElementById('servers-list');

    if (!btnAdd || !list) return;

    btnAdd.addEventListener('click', () => {
        inputName.value = '';
        inputIp.value = '';
        modal.classList.remove('hidden');
    });

    btnCancel.addEventListener('click', () => {
        modal.classList.add('hidden');
    });

    btnSave.addEventListener('click', async () => {
        const name = inputName.value.trim();
        const ip = inputIp.value.trim();

        if (!name || !ip) {
            notify('Veuillez remplir tous les champs', 'warning');
            return;
        }

        const res = await window.electronAPI.addServer({ name, ip });
        if (res.success) {
            notify('Serveur ajouté', 'success');
            modal.classList.add('hidden');
            loadServers();
        } else {
            notify('Erreur lors de l\'ajout', 'error');
        }
    });

    await loadServers();
}

async function loadServers() {
    const list = document.getElementById('servers-list');
    if (!list) return;

    const res = await window.electronAPI.getServers();
    if (!res.success || !res.servers || res.servers.length === 0) {
        list.innerHTML = `
            <div class="empty-state">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" style="width: 48px; height: 48px; opacity: 0.5; margin-bottom: 16px;"><path d="M5 12h14M12 5l7 7-7 7"/><rect x="2" y="4" width="20" height="16" rx="2" ry="2"/></svg>
                <h3>Aucun serveur favori</h3>
                <p>Ajoutez un serveur pour voir son statut et vous y connecter rapidement.</p>
            </div>`;
        return;
    }

    list.innerHTML = '';
    
    for (const s of res.servers) {
        const card = document.createElement('div');
        card.className = 'server-card';
        card.innerHTML = `
            <div class="server-favicon">
                <img src="data:image/svg+xml,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%23555' stroke-width='2'><rect x='2' y='4' width='20' height='16' rx='2'/></svg>" alt="Icon">
            </div>
            
            <div class="server-main">
                <div class="server-header">
                    <h3>${s.name}</h3>
                    <span class="server-ip">${s.ip}</span>
                </div>
                <div class="server-motd">—</div>
                <div class="server-status ping-loading">
                    <span class="status-dot"></span>
                    <span class="status-text">Recherche...</span>
                </div>
            </div>

            <div class="server-side">
                <div class="server-meta">
                    <span class="server-players">—</span>
                    <span class="server-version">—</span>
                </div>
                <div class="server-actions">
                    <button class="btn-icon btn-join" title="Lancer et rejoindre">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polygon points="5 3 19 12 5 21 5 3"/></svg>
                    </button>
                    <button class="btn-icon btn-remove" title="Supprimer">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M3 6h18m-2 0v14c0 1-1 2-2 2H7c-1 0-2-1-2-2V6m3 0V4c0-1 1-2 2-2h4c1 0 2 1 2 2v2"/></svg>
                    </button>
                </div>
            </div>
        `;

        card.querySelector('.btn-remove').addEventListener('click', async () => {
            if (confirm('Voulez-vous supprimer ce serveur ?')) {
                await window.electronAPI.removeServer(s.id);
                loadServers();
            }
        });

        card.querySelector('.btn-join').addEventListener('click', () => {
            window.electronAPI.getSettings().then(settings => {
                settings.quickJoin = s.ip;
                window.electronAPI.saveSettings(settings).then(() => {
                    notify(`Lancement avec connexion auto à ${s.name}...`, 'info');
                    document.getElementById('btn-play')?.click();
                });
            });
        });

        list.appendChild(card);

        // Ping the server with full SLP
        window.electronAPI.pingServer(s.ip).then(pingRes => {
            const statusDiv = card.querySelector('.server-status');
            const motdDiv = card.querySelector('.server-motd');
            const playersSpan = card.querySelector('.server-players');
            const versionSpan = card.querySelector('.server-version');
            const faviconImg = card.querySelector('.server-favicon img');

            if (pingRes.online) {
                statusDiv.className = 'server-status ping-online';
                statusDiv.querySelector('.status-text').textContent = `${pingRes.ping} ms`;
                if (pingRes.motd) motdDiv.textContent = pingRes.motd.replace(/§[0-9a-fk-or]/gi, '');
                if (pingRes.players) playersSpan.textContent = `${pingRes.players.online}/${pingRes.players.max} joueurs`;
                if (pingRes.version) versionSpan.textContent = pingRes.version;
                if (pingRes.favicon && faviconImg) faviconImg.src = pingRes.favicon;
            } else {
                statusDiv.className = 'server-status ping-offline';
                statusDiv.querySelector('.status-text').textContent = `Hors ligne`;
                motdDiv.textContent = 'Serveur inaccessible';
                playersSpan.textContent = '—';
            }
        });
    }
}
