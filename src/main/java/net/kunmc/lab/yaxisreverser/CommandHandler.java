package net.kunmc.lab.yaxisreverser;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class CommandHandler implements TabExecutor {
    private final Map<Long, Boolean> chunkKeyBooleanMap = new HashMap<>();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length < 1) {
            return false;
        }

        switch (args[0]) {
            case "run":
                run(sender, args);
                break;
            case "numberOfExecutions":
                numberOfExecutions(sender, args);
                break;
            default:
                sender.sendMessage(ChatColor.RED + "不明なコマンドです.");
        }
        return true;
    }

    private void run(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            return;
        }
        Player p = ((Player) sender);

        new BukkitRunnable() {
            @Override
            public void run() {
                int distance = 3;
                try {
                    distance = Integer.parseInt(args[1]);
                } catch (Exception ignored) {
                }

                List<Chunk> chunkList = getNearbyChunks(p.getLocation(), distance);
                AtomicInteger numberOfCompleted = new AtomicInteger(0);
                int latency = 0;
                for (int i = 0; i < chunkList.size(); i++) {
                    Chunk chunk = chunkList.get(i);
                    long chunkKey = chunk.getChunkKey();
                    if (chunkKeyBooleanMap.getOrDefault(chunkKey, false)) {
                        log(sender, String.format(ChatColor.GREEN + "%d/%d chunks completed", numberOfCompleted.addAndGet(1), chunkList.size()));
                        continue;
                    }
                    chunkKeyBooleanMap.put(chunkKey, true);

                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            for (int x = 0; x < 16; x++) {
                                for (int z = 0; z < 16; z++) {
                                    int height = getHighestBlockYAxis(chunk.getBlock(x, 0, z));
                                    for (int y = 0; y <= height / 2; y++) {
                                        int finalX = x;
                                        int finalY = y;
                                        int finalZ = z;

                                        YAxisReverser.taskScheduler.offer(new BukkitRunnable() {
                                            @Override
                                            public void run() {
                                                Block lower = chunk.getBlock(finalX, finalY, finalZ);
                                                BlockData lowerData = lower.getBlockData();
                                                if (lowerData.getMaterial().equals(Material.BEDROCK)) {
                                                    lowerData = Material.STONE.createBlockData();
                                                }

                                                Block higher = chunk.getBlock(finalX, height - finalY, finalZ);
                                                BlockData higherData = higher.getBlockData();

                                                lower.setBlockData(higherData, false);
                                                higher.setBlockData(lowerData, false);

                                                higher.getLocation().getNearbyEntities(1, 1, 1).forEach(e -> {
                                                    Location loc = e.getLocation();
                                                    loc.setY(finalY);
                                                    e.teleport(loc);
                                                });

                                                if (finalX == 15 && finalY == height / 2 && finalZ == 15) {
                                                    log(p, String.format(ChatColor.GREEN + "%d/%d chunks completed", numberOfCompleted.addAndGet(1), chunkList.size()));
                                                }
                                            }
                                        });
                                    }
                                }
                            }
                        }
                    }.runTaskLaterAsynchronously(YAxisReverser.instance, (long) (latency * 30.0 * (12500.0 / TaskScheduler.numberOfExecutionsPerSec)));
                    latency++;
                }
            }
        }.runTaskAsynchronously(YAxisReverser.instance);
    }

    private void numberOfExecutions(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.GREEN + "設定値: " + TaskScheduler.numberOfExecutionsPerSec);
            return;
        }

        try {
            TaskScheduler.numberOfExecutionsPerSec = Integer.parseInt(args[1]);
        } catch (Exception e) {
            sender.sendMessage(ChatColor.RED + args[1] + "は不正な値です.");
        }

        sender.sendMessage(ChatColor.GREEN + "値を" + args[1] + "に設定しました.");
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> completion = new ArrayList<>();

        if (args.length == 1) {
            completion.add("run");
            completion.add("numberOfExecutions");
        }

        if (args.length == 2) {
            switch (args[0]) {
                case "run":
                    completion.add("[distance]");
                    break;
                case "numberOfExecutions":
                    completion.add("[IntegerValue]");
                    break;
            }
        }

        return completion.stream().filter(x -> x.startsWith(args[args.length - 1])).collect(Collectors.toList());
    }

    private List<Chunk> getNearbyChunks(Location location, int chunkDistance) {
        List<Chunk> chunkList = new ArrayList<>();

        Chunk origin = location.getChunk();
        for (int x = -chunkDistance; x < chunkDistance; x++) {
            for (int z = -chunkDistance; z < chunkDistance; z++) {
                chunkList.add(location.getWorld().getChunkAt(origin.getX() + x, origin.getZ() + z));
            }
        }

        return chunkList;
    }

    private int getHighestBlockYAxis(Block b) {
        return b.getWorld().getHighestBlockYAt(b.getX(), b.getZ());
    }

    private void log(CommandSender sender, String msg) {
        sender.sendMessage(msg);
        Bukkit.getConsoleSender().sendMessage(msg);
    }
}