const FIREBASE_URL = 'https://elysiapanel-195c1-default-rtdb.firebaseio.com';

export class FirebaseClient {
    static async put(path, data) {
        const response = await fetch(`${FIREBASE_URL}${path}.json`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        });
        if (!response.ok) throw new Error('Firebase PUT failed');
        return response.json();
    }

    static async patch(path, data) {
        const response = await fetch(`${FIREBASE_URL}${path}.json`, {
            method: 'PATCH',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        });
        if (!response.ok) throw new Error('Firebase PATCH failed');
        return response.json();
    }

    static async get(path) {
        const response = await fetch(`${FIREBASE_URL}${path}.json`);
        if (!response.ok) throw new Error('Firebase GET failed');
        return response.json();
    }

    static async delete(path) {
        const response = await fetch(`${FIREBASE_URL}${path}.json`, {
            method: 'DELETE'
        });
        if (!response.ok) throw new Error('Firebase DELETE failed');
        return response.json();
    }

    // Server-Sent Events (SSE) for Realtime updates
    static listen(path, callback) {
        const source = new EventSource(`${FIREBASE_URL}${path}.json`);
        
        source.addEventListener('put', (e) => {
            try {
                const data = JSON.parse(e.data);
                callback(data);
            } catch (err) {
                console.error('SSE parse error:', err);
            }
        });

        source.addEventListener('patch', (e) => {
            try {
                const data = JSON.parse(e.data);
                callback(data);
            } catch (err) {
                console.error('SSE parse error:', err);
            }
        });

        source.onerror = (e) => {
            console.error('Firebase SSE Error:', e);
            // Optionally auto-reconnect logic if it closes
        };

        return source; // Return so we can close it later if needed (source.close())
    }
}
