package it.feargames.tileculling;

import it.feargames.tileculling.util.NMSUtils;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.*;
import java.util.concurrent.locks.StampedLock;
import net.minecraft.world.level.chunk.PalettedContainer;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.WorldUnloadEvent;

public class ChunkCache implements Listener {

    private final CullingPlugin plugin;
    private final HiddenTileRegistry hiddenTileRegistry;
    private final NMSUtils nms;

    private final Object2ObjectMap<UUID, Long2ObjectMap<ChunkEntry>> cachedChunks;
    private final StampedLock lock = new StampedLock();

    public ChunkCache(CullingPlugin plugin, HiddenTileRegistry hiddenTileRegistry, NMSUtils nms) {
        this.plugin = plugin;
        this.hiddenTileRegistry = hiddenTileRegistry;
        this.nms = nms;

        cachedChunks = new Object2ObjectOpenHashMap<>(Bukkit.getWorlds().size());
    }

    static class ChunkEntry {
        PalettedContainer<net.minecraft.world.level.block.state.BlockState>[] blocks;
        List<BlockState> tiles;
    }

    private void updateCachedChunkSync(World world, long chunkKey, final Chunk chunk) {
        if (chunk == null) {
            long stamp = lock.writeLock();
            try {
                Long2ObjectMap<ChunkEntry> entries = cachedChunks.get(world.getUID());
                if (entries != null) {
                    entries.remove(chunkKey);
                }
            } finally {
                lock.unlockWrite(stamp);
            }

            return;
        }

        long stamp = lock.writeLock();
        try {
            ChunkEntry entry = cachedChunks.computeIfAbsent(world.getUID(), k -> new Long2ObjectOpenHashMap<>(256, 0.65F)).computeIfAbsent(chunkKey, k -> new ChunkEntry());
            entry.blocks = nms.getBlockIds(chunk);
            entry.tiles = filterTiles(chunk.getTileEntities());
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    private List<BlockState> filterTiles(BlockState[] tiles) {
        if (tiles.length == 0) {
            return Collections.emptyList();
        }

        List<BlockState> result = new LinkedList<>();
        for (BlockState state : tiles) {
            if (hiddenTileRegistry.shouldHide(state)) {
                result.add(state);
            }
        }

        return result;
    }

    public List<BlockState> getChunkTiles(World world, long chunkKey) {
        long stamp = lock.tryOptimisticRead();
        if (lock.validate(stamp)) {
            ChunkEntry entry = getChunkEntry(world, chunkKey);
            return entry.tiles;
        } else {
            stamp = lock.readLock();
            try {
                ChunkEntry entry = getChunkEntry(world, chunkKey);
                return entry.tiles;
            } finally {
                lock.unlockRead(stamp);
            }
        }
    }

    public PalettedContainer<net.minecraft.world.level.block.state.BlockState>[] getBlocks(World world, long chunkKey) {
        long stamp = lock.tryOptimisticRead();
        if (lock.validate(stamp)) {
            ChunkEntry entry = getChunkEntry(world, chunkKey);
            return entry.blocks;
        } else {
            stamp = lock.readLock();
            try {
                ChunkEntry entry = getChunkEntry(world, chunkKey);
                return entry.blocks;
            } finally {
                lock.unlockRead(stamp);
            }
        }
    }

    private ChunkEntry getChunkEntry(World world, long chunkKey) {
        Long2ObjectMap<ChunkEntry> entries = cachedChunks.get(world.getUID());
        return entries != null ? entries.get(chunkKey) : null;
    }

    private void handleExplosionSync(List<Block> blockList) {
        Set<Chunk> chunks = new HashSet<>();

        for (Block block : blockList) {
            chunks.add(block.getChunk());
        }

        for (Chunk chunk : chunks) {
            updateCachedChunkSync(chunk.getWorld(), chunk.getChunkKey(), chunk);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onWorldUnload(WorldUnloadEvent event) {
        cachedChunks.remove(event.getWorld().getUID());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent e) {
        Block b = e.getBlock();
        Material type = b.getType();

        if (!CullingPlugin.isOccluding(type) && !hiddenTileRegistry.shouldHide(type)) {
            return;
        }

        Chunk chunk = b.getChunk();
        Bukkit.getScheduler().runTask(plugin, () -> {
            updateCachedChunkSync(chunk.getWorld(), chunk.getChunkKey(), chunk);
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent e) {
        Block b = e.getBlock();
        Material type = b.getType();

        if (!CullingPlugin.isOccluding(type) && !hiddenTileRegistry.shouldHide(type)) {
            return;
        }

        Chunk chunk = b.getChunk();
        Bukkit.getScheduler().runTask(plugin, () -> {
            updateCachedChunkSync(chunk.getWorld(), chunk.getChunkKey(), chunk);
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChunkLoad(ChunkLoadEvent e) {
        Chunk chunk = e.getChunk();
        Bukkit.getScheduler().runTask(plugin, () -> {
            updateCachedChunkSync(chunk.getWorld(), chunk.getChunkKey(), chunk);
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChunkUnload(ChunkUnloadEvent e) {
        Chunk chunk = e.getChunk();
        Bukkit.getScheduler().runTask(plugin, () -> {
            updateCachedChunkSync(chunk.getWorld(), chunk.getChunkKey(), null);
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent e) {
        handleExplosionSync(e.blockList());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent e) {
        handleExplosionSync(e.blockList());
    }
}
