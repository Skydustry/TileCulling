package it.feargames.tileculling;

import it.feargames.tileculling.util.NMSUtils;
import com.comphenix.protocol.ProtocolLibrary;
import it.feargames.tileculling.protocol.ChunkPacketListener;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class CullingPlugin extends JavaPlugin {

    private SettingsHolder settings;
    private HiddenTileRegistry hiddenTileRegistry;

    private NMSUtils nms;
    private ChunkTileVisibilityManager chunkTileVisibilityManager;
    private PlayerChunkTracker playerChunkTracker;
    private ChunkCache chunkCache;
    private VisibilityCache visibilityCache;
    private ChunkPacketListener chunkPacketListener;

    private VisibilityUpdateTask visibilityUpdateTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        settings = new SettingsHolder();
        settings.load(getConfig().getConfigurationSection("settings"));
        hiddenTileRegistry = new HiddenTileRegistry(getLogger());
        hiddenTileRegistry.load(getConfig().getConfigurationSection("hiddenTiles"));

        playerChunkTracker = new PlayerChunkTracker(this);
        visibilityCache = new VisibilityCache();
        nms = new NMSUtils();
        chunkCache = new ChunkCache(this, hiddenTileRegistry, nms);
        chunkTileVisibilityManager = new ChunkTileVisibilityManager(settings, nms, playerChunkTracker, visibilityCache, chunkCache);

        getServer().getPluginManager().registerEvents(playerChunkTracker, this);
        getServer().getPluginManager().registerEvents(chunkCache, this);
        getServer().getPluginManager().registerEvents(visibilityCache, this);

        chunkPacketListener = new ChunkPacketListener(this, hiddenTileRegistry, nms, playerChunkTracker);
        ProtocolLibrary.getProtocolManager().addPacketListener(chunkPacketListener);

        visibilityUpdateTask = new VisibilityUpdateTask(chunkTileVisibilityManager);
        visibilityUpdateTask.start(this);
    }

    @Override
    public void onDisable() {
        ProtocolLibrary.getProtocolManager().removePacketListeners(this);
        Bukkit.getScheduler().cancelTasks(this);
    }

    public ChunkTileVisibilityManager getVisibilityManager() {
        return chunkTileVisibilityManager;
    }

    public VisibilityUpdateTask getVisibilityUpdateTask() {
        return visibilityUpdateTask;
    }

    public static boolean isOccluding(Material material) {
        switch (material) {
            case BARREL:
            case BARRIER:
            case SPAWNER:
            case SUSPICIOUS_GRAVEL:
            case SUSPICIOUS_SAND:
                return false;
        }
        return material.isOccluding();
    }
}
