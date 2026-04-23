const { ipcMain } = require('electron');
const { store } = require('./store');

function setupSocialIpc() {
    ipcMain.handle('social:getFriends', () => {
        return store.get('friends') || [];
    });

    ipcMain.handle('social:addFriend', (event, friend) => {
        const friends = store.get('friends') || [];
        
        // Check if friend already exists
        if (friends.some(f => f.uuid === friend.uuid)) {
            return { success: false, error: 'Cet ami est déjà dans votre liste.' };
        }

        friends.push(friend);
        store.set('friends', friends);
        return { success: true, friends };
    });

    ipcMain.handle('social:removeFriend', (event, uuid) => {
        let friends = store.get('friends') || [];
        friends = friends.filter(f => f.uuid !== uuid);
        store.set('friends', friends);
        return { success: true, friends };
    });
}

module.exports = { setupSocialIpc };
