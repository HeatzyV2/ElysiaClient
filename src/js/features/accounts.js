import { state } from '../core/state.js';
import { notify } from '../core/utils.js';

// Global listener for account updates from main process
if (window.electronAPI && window.electronAPI.onAccountsUpdated) {
    window.electronAPI.onAccountsUpdated(() => {
        refreshAccountsList();
    });
}

export async function initAccounts() {
    const btnMicrosoft = document.getElementById('btn-add-microsoft');
    const btnOffline = document.getElementById('btn-add-offline');
    const inputOffline = document.getElementById('input-offline-username');
    const btnChangeSkin = document.getElementById('btn-change-skin');

    await refreshAccountsList();

    btnChangeSkin?.addEventListener('click', async () => {
        const originalText = btnChangeSkin.innerHTML;
        btnChangeSkin.innerHTML = '<div class="spinner" style="width:14px;height:14px;border-width:2px;border-top-color:#fff;"></div>';
        btnChangeSkin.disabled = true;
        
        try {
            const res = await window.electronAPI.uploadSkin();
            if (res.success) {
                notify('Skin mis à jour avec succès', 'success');
                // Force Minotar cache bypass by adding a timestamp
                const skinImg = document.getElementById('skin-preview-img');
                const avatarId = state.get('activeAccountId')?.replace(/-/g, '');
                if (skinImg && avatarId) {
                    skinImg.src = `https://minotar.net/armor/body/${avatarId}/200.png?v=${Date.now()}`;
                }
            } else if (!res.canceled) {
                notify(res.error || 'Erreur lors du changement de skin', 'error');
            }
        } finally {
            btnChangeSkin.innerHTML = originalText;
            btnChangeSkin.disabled = false;
        }
    });

    btnMicrosoft?.addEventListener('click', async () => {
        const originalText = btnMicrosoft.innerHTML;
        btnMicrosoft.innerHTML = '<div class="spinner" style="width:16px;height:16px;border-width:2px;border-top-color:#fff;"></div>';
        btnMicrosoft.disabled = true;

        try {
            const result = await window.electronAPI.authMicrosoft();
            if (result.success) {
                notify('Compte Microsoft ajoute avec succes', 'success');
                await refreshAccountsList();
            } else {
                notify(result.error || 'Connexion Microsoft impossible', 'error');
            }
        } finally {
            btnMicrosoft.innerHTML = originalText;
            btnMicrosoft.disabled = false;
        }
    });

    btnOffline?.addEventListener('click', async () => {
        const username = inputOffline?.value.trim();
        if (!username) {
            notify('Veuillez entrer un pseudo', 'warning');
            return;
        }

        const result = await window.electronAPI.authOffline(username);

        if (result.success) {
            if (inputOffline) inputOffline.value = '';
            notify('Compte hors-ligne ajoute', 'success');
            await refreshAccountsList();
        } else {
            notify(result.error || 'Impossible d ajouter le compte', 'error');
        }
    });

    inputOffline?.addEventListener('keydown', (event) => {
        if (event.key === 'Enter') {
            event.preventDefault();
            btnOffline?.click();
        }
    });
}

export async function refreshAccountsList() {
    try {
        const result = await window.electronAPI.getAccounts();
        if (!result?.success) {
            throw new Error(result?.error || 'Impossible de charger les comptes');
        }

        const accounts = Array.isArray(result.accounts) ? result.accounts : [];
        const activeId = result.activeId ?? null;

        state.set('accounts', accounts);
        state.set('activeAccountId', activeId);

        renderAccounts(accounts, activeId);
        updateHeaderAccount(accounts, activeId);
    } catch (error) {
        console.error('Failed to refresh accounts:', error);
        renderAccounts([], null);
        updateHeaderAccount([], null);
        notify('Impossible de charger les comptes', 'error');
    }
}

function renderAccounts(accounts, activeId) {
    const list = document.getElementById('accounts-list');
    if (!list) return;

    if (accounts.length === 0) {
        list.innerHTML = `
            <div class="empty-state">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="width:48px;height:48px;opacity:0.5;margin-bottom:16px;">
                    <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
                    <circle cx="12" cy="7" r="4"></circle>
                </svg>
                <p>Aucun compte connecte</p>
            </div>
        `;
        return;
    }

    list.innerHTML = '';
    accounts.forEach((acc) => {
        const isOffline = acc.type === 'offline';
        const avatarId = (acc.uuid || acc.id || acc.name || 'steve').toString().replace(/-/g, '');
        const avatarUrl = isOffline
            ? `https://minotar.net/helm/${acc.name || 'steve'}/64.png`
            : `https://minotar.net/helm/${avatarId}/64.png`;

        const card = document.createElement('div');
        card.className = `account-card ${acc.id === activeId ? 'active' : ''}`;

        card.innerHTML = `
            <img src="${avatarUrl}" class="account-avatar" alt="Avatar" onerror="this.src='data:image/svg+xml,<svg xmlns=%22http://www.w3.org/2000/svg%22 viewBox=%220 0 24 24%22 fill=%22none%22 stroke=%22%23666%22 stroke-width=%222%22><path d=%22M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2%22></path><circle cx=%2212%22 cy=%227%22 r=%224%22></circle></svg>'">
            <div class="account-info">
                <div class="account-name">${acc.name}</div>
                <div class="account-type">${isOffline ? 'Compte Local' : 'Compte Microsoft'}</div>
            </div>
            <div class="account-actions">
                ${!isOffline ? `<button class="btn-icon btn-refresh" title="Revalider la session"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M23 4v6h-6"></path><path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10"></path></svg></button>` : ''}
                ${acc.id !== activeId ? `<button class="btn-icon btn-switch" title="Utiliser"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="20 6 9 17 4 12"></polyline></svg></button>` : ''}
                <button class="btn-icon btn-delete" title="Deconnecter"><svg viewBox="0 0 24 24" fill="none" stroke="var(--danger)" stroke-width="2"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"></path><polyline points="16 17 21 12 16 7"></polyline><line x1="21" y1="12" x2="9" y2="12"></line></svg></button>
            </div>
        `;

        const btnRefresh = card.querySelector('.btn-refresh');
        if (btnRefresh) {
            btnRefresh.addEventListener('click', async (event) => {
                event.stopPropagation();
                const originalHTML = btnRefresh.innerHTML;
                btnRefresh.innerHTML = '<div class="spinner" style="width:12px;height:12px;border-width:2px;border-top-color:var(--primary);"></div>';
                btnRefresh.disabled = true;

                try {
                    const res = await window.electronAPI.authRefresh(acc.id);
                    if (res.success) {
                        notify('Session revalidée avec succès', 'success');
                        await refreshAccountsList();
                    } else {
                        notify(res.error || 'Erreur lors de la revalidation', 'error');
                    }
                } finally {
                    btnRefresh.innerHTML = originalHTML;
                    btnRefresh.disabled = false;
                }
            });
        }

        const btnSwitch = card.querySelector('.btn-switch');
        if (btnSwitch) {
            btnSwitch.addEventListener('click', async (event) => {
                event.stopPropagation();
                const res = await window.electronAPI.switchAccount(acc.id);
                if (res.success) {
                    await refreshAccountsList();
                } else {
                    notify(res.error || 'Impossible de changer de compte', 'error');
                }
            });
        }

        const btnDelete = card.querySelector('.btn-delete');
        btnDelete?.addEventListener('click', async (event) => {
            event.stopPropagation();
            if (confirm(`Voulez-vous deconnecter ${acc.name} ?`)) {
                await window.electronAPI.authLogout(acc.id);
                await refreshAccountsList();
            }
        });

        card.addEventListener('click', async () => {
            if (acc.id === activeId) return;

            const res = await window.electronAPI.switchAccount(acc.id);
            if (res.success) {
                await refreshAccountsList();
            } else {
                notify(res.error || 'Impossible de changer de compte', 'error');
            }
        });

        list.appendChild(card);
    });
}

function updateHeaderAccount(accounts, activeId) {
    const activeAcc = accounts.find((account) => account.id === activeId);
    const headerAvatar = document.getElementById('header-avatar');
    const headerUsername = document.getElementById('header-username');
    
    // Skin Preview Elements
    const skinImg = document.getElementById('skin-preview-img');
    const skinName = document.getElementById('skin-preview-name');
    const skinType = document.getElementById('skin-preview-type');
    const btnChangeSkin = document.getElementById('btn-change-skin');

    if (activeAcc) {
        const isOffline = activeAcc.type === 'offline';
        const avatarId = (activeAcc.uuid || activeAcc.id || activeAcc.name || 'steve').replace(/-/g, '');

        if (headerUsername) headerUsername.textContent = activeAcc.name;
        if (skinName) skinName.textContent = activeAcc.name;
        if (skinType) skinType.textContent = isOffline ? 'Compte Local' : 'Compte Microsoft';

        if (headerAvatar) {
            headerAvatar.src = isOffline
                ? `https://minotar.net/helm/${activeAcc.name}/32.png`
                : `https://minotar.net/helm/${avatarId}/32.png`;
        }

        if (skinImg) {
            skinImg.src = isOffline
                ? `https://minotar.net/armor/body/${activeAcc.name}/200.png`
                : `https://minotar.net/armor/body/${avatarId}/200.png`;
        }

        if (btnChangeSkin) {
            btnChangeSkin.style.display = isOffline ? 'none' : 'flex';
        }

        return;
    }

    if (headerUsername) headerUsername.textContent = 'Non connecte';
    if (skinName) skinName.textContent = 'Non connecté';
    if (skinType) skinType.textContent = 'Aucun compte actif';
    if (btnChangeSkin) btnChangeSkin.style.display = 'none';

    if (headerAvatar) {
        headerAvatar.src = 'data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="%23888" stroke-width="2"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path><circle cx="12" cy="7" r="4"></circle></svg>';
    }
    
    if (skinImg) {
        skinImg.src = 'data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="%23444" stroke-width="2"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path><circle cx="12" cy="7" r="4"></circle></svg>';
    }
}
