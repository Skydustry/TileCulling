package it.feargames.tileculling;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class PlayerChunkTracker implements Listener {

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();
    private final Map<Player, LongSet> trackedPlayers;

    public PlayerChunkTracker(CullingPlugin plugin) {
        trackedPlayers = new HashMap<>();
    }

    public void trackChunk(Player player, long chunkKey) {
        try {
            writeLock.lock();
            trackedPlayers.computeIfAbsent(player, k -> new LongOpenHashSet()).add(chunkKey);
        } finally {
            writeLock.unlock();
        }
    }

    public void untrackChunk(Player player, long chunkKey) {
        try {
            writeLock.lock();
            LongSet chunkKeys = trackedPlayers.get(player);
            if (chunkKeys == null) {
                return;
            }
            chunkKeys.remove(chunkKey);
        } finally {
            writeLock.unlock();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerChangeWorld(PlayerChangedWorldEvent event) {
        try {
            writeLock.lock();
            trackedPlayers.remove(event.getPlayer());
        } finally {
            writeLock.unlock();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        try {
            writeLock.lock();
            trackedPlayers.remove(event.getPlayer());
        } finally {
            writeLock.unlock();
        }
    }

    public long[] getTrackedChunks(Player player) {
        LongSet trackedChunks = trackedPlayers.get(player);

        if (trackedChunks == null) {
            return null;
        }

        return trackedChunks.toArray(new long[0]);
    }
}
