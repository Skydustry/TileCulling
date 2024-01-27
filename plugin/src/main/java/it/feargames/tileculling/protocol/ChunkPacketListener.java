package it.feargames.tileculling.protocol;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.*;
import it.feargames.tileculling.util.NMSUtils;
import it.feargames.tileculling.CullingPlugin;
import it.feargames.tileculling.HiddenTileRegistry;
import it.feargames.tileculling.PlayerChunkTracker;
import java.util.Arrays;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;

public class ChunkPacketListener extends PacketAdapter {

    private final HiddenTileRegistry hiddenTileRegistry;
    private final NMSUtils nms;
    private final PlayerChunkTracker playerChunkTracker;

    public ChunkPacketListener(CullingPlugin plugin, HiddenTileRegistry hiddenTileRegistry, NMSUtils nms, PlayerChunkTracker playerChunkTracker) {
        super(plugin, ListenerPriority.HIGHEST, Arrays.asList(PacketType.Play.Server.MAP_CHUNK, PacketType.Play.Server.UNLOAD_CHUNK), ListenerOptions.ASYNC);
        this.hiddenTileRegistry = hiddenTileRegistry;
        this.plugin = plugin;
        this.nms = nms;
        this.playerChunkTracker = playerChunkTracker;
    }

    @Override
    public void onPacketSending(PacketEvent event) {
        Player player = event.getPlayer();
        PacketContainer packet = event.getPacket();

        int chunkX;
        int chunkZ;
        long chunkKey;

        if (packet.getType() == PacketType.Play.Server.MAP_CHUNK) {
            chunkX = packet.getIntegers().read(0);
            chunkZ = packet.getIntegers().read(1);
            chunkKey = Chunk.getChunkKey(chunkX, chunkZ);
            nms.transformPacket(player, packet, chunkX, chunkZ, hiddenTileRegistry::shouldHide);
            playerChunkTracker.trackChunk(player, chunkKey);
        } else {
            chunkX = packet.getChunkCoordIntPairs().read(0).getChunkX();
            chunkZ = packet.getChunkCoordIntPairs().read(0).getChunkZ();
            chunkKey = Chunk.getChunkKey(chunkX, chunkZ);
            playerChunkTracker.untrackChunk(player, chunkKey);
        }
    }
}
