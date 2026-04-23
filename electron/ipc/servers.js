const { ipcMain } = require('electron');
const { store } = require('./store');
const net = require('net');

// Minecraft VarInt helpers
function writeVarInt(value) {
    const buf = [];
    while (true) {
        if ((value & ~0x7F) === 0) {
            buf.push(value);
            return Buffer.from(buf);
        }
        buf.push((value & 0x7F) | 0x80);
        value >>>= 7;
    }
}

function readVarInt(buffer, offset = 0) {
    let value = 0;
    let size = 0;
    let byte;
    do {
        byte = buffer[offset + size];
        value |= (byte & 0x7F) << (7 * size);
        size++;
        if (size > 5) throw new Error('VarInt too big');
    } while ((byte & 0x80) !== 0);
    return { value, size };
}

function setupServersIpc() {
    // Get saved servers
    ipcMain.handle('servers:list', async () => {
        return { success: true, servers: store.get('servers') || [] };
    });

    // Add server
    ipcMain.handle('servers:add', async (_, server) => {
        const servers = store.get('servers') || [];
        server.id = Date.now().toString();
        server.addedAt = new Date().toISOString();
        servers.push(server);
        store.set('servers', servers);
        return { success: true, server };
    });

    // Remove server
    ipcMain.handle('servers:remove', async (_, serverId) => {
        let servers = store.get('servers') || [];
        servers = servers.filter(s => s.id !== serverId);
        store.set('servers', servers);
        return { success: true };
    });

    // Ping server — Full Minecraft SLP Protocol
    ipcMain.handle('servers:ping', async (_, address) => {
        try {
            const [host, portStr] = address.split(':');
            const port = parseInt(portStr) || 25565;

            return new Promise((resolve) => {
                const socket = new net.Socket();
                socket.setTimeout(5000);
                const startTime = Date.now();
                let responseBuffer = Buffer.alloc(0);

                socket.connect(port, host, () => {
                    // Build Handshake packet (id=0x00)
                    const hostBuf = Buffer.from(host, 'utf8');
                    const portBuf = Buffer.alloc(2);
                    portBuf.writeUInt16BE(port);

                    const handshakeData = Buffer.concat([
                        writeVarInt(0x00),           // Packet ID
                        writeVarInt(763),             // Protocol version (1.20.1)
                        writeVarInt(hostBuf.length),  // Host string length
                        hostBuf,                      // Host
                        portBuf,                      // Port
                        writeVarInt(1)                // Next state: Status
                    ]);
                    const handshakePacket = Buffer.concat([writeVarInt(handshakeData.length), handshakeData]);

                    // Build Status Request packet (id=0x00, empty)
                    const statusRequest = Buffer.from([0x01, 0x00]);

                    socket.write(Buffer.concat([handshakePacket, statusRequest]));
                });

                socket.on('data', (data) => {
                    responseBuffer = Buffer.concat([responseBuffer, data]);

                    try {
                        let offset = 0;
                        const { value: packetLength, size: pLenSize } = readVarInt(responseBuffer, offset);
                        offset += pLenSize;

                        if (responseBuffer.length < packetLength + pLenSize) return; // Wait for more data

                        const { size: pIdSize } = readVarInt(responseBuffer, offset);
                        offset += pIdSize;

                        const { value: strLen, size: sLenSize } = readVarInt(responseBuffer, offset);
                        offset += sLenSize;

                        const jsonStr = responseBuffer.slice(offset, offset + strLen).toString('utf8');
                        const ping = Date.now() - startTime;
                        socket.destroy();

                        const parsed = JSON.parse(jsonStr);
                        resolve({
                            success: true,
                            online: true,
                            ping,
                            players: parsed.players || { online: 0, max: 0 },
                            version: parsed.version?.name || 'Unknown',
                            motd: typeof parsed.description === 'string' 
                                ? parsed.description 
                                : parsed.description?.text || '',
                            favicon: parsed.favicon || null
                        });
                    } catch (e) {
                        // Not enough data yet, wait
                    }
                });

                socket.on('timeout', () => {
                    socket.destroy();
                    resolve({ success: true, ping: -1, online: false, players: { online: 0, max: 0 }, version: '', motd: '' });
                });

                socket.on('error', () => {
                    socket.destroy();
                    resolve({ success: true, ping: -1, online: false, players: { online: 0, max: 0 }, version: '', motd: '' });
                });
            });
        } catch (e) {
            return { success: false, error: e.message };
        }
    });
}

module.exports = { setupServersIpc };
