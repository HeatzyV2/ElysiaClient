const { ipcMain } = require('electron');
const { Microsoft } = require('minecraft-java-core');
const { store } = require('./store');

function setupAuthIpc(mainWindow) {
    ipcMain.handle('auth:microsoft', async () => {
        try {
            const msAuth = new Microsoft();
            const account = await msAuth.getAuth();
            
            if (account && !account.error) {
                account.type = 'microsoft';
                account.id = account.uuid || Date.now().toString();
                
                const accounts = store.get('accounts') || [];
                // Check if already exists
                const existingIndex = accounts.findIndex(a => a.uuid === account.uuid);
                if (existingIndex >= 0) {
                    accounts[existingIndex] = account;
                } else {
                    accounts.push(account);
                }
                
                store.set('accounts', accounts);
                store.set('activeAccountId', account.id);
                
                return { success: true, account };
            }
            return { success: false, error: account?.error || 'Authentication failed' };
        } catch (error) {
            console.error('Microsoft auth error:', error);
            return { success: false, error: error.message };
        }
    });

    ipcMain.handle('auth:offline', async (_, username) => {
        if (!username || username.trim().length < 3 || username.trim().length > 16) {
            return { success: false, error: 'Le pseudo doit contenir entre 3 et 16 caractères' };
        }
        if (!/^[a-zA-Z0-9_]+$/.test(username.trim())) {
            return { success: false, error: 'Le pseudo ne peut contenir que des lettres, chiffres et underscores' };
        }
        
        const account = {
            access_token: '0',
            client_token: '0',
            uuid: '00000000-0000-0000-0000-000000000000',
            id: 'offline-' + Date.now(),
            name: username.trim(),
            user_properties: '{}',
            meta: { type: 'offline', offline: true },
            type: 'offline'
        };
        
        const accounts = store.get('accounts') || [];
        accounts.push(account);
        store.set('accounts', accounts);
        store.set('activeAccountId', account.id);
        
        return { success: true, account };
    });

    ipcMain.handle('auth:refresh', async (_, accountId) => {
        const accounts = store.get('accounts') || [];
        const idToRefresh = accountId || store.get('activeAccountId');
        const account = accounts.find(a => a.id === idToRefresh);
        
        if (!account) return { success: false, error: 'No account found' };

        if (account.type === 'offline') {
            return { success: true, account };
        }

        try {
            if (account.refresh_token) {
                const msAuth = new Microsoft();
                const refreshed = await msAuth.refresh(account);
                if (refreshed && !refreshed.error) {
                    refreshed.type = 'microsoft';
                    refreshed.id = account.id; // preserve ID
                    
                    const index = accounts.findIndex(a => a.id === account.id);
                    if (index >= 0) accounts[index] = refreshed;
                    store.set('accounts', accounts);
                    
                    return { success: true, account: refreshed };
                }
            }
            return { success: false, error: 'Token expired, please re-authenticate' };
        } catch (error) {
            return { success: false, error: error.message };
        }
    });

    ipcMain.handle('auth:logout', async (_, accountId) => {
        let accounts = store.get('accounts') || [];
        const idToRemove = accountId || store.get('activeAccountId');
        
        accounts = accounts.filter(a => a.id !== idToRemove);
        store.set('accounts', accounts);
        
        if (store.get('activeAccountId') === idToRemove) {
            store.set('activeAccountId', accounts.length > 0 ? accounts[0].id : null);
        }
        
        return { success: true };
    });

    ipcMain.handle('auth:getAccounts', async () => {
        return { 
            success: true, 
            accounts: store.get('accounts') || [],
            activeId: store.get('activeAccountId')
        };
    });
    
    ipcMain.handle('auth:switchAccount', async (_, accountId) => {
        const accounts = store.get('accounts') || [];
        const targetAccount = accounts.find(a => a.id === accountId);
        if (targetAccount) {
            store.set('activeAccountId', accountId);
            
            // Update Discord RPC if possible
            try {
                const { updatePresenceFromConfig } = require('./discord');
                updatePresenceFromConfig({ username: targetAccount.name });
            } catch (e) {}
            
            return { success: true };
        }
        return { success: false, error: 'Account not found' };
    });
}

module.exports = { setupAuthIpc };
