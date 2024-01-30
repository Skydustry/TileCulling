package it.feargames.tileculling;

import it.feargames.tileculling.util.NMSUtils;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;
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

    private final Map<World, Long2ObjectMap<ChunkEntry>> cachedChunks = new HashMap<>();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();
    private final ReentrantReadWriteLock.ReadLock readLock = lock.readLock();

    public ChunkCache(CullingPlugin plugin, HiddenTileRegistry hiddenTileRegistry, NMSUtils nms) {
        this.plugin = plugin;
        this.hiddenTileRegistry = hiddenTileRegistry;
        this.nms = nms;
    }

    static class ChunkEntry {
        PalettedContainer<net.minecraft.world.level.block.state.BlockState>[] blocks;
        List<BlockState> tiles;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onWorldUnload(WorldUnloadEvent event) {
        cachedChunks.remove(event.getWorld());
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
        updateCachedChunkSync(chunk.getWorld(), chunk.getChunkKey(), chunk);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChunkUnload(ChunkUnloadEvent e) {
        Chunk chunk = e.getChunk();
        updateCachedChunkSync(chunk.getWorld(), chunk.getChunkKey(), null);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent e) {
        handleExplosionSync(e.blockList());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent e) {
        handleExplosionSync(e.blockList());
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

    private void updateCachedChunkSync(World world, long chunkKey, final Chunk chunk) {
        if (chunk == null) {
            try {
                writeLock.lock();
                Long2ObjectMap<ChunkEntry> entries = cachedChunks.get(world);
                if (entries != null) {
                    entries.remove(chunkKey);
                }
            } finally {
                writeLock.unlock();
            }

            return;
        }

        try {
            writeLock.lock();
            ChunkEntry entry = cachedChunks.computeIfAbsent(world, k -> new Long2ObjectOpenHashMap<>()).computeIfAbsent(chunkKey, k -> new ChunkEntry());
            entry.blocks = nms.getBlockIds(chunk);
            entry.tiles = filterTiles(chunk.getTileEntities());
        } finally {
            writeLock.unlock();
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
        try {
            readLock.lock();
            Long2ObjectMap<ChunkEntry> entries = cachedChunks.get(world);
            if (entries == null) {
                return null;
            }

            ChunkEntry entry = entries.get(chunkKey);
            if (entry == null) {
                return null;
            }

            return entry.tiles;
        } finally {
            readLock.unlock();
        }
    }

    public PalettedContainer<net.minecraft.world.level.block.state.BlockState>[] getBlocks(World world, long chunkKey) {
        try {
            readLock.lock();
            Long2ObjectMap<ChunkEntry> entries = cachedChunks.get(world);
            if (entries == null) {
                return null;
            }

            ChunkEntry entry = entries.get(chunkKey);
            if (entry == null) {
                return null;
            }

            return entry.blocks;
        } finally {
            readLock.unlock();
        }
    }

}
