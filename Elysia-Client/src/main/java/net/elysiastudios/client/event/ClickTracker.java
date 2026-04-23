package net.elysiastudios.client.event;

import java.util.ArrayList;
import java.util.List;

public class ClickTracker {
    private static final List<Long> leftClicks = new ArrayList<>();
    private static int pendingLeftClicks;
    private static long lastLeftClickTime;

    public static synchronized void addLeftClick() {
        long now = System.currentTimeMillis();
        leftClicks.add(now);
        pendingLeftClicks++;
        lastLeftClickTime = now;
    }

    public static synchronized int getCPS() {
        long now = System.currentTimeMillis();
        leftClicks.removeIf(time -> now - time > 1000);
        return leftClicks.size();
    }

    public static synchronized int consumePendingLeftClicks() {
        int pending = pendingLeftClicks;
        pendingLeftClicks = 0;
        return pending;
    }

    public static synchronized long getLastLeftClickTime() {
        return lastLeftClickTime;
    }
}
