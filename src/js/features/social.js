import { notify } from '../core/utils.js';
import { FirebaseClient } from '../core/firebase.js';

let myAccount = null;
let friendsListener = null;

export async function initSocial() {
    // UI Elements
    const btnAddFriend = document.getElementById('btn-add-friend');
    const inputFriendUsername = document.getElementById('input-friend-username');
    const tabFriendsList = document.getElementById('tab-friends-list');
    const tabFriendsRequests = document.getElementById('tab-friends-requests');
    const listFriends = document.getElementById('friends-list');
    const listRequests = document.getElementById('requests-list');

    // Setup Tabs
    if (tabFriendsList && tabFriendsRequests) {
        tabFriendsList.addEventListener('click', () => {
            tabFriendsList.classList.add('active');
            tabFriendsRequests.classList.remove('active');
            listFriends.style.display = 'grid';
            listRequests.style.display = 'none';
        });

        tabFriendsRequests.addEventListener('click', () => {
            tabFriendsRequests.classList.add('active');
            tabFriendsList.classList.remove('active');
            listRequests.style.display = 'grid';
            listFriends.style.display = 'none';
        });
    }

    // Identify user
    await identifyUser();
    
    if (!myAccount) {
        showErrorState("Vous devez être connecté avec un compte Microsoft pour accéder aux amis.");
        if (btnAddFriend) btnAddFriend.disabled = true;
        return;
    }

    // Set Online Presence
    setupPresence();

    // Listen to friends real-time
    listenToFriends();

    // Add friend action
    if (btnAddFriend) {
        btnAddFriend.addEventListener('click', async () => {
            const username = inputFriendUsername.value.trim();
            if (!username) {
                showStatus('Veuillez entrer un pseudo.', 'var(--danger)');
                return;
            }
            if (username.toLowerCase() === myAccount.name.toLowerCase()) {
                showStatus('Vous ne pouvez pas vous ajouter vous-même.', 'var(--danger)');
                return;
            }

            btnAddFriend.disabled = true;
            showStatus('Recherche...', 'var(--text-muted)');

            try {
                // Fetch from Mojang API
                const response = await fetch(`https://api.mojang.com/users/profiles/minecraft/${username}`);
                if (!response.ok) {
                    if (response.status === 404 || response.status === 204) throw new Error('Joueur introuvable.');
                    if (response.status === 429) throw new Error('Trop de requêtes, réessayez plus tard.');
                    throw new Error('Erreur réseau.');
                }
                
                const data = await response.json();
                const targetUuid = data.id; // Mojang 'id' is UUID
                const targetUsername = data.name;

                // Send Friend Request via Firebase
                await FirebaseClient.patch(`/friends/${targetUuid}`, {
                    [myAccount.id]: { status: 'pending', username: myAccount.name, timestamp: Date.now() }
                });
                
                await FirebaseClient.patch(`/friends/${myAccount.id}`, {
                    [targetUuid]: { status: 'requested', username: targetUsername, timestamp: Date.now() }
                });
                
                showStatus(`Demande envoyée à ${targetUsername} !`, '#10b981');
                inputFriendUsername.value = '';
                
            } catch (error) {
                console.error(error);
                showStatus(error.message || 'Joueur introuvable ou erreur réseau.', 'var(--danger)');
            } finally {
                btnAddFriend.disabled = false;
            }
        });
    }
}

async function identifyUser() {
    try {
        const activeAccountId = await window.electronAPI.storeGet('activeAccountId');
        if (!activeAccountId) return;
        
        const accounts = await window.electronAPI.storeGet('accounts') || [];
        const account = accounts.find(a => a.id === activeAccountId);
        
        if (account && account.type === 'microsoft') {
            myAccount = {
                id: account.id.replace(/-/g, ''), // UUID without dashes to match Mojang standard
                name: account.name
            };
        }
    } catch (e) {
        console.error("Failed to identify user", e);
    }
}

function setupPresence() {
    if (!myAccount) return;
    
    const updatePresence = async (status) => {
        try {
            await FirebaseClient.patch(`/users/${myAccount.id}`, {
                username: myAccount.name,
                status: status,
                lastSeen: Date.now()
            });
        } catch (e) {
            console.error('Failed to update presence', e);
        }
    };

    // Set online
    updatePresence('online');

    // Refresh every minute to show we are still alive
    setInterval(() => updatePresence('online'), 60000);

    // Set offline when leaving
    window.addEventListener('beforeunload', () => {
        // Can't await in beforeunload, but we try a fire-and-forget fetch
        fetch(`https://elysiapanel-195c1-default-rtdb.firebaseio.com/users/${myAccount.id}.json`, {
            method: 'PATCH',
            keepalive: true,
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ status: 'offline', lastSeen: Date.now() })
        });
    });
}

function listenToFriends() {
    if (!myAccount) return;

    friendsListener = FirebaseClient.listen(`/friends/${myAccount.id}`, async (eventData) => {
        // EventData contains path and data. Since we listen to the root of friends/uuid,
        // it gives us the whole object on first load (put path="/"), or updates for children (patch/put)
        
        // Let's just fetch the whole list to simplify rendering when an event fires
        try {
            const data = await FirebaseClient.get(`/friends/${myAccount.id}`);
            renderFriendsUI(data || {});
        } catch (e) {
            console.error("Failed to fetch friends", e);
        }
    });
}

async function renderFriendsUI(friendsMap) {
    const listFriends = document.getElementById('friends-list');
    const listRequests = document.getElementById('requests-list');
    const requestsBadge = document.getElementById('requests-badge');
    
    if (!listFriends || !listRequests) return;

    const friends = [];
    const incomingRequests = [];
    const outgoingRequests = [];

    // Separate based on status
    for (const [uuid, data] of Object.entries(friendsMap)) {
        if (data.status === 'accepted') {
            friends.push({ uuid, ...data });
        } else if (data.status === 'pending') {
            incomingRequests.push({ uuid, ...data });
        } else if (data.status === 'requested') {
            outgoingRequests.push({ uuid, ...data });
        }
    }

    // Update Badge
    if (incomingRequests.length > 0) {
        requestsBadge.textContent = incomingRequests.length;
        requestsBadge.style.display = 'inline-block';
    } else {
        requestsBadge.style.display = 'none';
    }

    // Fetch realtime statuses for accepted friends
    for (let friend of friends) {
        try {
            const userData = await FirebaseClient.get(`/users/${friend.uuid}`);
            if (userData) {
                friend.isOnline = userData.status === 'online';
                // If they haven't updated in 2 minutes, consider them offline
                if (Date.now() - userData.lastSeen > 120000) friend.isOnline = false;
            }
        } catch (e) {
            friend.isOnline = false;
        }
    }

    renderAcceptedFriends(listFriends, friends);
    renderRequests(listRequests, incomingRequests, outgoingRequests);
}

function renderAcceptedFriends(container, friends) {
    if (friends.length === 0) {
        container.innerHTML = `
            <div class="empty-state">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" style="width: 48px; height: 48px; opacity: 0.5; margin-bottom: 16px;"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path><circle cx="9" cy="7" r="4"></circle><path d="M23 21v-2a4 4 0 0 0-3-3.87"></path><path d="M16 3.13a4 4 0 0 1 0 7.75"></path></svg>
                <h3>Aucun ami pour le moment</h3>
                <p>Utilisez le panneau latéral pour trouver vos amis via leur pseudo Minecraft.</p>
            </div>
        `;
        return;
    }

    container.innerHTML = '';
    friends.sort((a, b) => b.isOnline - a.isOnline || a.username.localeCompare(b.username)); // Online first

    friends.forEach(friend => {
        const card = document.createElement('div');
        card.className = 'friend-card';
        const avatarUrl = `https://mc-heads.net/head/${friend.uuid}/64`;
        const statusText = friend.isOnline ? 'En ligne' : 'Hors ligne';
        const statusClass = friend.isOnline ? 'online' : '';

        card.innerHTML = `
            <img src="${avatarUrl}" alt="${friend.username}" class="friend-avatar" loading="lazy">
            <div class="friend-info">
                <div class="friend-name">${friend.username}</div>
                <div class="friend-status">
                    <div class="status-dot ${statusClass}"></div>
                    ${statusText}
                </div>
            </div>
            <div class="friend-actions">
                <button class="btn-icon danger" title="Retirer des amis" onclick="window.socialAction('remove', '${friend.uuid}')">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="width:16px;height:16px;"><path d="M18 6L6 18M6 6l12 12"></path></svg>
                </button>
            </div>
        `;
        container.appendChild(card);
    });
}

function renderRequests(container, incoming, outgoing) {
    if (incoming.length === 0 && outgoing.length === 0) {
        container.innerHTML = `
            <div class="empty-state">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" style="width: 48px; height: 48px; opacity: 0.5; margin-bottom: 16px;"><path d="M22 11.08V12a10.00 10.00 0 1 1-5.93-9.14"></path><polyline points="22 4 12 14.01 9 11.01"></polyline></svg>
                <h3>Aucune demande d'ami</h3>
            </div>
        `;
        return;
    }

    container.innerHTML = '';

    const renderList = (title, list, type) => {
        if (list.length === 0) return;
        const group = document.createElement('div');
        group.style.gridColumn = '1 / -1';
        group.innerHTML = `<h3 style="color:#fff; font-size:14px; margin-bottom:12px; margin-top: 8px;">${title} (${list.length})</h3>`;
        container.appendChild(group);

        list.forEach(req => {
            const card = document.createElement('div');
            card.className = 'friend-card';
            card.style.background = 'rgba(15,15,45,0.8)';
            const avatarUrl = `https://mc-heads.net/head/${req.uuid}/64`;

            let actionsHtml = '';
            if (type === 'incoming') {
                actionsHtml = `
                    <button class="btn-icon" style="background:#10b981;" title="Accepter" onclick="window.socialAction('accept', '${req.uuid}')">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="width:16px;height:16px;"><polyline points="20 6 9 17 4 12"></polyline></svg>
                    </button>
                    <button class="btn-icon danger" title="Refuser" onclick="window.socialAction('remove', '${req.uuid}')">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="width:16px;height:16px;"><line x1="18" y1="6" x2="6" y2="18"></line><line x1="6" y1="6" x2="18" y2="18"></line></svg>
                    </button>
                `;
            } else {
                actionsHtml = `
                    <span style="font-size:12px; color:var(--text-muted); margin-right:8px;">En attente</span>
                    <button class="btn-icon danger" title="Annuler" onclick="window.socialAction('remove', '${req.uuid}')">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="width:16px;height:16px;"><line x1="18" y1="6" x2="6" y2="18"></line><line x1="6" y1="6" x2="18" y2="18"></line></svg>
                    </button>
                `;
            }

            card.innerHTML = `
                <img src="${avatarUrl}" alt="${req.username}" class="friend-avatar" loading="lazy">
                <div class="friend-info">
                    <div class="friend-name">${req.username}</div>
                </div>
                <div class="friend-actions" style="opacity:1; position:relative; right:0; background:transparent;">
                    ${actionsHtml}
                </div>
            `;
            container.appendChild(card);
        });
    };

    renderList('Demandes reçues', incoming, 'incoming');
    renderList('Demandes envoyées', outgoing, 'outgoing');
}

window.socialAction = async (action, targetUuid) => {
    if (!myAccount) return;

    try {
        if (action === 'accept') {
            await FirebaseClient.patch(`/friends/${myAccount.id}/${targetUuid}`, { status: 'accepted' });
            await FirebaseClient.patch(`/friends/${targetUuid}/${myAccount.id}`, { status: 'accepted' });
            notify('Demande acceptée !', 'success');
        } else if (action === 'remove') {
            await FirebaseClient.delete(`/friends/${myAccount.id}/${targetUuid}`);
            await FirebaseClient.delete(`/friends/${targetUuid}/${myAccount.id}`);
            notify('Ami / Demande supprimé(e).', 'success');
        }
        
        // Force refresh
        const data = await FirebaseClient.get(`/friends/${myAccount.id}`);
        renderFriendsUI(data || {});
    } catch (e) {
        console.error("Action failed", e);
        notify('Erreur lors de l\'action.', 'error');
    }
};

function showStatus(message, color) {
    const statusText = document.getElementById('add-friend-status');
    if (statusText) {
        statusText.textContent = message;
        statusText.style.color = color;
    }
}

function showErrorState(message) {
    const main = document.querySelector('.social-main');
    if (main) {
        main.innerHTML = `
            <div class="empty-state" style="margin-top: 40px;">
                <svg viewBox="0 0 24 24" fill="none" stroke="var(--danger)" stroke-width="1.5" style="width: 48px; height: 48px; margin-bottom: 16px;"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"></path><line x1="12" y1="9" x2="12" y2="13"></line><line x1="12" y1="17" x2="12.01" y2="17"></line></svg>
                <h3>Connexion Requise</h3>
                <p style="color: var(--danger);">${message}</p>
            </div>
        `;
    }
}
