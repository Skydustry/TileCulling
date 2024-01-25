package it.feargames.tileculling;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import org.bukkit.scheduler.BukkitScheduler;

public class VisibilityUpdateTask implements Runnable {

    public static final long TASK_INTERVAL = 2L;

    private final ChunkTileVisibilityManager chunkTileVisibilityManager;

    private boolean running = false;

    public VisibilityUpdateTask(ChunkTileVisibilityManager chunkTileVisibilityManager) {
        this.chunkTileVisibilityManager = chunkTileVisibilityManager;
    }

    public void start(CullingPlugin plugin) {
        BukkitScheduler scheduler = plugin.getServer().getScheduler();
        scheduler.runTaskTimerAsynchronously(plugin, this, 20L, TASK_INTERVAL);
    }

    @Override
    public void run() {
        if (running) {
            return;
        }

        running = true;

        for (Player player : Bukkit.getOnlinePlayers()) {
            chunkTileVisibilityManager.updateVisibility(player);
        }

        running = false;
    }
}
