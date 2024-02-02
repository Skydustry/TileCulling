package it.feargames.tileculling;

import it.feargames.tileculling.util.BlockUtils;
import it.unimi.dsi.fastutil.longs.Long2BooleanMap;
import it.unimi.dsi.fastutil.longs.Long2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.UUID;
import java.util.concurrent.locks.StampedLock;
import java.util.function.LongPredicate;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

public class VisibilityCache implements Listener {

    private final Object2ObjectMap<UUID, Long2BooleanMap> hiddenBlocks;
    private final StampedLock lock = new StampedLock();

    public VisibilityCache() {
        hiddenBlocks = new Object2ObjectOpenHashMap<>();
    }

    public void setHidden(Player player, long blockKey, boolean hidden) {
        long stamp = lock.writeLock();
        try {
            hiddenBlocks.computeIfAbsent(player.getUniqueId(), p -> new Long2BooleanOpenHashMap()).put(blockKey, hidden);
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    public void setHidden(Player player, Location blockLocation, boolean hidden) {
        setHidden(player, BlockUtils.getBlockKey(blockLocation), hidden);
    }

    public boolean isHidden(Player player, Location blockLocation) {
        return isHidden(player, BlockUtils.getBlockKey(blockLocation));
    }

    public boolean isHidden(Player player, long blockKey) {
        long stamp = lock.tryOptimisticRead();
        if (!lock.validate(stamp)) {
            stamp = lock.readLock();
            try {
                return getHiddenResult(player, blockKey);
            } finally {
                lock.unlockRead(stamp);
            }
        } else {
            return getHiddenResult(player, blockKey);
        }
    }

    private boolean getHiddenResult(Player player, long blockKey) {
        Long2BooleanMap blocks = hiddenBlocks.get(player.getUniqueId());
        boolean result;

        if (blocks == null) {
            result = true;
        } else {
            result = blocks.getOrDefault(blockKey, true);
        }

        return result;
    }

    private void invalidateCache(Player player) {
        long stamp = lock.writeLock();
        try {
            hiddenBlocks.remove(player.getUniqueId());
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    @EventHandler
    public void onUnload(ChunkUnloadEvent event) {
        long stamp = lock.writeLock();
        try {
            for (Long2BooleanMap blocks : hiddenBlocks.values()) {
                blocks.keySet().removeIf((LongPredicate) block -> {
                    int chunkX = BlockUtils.getBlockKeyX(block) >> 4;
                    if (event.getChunk().getX() != chunkX) {
                        return false;
                    }
                    int chunkZ = BlockUtils.getBlockKeyZ(block) >> 4;
                    return event.getChunk().getZ() == chunkZ;
                });
            }
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent event) {
        invalidateCache(event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onTeleport(PlayerTeleportEvent event) {
        invalidateCache(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        invalidateCache(event.getPlayer());
    }
}
