package it.feargames.tileculling.occlusionculling;

import com.logisticscraft.occlusionculling.DataProvider;
import com.logisticscraft.occlusionculling.util.Vec3d;
import it.feargames.tileculling.ChunkCache;
import it.feargames.tileculling.CullingPlugin;
import net.minecraft.world.level.chunk.PalettedContainer;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_20_R3.util.CraftMagicNumbers;

public class PaperDataProvider implements DataProvider {

    private final ChunkCache chunkCache;

    private World world;
    private long chunkKey;
    private PalettedContainer<net.minecraft.world.level.block.state.BlockState>[] blockIds;

    public PaperDataProvider(ChunkCache chunkCache) {
        this.chunkCache = chunkCache;
    }

    @Override
    public boolean prepareChunk(int chunkX, int chunkZ) {
        if (world == null) {
            throw new IllegalStateException("World not loaded into DataProvider!");
        }

        if (chunkKey == Chunk.getChunkKey(chunkX, chunkZ)) {
            return true;
        }

        chunkKey = Chunk.getChunkKey(chunkX, chunkZ);
        blockIds = chunkCache.getBlocks(world, chunkKey);
        return blockIds != null;
    }

    @Override
    public boolean isOpaqueFullCube(int x, int y, int z) {
        if (blockIds == null) {
            throw new IllegalStateException("Chunk not loaded into DataProvider!");
        }

        if (y < world.getMinHeight() || y > world.getMaxHeight()) {
            return false;
        }

        int relativeX = x & 0xF;
        int relativeZ = z & 0xF;
        Material material = CraftMagicNumbers.getMaterial(blockIds[(y + 64) >> 4].get(relativeX, y & 0xF, relativeZ).getBlock());
        return CullingPlugin.isOccluding(material);
    }

    @Override
    public void cleanup() {
        this.chunkKey = 0;
        this.blockIds = null;
    }

    @Override
    public void checkingPosition(Vec3d[] targetPoints, int size, Vec3d viewerPosition) {
    }

    public void setWorld(World world) {
        this.world = world;
    }
}
