package it.feargames.tileculling;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.concurrent.atomic.AtomicBoolean;

public class VisibilityUpdateThread extends Thread {

    public static final int TASK_INTERVAL = 100;

    private final ChunkTileVisibilityManager chunkTileVisibilityManager;

    private final AtomicBoolean running = new AtomicBoolean(false);

    public VisibilityUpdateThread(ChunkTileVisibilityManager chunkTileVisibilityManager) {
        super("TileCulling-VisibilityUpdateThread");
        this.chunkTileVisibilityManager = chunkTileVisibilityManager;
    }

    @Override
    public synchronized void start() {
        running.set(true);
        super.start();
    }

    public void shutdown() {
        running.set(false);
        interrupt();
    }

    @Override
    public void run() {
        while (running.get()) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                chunkTileVisibilityManager.updateVisibility(player);
            }

            try {
                Thread.sleep(TASK_INTERVAL);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

}
