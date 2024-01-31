package it.feargames.tileculling;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.UUID;
import java.util.concurrent.locks.StampedLock;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerChunkTracker implements Listener {

    private final Object2ObjectMap<UUID, LongSet> trackedPlayers;
    private final StampedLock lock = new StampedLock();

    public PlayerChunkTracker() {
        trackedPlayers = new Object2ObjectOpenHashMap<>();
    }

    public void trackChunk(Player player, long chunkKey) {
        long stamp = lock.writeLock();
        try {
            trackedPlayers.computeIfAbsent(player.getUniqueId(), k -> new LongOpenHashSet()).add(chunkKey);
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    public void untrackChunk(Player player, long chunkKey) {
        long stamp = lock.writeLock();
        try {
            LongSet chunkKeys = trackedPlayers.get(player.getUniqueId());
            if (chunkKeys == null) {
                return;
            }

            chunkKeys.remove(chunkKey);
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerChangeWorld(PlayerChangedWorldEvent event) {
        long stamp = lock.writeLock();
        try {
            trackedPlayers.remove(event.getPlayer().getUniqueId());
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        long stamp = lock.writeLock();
        try {
            trackedPlayers.remove(event.getPlayer().getUniqueId());
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    public long[] getTrackedChunks(Player player) {
        long stamp = lock.tryOptimisticRead();
        if (!lock.validate(stamp)) {
            stamp = lock.readLock();
            try {
                return getChunksArray(player);
            } finally {
                lock.unlockRead(stamp);
            }
        } else {
            return getChunksArray(player);
        }
    }

    private long[] getChunksArray(Player player) {
        LongSet trackedChunks = trackedPlayers.get(player.getUniqueId());
        if (trackedChunks == null) {
            return null;
        }

        return trackedChunks.toArray(new long[0]);
    }
}
