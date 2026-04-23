package net.elysiastudios.client.social;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.SocketAddress;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class ElysiaPresenceManager {
    private static final String FIREBASE_USERS_URL = "https://elysiapanel-195c1-default-rtdb.firebaseio.com/users";
    private static final long LOOKUP_TTL_MS = 45_000L;
    private static final long LAST_SEEN_TTL_MS = 120_000L;
    private static final long CACHE_TTL_MS = 300_000L;
    private static final long PUBLISH_INTERVAL_MS = 30_000L;
    private static final int LOOKUPS_PER_TICK = 2;
    private static final ElysiaPresenceManager INSTANCE = new ElysiaPresenceManager();

    private final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build();
    private final Map<UUID, PresenceSnapshot> cache = new ConcurrentHashMap<>();
    private final Set<UUID> pendingLookups = ConcurrentHashMap.newKeySet();
    private final ConcurrentLinkedQueue<UUID> lookupQueue = new ConcurrentLinkedQueue<>();

    private UUID publishedUuid;
    private String publishedName;
    private boolean publishedOnline;
    private String publishedContextKey = "";
    private long lastPublishAttempt;
    private long lastCleanupAt;

    private ElysiaPresenceManager() {
    }

    public static ElysiaPresenceManager getInstance() {
        return INSTANCE;
    }

    public void tick(Minecraft minecraft) {
        long now = System.currentTimeMillis();
        pumpLookups(now);
        cleanup(now);

        if (minecraft == null) {
            return;
        }

        SessionContext context = resolveSessionContext(minecraft);
        if (context.isInWorld()) {
            UUID uuid = context.uuid;
            String username = context.username;
            PresenceSnapshot current = cache.get(uuid);
            if (current == null || !current.elysiaOnline || now - current.lastSeenAt > 5_000L) {
                cache.put(uuid, new PresenceSnapshot(username, true, now, now));
            }

            boolean identityChanged = !uuid.equals(publishedUuid) || !username.equals(publishedName);
            boolean contextChanged = !context.signature().equals(publishedContextKey);
            boolean publishHeartbeat = now - lastPublishAttempt >= PUBLISH_INTERVAL_MS;

            if (!publishedOnline || identityChanged || contextChanged || publishHeartbeat) {
                publishedUuid = uuid;
                publishedName = username;
                publishedOnline = true;
                publishedContextKey = context.signature();
                lastPublishAttempt = now;
                publishPresence(context, now);
            }
        } else if (publishedOnline && publishedUuid != null) {
            publishedOnline = false;
            publishedContextKey = SessionContext.menu(publishedUuid, publishedName).signature();
            lastPublishAttempt = now;
            publishPresence(SessionContext.menu(publishedUuid, publishedName), now);
        } else if (!publishedOnline && publishedUuid != null && now - lastPublishAttempt >= PUBLISH_INTERVAL_MS) {
            SessionContext menuContext = SessionContext.menu(publishedUuid, publishedName);
            publishedContextKey = menuContext.signature();
            lastPublishAttempt = now;
            publishPresence(menuContext, now);
        }
    }

    public boolean isElysiaUser(UUID uuid) {
        if (uuid == null) {
            return false;
        }

        long now = System.currentTimeMillis();
        PresenceSnapshot snapshot = cache.get(uuid);
        if (snapshot == null || snapshot.isStale(now)) {
            queueLookup(uuid);
        }
        return snapshot != null && snapshot.isVisible(now);
    }

    private void queueLookup(UUID uuid) {
        if (uuid == null || pendingLookups.contains(uuid)) {
            return;
        }
        pendingLookups.add(uuid);
        lookupQueue.offer(uuid);
    }

    private void pumpLookups(long now) {
        for (int i = 0; i < LOOKUPS_PER_TICK; i++) {
            UUID uuid = lookupQueue.poll();
            if (uuid == null) {
                return;
            }
            fetchPresence(uuid, now);
        }
    }

    private void fetchPresence(UUID uuid, long now) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(FIREBASE_USERS_URL + "/" + formatUuid(uuid) + ".json"))
            .timeout(Duration.ofSeconds(6))
            .GET()
            .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(HttpResponse::body)
            .thenAccept(body -> cache.put(uuid, parseSnapshot(body, now)))
            .exceptionally(error -> {
                cache.put(uuid, new PresenceSnapshot(null, false, 0L, now));
                return null;
            })
            .whenComplete((ignored, error) -> pendingLookups.remove(uuid));
    }

    private PresenceSnapshot parseSnapshot(String body, long now) {
        if (body == null || body.isBlank() || "null".equals(body)) {
            return new PresenceSnapshot(null, false, 0L, now);
        }

        try {
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            String username = readString(json, "username");
            String status = readString(json, "status");
            String client = readString(json, "client");
            String launcher = readString(json, "launcher");
            long lastSeen = json.has("lastSeen") ? json.get("lastSeen").getAsLong() : 0L;
            boolean isFresh = lastSeen > 0L && now - lastSeen <= LAST_SEEN_TTL_MS;
            boolean elysiaClient = "elysia-client".equalsIgnoreCase(client)
                || "elysia".equalsIgnoreCase(launcher)
                || (json.has("elysiaClient") && json.get("elysiaClient").getAsBoolean());
            boolean visible = "online".equalsIgnoreCase(status) && isFresh && elysiaClient;
            return new PresenceSnapshot(username, visible, lastSeen, now);
        } catch (RuntimeException ignored) {
            return new PresenceSnapshot(null, false, 0L, now);
        }
    }

    private void publishPresence(SessionContext context, long now) {
        if (context == null || context.uuid == null || context.username == null || context.username.isBlank()) {
            return;
        }

        JsonObject payload = new JsonObject();
        payload.addProperty("username", context.username);
        payload.addProperty("status", context.isInWorld() ? "online" : "offline");
        payload.addProperty("lastSeen", now);
        payload.addProperty("client", "elysia-client");
        payload.addProperty("launcher", "elysia");
        payload.addProperty("elysiaClient", true);
        payload.addProperty("mode", context.mode);
        payload.addProperty("serverType", context.mode);
        payload.addProperty("server", context.serverDisplay);

        if (!context.serverAddress.isBlank()) {
            payload.addProperty("serverAddress", context.serverAddress);
        }

        if (!context.serverName.isBlank()) {
            payload.addProperty("serverName", context.serverName);
        }

        // 1. Publish to Firebase
        HttpRequest request = HttpRequest.newBuilder(URI.create(FIREBASE_USERS_URL + "/" + formatUuid(context.uuid) + ".json"))
            .timeout(Duration.ofSeconds(6))
            .header("Content-Type", "application/json")
            .method("PATCH", HttpRequest.BodyPublishers.ofString(payload.toString()))
            .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.discarding());

        // 2. Write to local file for Launcher RPC
        try {
            java.nio.file.Path path = java.nio.file.Paths.get(System.getProperty("user.home"), "AppData", "Roaming", ".elysia-launcher", "minecraft", "runtime", "presence.json");
            java.nio.file.Files.createDirectories(path.getParent());
            java.nio.file.Files.writeString(path, payload.toString());
        } catch (Exception e) {
            // Ignore errors for local write
        }
    }

    private void cleanup(long now) {
        if (now - lastCleanupAt < 30_000L) {
            return;
        }
        lastCleanupAt = now;
        cache.entrySet().removeIf(entry -> now - entry.getValue().fetchedAt > CACHE_TTL_MS);
    }

    private static String formatUuid(UUID uuid) {
        return uuid.toString().replace("-", "");
    }

    private SessionContext resolveSessionContext(Minecraft minecraft) {
        if (minecraft == null || minecraft.player == null || minecraft.getConnection() == null) {
            return SessionContext.menu(null, null);
        }

        UUID uuid = minecraft.player.getUUID();
        String username = minecraft.player.getGameProfile().name();
        if (minecraft.isLocalServer() || minecraft.hasSingleplayerServer()) {
            return SessionContext.singleplayer(uuid, username);
        }

        ServerData currentServer = minecraft.getCurrentServer();
        String configuredAddress = sanitizeServerAddress(currentServer != null ? currentServer.ip : null);
        String configuredName = sanitizeValue(currentServer != null ? currentServer.name : null);
        String remoteAddress = extractRemoteServerAddress(minecraft);
        String serverAddress = !configuredAddress.isBlank() ? configuredAddress : remoteAddress;

        if (serverAddress.isBlank() && !configuredName.isBlank()) {
            serverAddress = configuredName;
        }

        String serverDisplay = !serverAddress.isBlank() ? serverAddress : "Serveur multijoueur";
        return SessionContext.multiplayer(uuid, username, serverDisplay, serverAddress, configuredName);
    }

    private static String extractRemoteServerAddress(Minecraft minecraft) {
        try {
            if (minecraft == null || minecraft.getConnection() == null || minecraft.getConnection().getConnection() == null) {
                return "";
            }

            SocketAddress remoteAddress = minecraft.getConnection().getConnection().getRemoteAddress();
            if (remoteAddress instanceof InetSocketAddress inetSocketAddress) {
                InetAddress inetAddress = inetSocketAddress.getAddress();
                String host = sanitizeServerAddress(inetSocketAddress.getHostString());
                if (host.isBlank() && inetAddress != null) {
                    host = sanitizeServerAddress(inetAddress.getHostAddress());
                }

                if (host.isBlank() || "local".equalsIgnoreCase(host) || "localhost".equalsIgnoreCase(host)) {
                    return "";
                }

                if (inetAddress != null && (inetAddress.isLoopbackAddress() || inetAddress.isAnyLocalAddress())) {
                    return "";
                }

                int port = inetSocketAddress.getPort();
                if (port > 0 && port != 25565) {
                    return host + ":" + port;
                }
                return host;
            }

            return sanitizeServerAddress(remoteAddress != null ? remoteAddress.toString() : "");
        } catch (RuntimeException ignored) {
            return "";
        }
    }

    private static String sanitizeServerAddress(String value) {
        String sanitized = sanitizeValue(value);
        if (sanitized.isBlank()) {
            return "";
        }

        if (sanitized.startsWith("/")) {
            sanitized = sanitized.substring(1).trim();
        }

        if (sanitized.contains("/")) {
            String[] parts = sanitized.split("/");
            sanitized = "";
            for (int i = parts.length - 1; i >= 0; i--) {
                String candidate = parts[i].trim();
                if (!candidate.isBlank()) {
                    sanitized = candidate;
                    break;
                }
            }
        }

        if ("local".equalsIgnoreCase(sanitized) || "localhost".equalsIgnoreCase(sanitized)) {
            return "";
        }

        return sanitized;
    }

    private static String sanitizeValue(String value) {
        return value == null ? "" : value.trim();
    }

    private static String readString(JsonObject json, String key) {
        return json.has(key) ? json.get(key).getAsString() : "";
    }

    private static final class SessionContext {
        private final UUID uuid;
        private final String username;
        private final String mode;
        private final String serverDisplay;
        private final String serverAddress;
        private final String serverName;

        private SessionContext(UUID uuid, String username, String mode, String serverDisplay, String serverAddress, String serverName) {
            this.uuid = uuid;
            this.username = username;
            this.mode = mode;
            this.serverDisplay = serverDisplay;
            this.serverAddress = serverAddress;
            this.serverName = serverName;
        }

        private static SessionContext menu(UUID uuid, String username) {
            return new SessionContext(uuid, username, "menu", "Menus", "", "");
        }

        private static SessionContext singleplayer(UUID uuid, String username) {
            return new SessionContext(uuid, username, "singleplayer", "Solo", "", "");
        }

        private static SessionContext multiplayer(UUID uuid, String username, String serverDisplay, String serverAddress, String serverName) {
            return new SessionContext(uuid, username, "multiplayer", serverDisplay, serverAddress, serverName);
        }

        private boolean isInWorld() {
            return "singleplayer".equals(mode) || "multiplayer".equals(mode);
        }

        private String signature() {
            return (uuid == null ? "none" : uuid.toString())
                + "|" + sanitizeValue(username)
                + "|" + mode
                + "|" + sanitizeValue(serverDisplay)
                + "|" + sanitizeValue(serverAddress)
                + "|" + sanitizeValue(serverName);
        }
    }

    private static final class PresenceSnapshot {
        private final String username;
        private final boolean elysiaOnline;
        private final long lastSeenAt;
        private final long fetchedAt;

        private PresenceSnapshot(String username, boolean elysiaOnline, long lastSeenAt, long fetchedAt) {
            this.username = username;
            this.elysiaOnline = elysiaOnline;
            this.lastSeenAt = lastSeenAt;
            this.fetchedAt = fetchedAt;
        }

        private boolean isStale(long now) {
            return now - fetchedAt > LOOKUP_TTL_MS;
        }

        private boolean isVisible(long now) {
            return elysiaOnline && now - fetchedAt <= LOOKUP_TTL_MS && now - lastSeenAt <= LAST_SEEN_TTL_MS;
        }
    }
}
