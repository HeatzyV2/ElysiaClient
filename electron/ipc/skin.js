const { ipcMain, dialog } = require('electron');
const { store } = require('./store');
const fs = require('fs');
const path = require('path');

function setupSkinIpc(mainWindow) {
    ipcMain.handle('skin:upload', async () => {
        try {
            const { canceled, filePaths } = await dialog.showOpenDialog(mainWindow, {
                title: 'Choisir un Skin Minecraft',
                filters: [{ name: 'Images', extensions: ['png'] }],
                properties: ['openFile']
            });

            if (canceled || filePaths.length === 0) return { success: false, canceled: true };

            const filePath = filePaths[0];
            const activeId = store.get('activeAccountId');
            const accounts = store.get('accounts') || [];
            const account = accounts.find(a => a.id === activeId);

            if (!account || account.type !== 'microsoft' || !account.access_token) {
                return { success: false, error: "Vous devez être connecté avec un compte Microsoft Premium pour changer votre skin." };
            }

            // Prepare multipart/form-data payload
            const formData = new FormData();
            formData.append('variant', 'classic'); // default
            
            const fileData = fs.readFileSync(filePath);
            const blob = new Blob([fileData], { type: 'image/png' });
            formData.append('file', blob, path.basename(filePath));

            const res = await fetch('https://api.minecraftservices.com/minecraft/profile/skins', {
                method: 'POST',
                headers: {
                    'Authorization': `Bearer ${account.access_token}`
                },
                body: formData
            });

            if (res.ok) {
                return { success: true };
            } else {
                const text = await res.text();
                return { success: false, error: `Erreur API: ${res.status} ${text}` };
            }
        } catch (error) {
            console.error('Skin upload error:', error);
            return { success: false, error: error.message };
        }
    });
}

module.exports = { setupSkinIpc };
