const { ipcMain, shell } = require('electron');
const { exec } = require('child_process');
const path = require('path');

function setupBedrockIpc(mainWindow) {
    // Check if Minecraft Bedrock is installed
    ipcMain.handle('bedrock:checkInstalled', async () => {
        return new Promise((resolve) => {
            exec('powershell.exe -NoProfile -Command "Get-AppxPackage -Name Microsoft.MinecraftUWP | Select-Object -ExpandProperty InstallLocation"', 
                (error, stdout, stderr) => {
                    if (error || !stdout.trim()) {
                        exec('powershell.exe -NoProfile -Command "Get-AppxPackage -Name Microsoft.MinecraftWindowsBeta | Select-Object -ExpandProperty InstallLocation"',
                            (error2, stdout2) => {
                                if (error2 || !stdout2.trim()) {
                                    resolve({ installed: false });
                                } else {
                                    resolve({ installed: true, path: stdout2.trim(), edition: 'preview' });
                                }
                            }
                        );
                    } else {
                        resolve({ installed: true, path: stdout.trim(), edition: 'release' });
                    }
                }
            );
        });
    });

    // Get Bedrock version info
    ipcMain.handle('bedrock:getInfo', async () => {
        return new Promise((resolve) => {
            exec('powershell.exe -NoProfile -Command "Get-AppxPackage -Name Microsoft.MinecraftUWP | Select-Object Name, Version, InstallLocation | ConvertTo-Json"',
                (error, stdout) => {
                    if (error || !stdout.trim()) {
                        resolve({ success: false });
                    } else {
                        try {
                            const info = JSON.parse(stdout.trim());
                            resolve({ 
                                success: true, 
                                name: info.Name,
                                version: info.Version,
                                path: info.InstallLocation
                            });
                        } catch (e) {
                            resolve({ success: false });
                        }
                    }
                }
            );
        });
    });

    // Launch Minecraft Bedrock
    ipcMain.handle('bedrock:launch', async (_, options = {}) => {
        try {
            let uri = 'minecraft:';

            if (options.serverIP) {
                const port = options.serverPort || 19132;
                const serverName = options.serverName || 'Elysia Server';
                uri = `minecraft:?addExternalServer=${encodeURIComponent(serverName)}|${options.serverIP}:${port}`;
            }

            mainWindow?.webContents.send('launch:data', `[Bedrock] Lancement: ${uri}`);

            await shell.openExternal(uri);

            mainWindow?.webContents.send('launch:data', `[Bedrock] Minecraft Bedrock lancé avec succès !`);

            ipcMain.emit('game:status', null, {
                status: 'playing',
                version: 'Bedrock Edition',
                username: 'Bedrock'
            });

            return { success: true };
        } catch (error) {
            mainWindow?.webContents.send('launch:data', `[Bedrock] Erreur: ${error.message}`);
            return { success: false, error: error.message };
        }
    });
}

module.exports = { setupBedrockIpc };
