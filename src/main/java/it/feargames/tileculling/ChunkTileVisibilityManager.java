package it.feargames.tileculling;

import it.feargames.tileculling.util.NMSUtils;
import com.logisticscraft.occlusionculling.OcclusionCullingInstance;
import com.logisticscraft.occlusionculling.cache.ArrayOcclusionCache;
import com.logisticscraft.occlusionculling.util.Vec3d;
import it.feargames.tileculling.occlusionculling.PaperDataProvider;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.TileState;
import org.bukkit.entity.Player;

import java.util.List;

public class ChunkTileVisibilityManager {

    private final NMSUtils nms;
    private final PlayerChunkTracker playerTracker;
    private final VisibilityCache visibilityCache;
    private final ChunkCache chunkCache;

    private final PaperDataProvider dataProvider;
    private final OcclusionCullingInstance culling;

    private final Vec3d viewerPosition = new Vec3d(0, 0, 0);
    private final Vec3d aabbMin = new Vec3d(0, 0, 0);
    private final Vec3d aabbMax = new Vec3d(0, 0, 0);

    public ChunkTileVisibilityManager(SettingsHolder settings, NMSUtils nms, PlayerChunkTracker playerTracker, VisibilityCache visibilityCache, ChunkCache chunkCache) {
        this.nms = nms;
        this.playerTracker = playerTracker;
        this.visibilityCache = visibilityCache;
        this.chunkCache = chunkCache;

        this.dataProvider = new PaperDataProvider(chunkCache);
        this.culling = new OcclusionCullingInstance(settings.getTileRange(), dataProvider, new ArrayOcclusionCache(settings.getTileRange()), -0.01);
    }

    public void updateVisibility(Player player) {
        long[] trackedChunks = playerTracker.getTrackedChunks(player);

        if (trackedChunks == null) {
            return;
        }

        World world = player.getWorld();
        Location playerEyeLocation = player.getEyeLocation();

        viewerPosition.set(playerEyeLocation.getX(), playerEyeLocation.getY(), playerEyeLocation.getZ());

        culling.resetCache();
        dataProvider.setWorld(world);

        for (long chunkKey : trackedChunks) {
            List<BlockState> tiles = chunkCache.getChunkTiles(world, chunkKey);

            if (tiles == null) {
                continue;
            }

            for (BlockState block : tiles) {
                int blockX = block.getX();
                int blockY = block.getY();
                int blockZ = block.getZ();

                aabbMin.set(blockX, blockY, blockZ);
                aabbMax.set(blockX + 1, blockY + 1, blockZ + 1);
                Location bLoc = block.getLocation();

                boolean canSee = culling.isAABBVisible(aabbMin, aabbMax, viewerPosition);
                boolean hidden = visibilityCache.isHidden(player, bLoc);

                if (hidden && canSee) {
                    nms.updateBlockState(player, bLoc, block.getBlockData());
                    visibilityCache.setHidden(player, bLoc, false);

                    if (block instanceof TileState) {
                        nms.updateBlockData(player, bLoc, block);
                    }

                    continue;
                }

                if (!hidden && !canSee) {
                    nms.updateBlockState(player, bLoc, null);
                    visibilityCache.setHidden(player, bLoc, true);
                }
            }
        }

        dataProvider.setWorld(null);
    }
}
