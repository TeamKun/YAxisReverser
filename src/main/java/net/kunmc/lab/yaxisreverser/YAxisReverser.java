package net.kunmc.lab.yaxisreverser;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Levelled;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public final class YAxisReverser extends JavaPlugin implements Listener {
    public static YAxisReverser instance;
    public static TaskScheduler taskScheduler;
    private final String metadataKey = "enabledPhysics";

    @Override
    public void onEnable() {
        instance = this;
        taskScheduler = new TaskScheduler(this);

        getServer().getPluginCommand("yaxisreversal").setExecutor(new CommandHandler());
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockPlace(BlockPlaceEvent e) {
        e.getBlockPlaced().setMetadata(metadataKey, new FixedMetadataValue(this, null));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBucketEmpty(PlayerBucketEmptyEvent e) {
        e.getBlock().setMetadata(metadataKey, new FixedMetadataValue(this, null));
    }

    @EventHandler
    public void onBlockFromTo(BlockFromToEvent e) {
        Block b = e.getBlock();
        if (b.hasMetadata(metadataKey)) {
            return;
        }

        if (b.getType().equals(Material.WATER) || b.getType().equals(Material.LAVA)) {
            Levelled data = ((Levelled) b.getBlockData());
            if (data.getLevel() == 0) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onBlockPhysics(BlockPhysicsEvent e) {
        Block b = e.getBlock();
        if (b.hasMetadata(metadataKey)) {
            return;
        }

        Material type = b.getType();
        if (type.equals(Material.SAND) || type.equals(Material.GRAVEL)) {
            e.setCancelled(true);
            b.setType(Material.AIR, false);
            new BukkitRunnable() {
                @Override
                public void run() {
                    b.setType(type, false);
                }
            }.runTaskLater(this, 4);
        }
    }

    @EventHandler
    public void onBlockPhysics2(BlockPhysicsEvent e) {
        Block b = e.getSourceBlock();
        if (b.hasMetadata(metadataKey)) {
            return;
        }

        Material type = b.getType();
        if (type.equals(Material.SAND) || type.equals(Material.GRAVEL)) {
            e.setCancelled(true);
            b.setType(Material.AIR, false);
            new BukkitRunnable() {
                @Override
                public void run() {
                    b.setType(type, false);
                }
            }.runTaskLater(this, 4);
        }
    }
}
