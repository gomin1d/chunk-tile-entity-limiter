package ua.lokha.tileentityradiuslimiter;

import lombok.SneakyThrows;
import net.minecraft.server.v1_12_R1.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_12_R1.CraftWorld;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class ChunkChecker implements Runnable {

    @Override
    public void run() {
        long start = System.currentTimeMillis();

        Map<String, Counter> typeCounters = new HashMap<>();
        Main.getInstance().getTileEntityLimitMap()
                .forEach((type, limit) -> typeCounters.put(type, new Counter(limit)));

        for (World world : Bukkit.getWorlds()) {
            try {
                WorldServer handle = ((CraftWorld) world).getHandle();
                List<Chunk> doRemove = null;

                for (Chunk chunk : handle.getChunkProviderServer().chunks.values()) {
                    try {
                        int size = chunk.tileEntities.size();
                        if (size > Main.getInstance().getCommonLimit()) {
                            Main.getInstance().getLogger().warning("Чанк world " + world.getName() + ", " +
                                    "x " + chunk.locX + ", z " + chunk.locZ + " (" +
                                    "/tp " + (chunk.locX << 4) + " 100 " + (chunk.locZ << 4) + ") " +
                                    "превысил лимит tile entity " + size + ">" + Main.getInstance().getCommonLimit() +
                                    ", удаляем его.");
                            if (doRemove == null) {
                                doRemove = new ArrayList<>();
                            }
                            doRemove.add(chunk);
                            break;
                        }

                        if (size > Main.getInstance().getMinTileEntityLimit()) { // если меньше минимального лимита, тогда нет смысла проверять
                            for (TileEntity tileEntity : chunk.tileEntities.values()) {
                                if (tileEntity == null) {
                                    continue;
                                }

                                String key = tileEntity.getMinecraftKeyString();
                                Counter counter = typeCounters.get(key);

                                if (counter != null && counter.counter++ >= counter.limit) {
                                    Main.getInstance().getLogger().warning("Чанк world " + world.getName() + ", " +
                                            "x " + chunk.locX + ", z " + chunk.locZ + " (" +
                                            "/tp " + (chunk.locX << 4) + " 100 " + (chunk.locZ << 4) + ") " +
                                            "превысил лимит tile entity типа " + key + " " + counter.counter + ">" + counter.limit +
                                            ", удаляем его.");
                                    if (doRemove == null) {
                                        doRemove = new ArrayList<>();
                                    }
                                    doRemove.add(chunk);
                                    break;
                                }
                            }
                        }
                    } catch (Exception e) {
                        Main.getInstance().getLogger().severe("Ошибка обработки чанка " +
                                chunk.locX + " " + chunk.locZ + " в мире " + world.getName());
                        e.printStackTrace();
                    } finally {
                        for (Counter counter : typeCounters.values()) {
                            counter.counter = 0;
                        }
                    }
                }

                if (doRemove != null) {
                    for (Chunk chunk : doRemove) {
                        try {
                            handle.getChunkProviderServer().unloadQueue.remove(chunk.chunkKey);
                            handle.getChunkProviderServer().chunks.remove(chunk.chunkKey);
                            RegionFile regionFile = RegionFileCache.a(world.getWorldFolder(), chunk.locX, chunk.locZ);
                            deleteChunk(regionFile, chunk.locX & 31, chunk.locZ & 31);
                        } catch (Exception e) {
                            Main.getInstance().getLogger().severe("Ошибка при удалении чанка " +
                                    chunk.locX + " " + chunk.locZ + " в мире " + world.getName());
                            e.printStackTrace();
                        }
                    }
                }
            } catch (Exception e) {
                Main.getInstance().getLogger().severe("Ошибка обработки мира " + world.getName());
                e.printStackTrace();
            }
        }
        long left = System.currentTimeMillis() - start;
        if (left > Main.getInstance().getCheckWarnTimeMillis()) {
            Main.getInstance().getLogger().warning("Проверка чанков заняла " + left + " ms " +
                    "(в конфиге можно настроить порог времени для лога этого сообщения).");
        }
    }

    public static class Counter {
        public int limit;
        public int counter;

        public Counter(int limit) {
            this.limit = limit;
        }
    }

    private static Method setOffset;
    private static Method setTimestamp;
    private static Field fileField;

    static {
        try {
            fileField = RegionFile.class.getDeclaredField("b");
            fileField.setAccessible(true);

            setOffset = RegionFile.class.getDeclaredMethod("a", int.class, int.class, int.class);
            setOffset.setAccessible(true);

            setTimestamp = RegionFile.class.getDeclaredMethod("b", int.class, int.class, int.class);
            setTimestamp.setAccessible(true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SneakyThrows
    public static void deleteChunk(RegionFile regionFile, int x, int z) {
        backup((File) fileField.get(regionFile));
        setOffset.invoke(regionFile, x, z, 0);
        setTimestamp.invoke(regionFile, x, z, 0);
    }

    private static void backup(File file) {
        DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        Date today = new Date();
        File corrupt = new File(file.getParentFile(), file.getName() + "." + formatter.format(today) + ".corrupt");
        if (!corrupt.exists()) {
            Logger logger = LogManager.getLogger();
            logger.error("Region file " + file.getAbsolutePath() + " was corrupt. Backing up to " + corrupt.getAbsolutePath() + " and repairing");

            try {
                Files.copy(file.toPath(), corrupt.toPath());
            } catch (IOException var7) {
                logger.error("Error backing up corrupt file" + file.getAbsolutePath(), var7);
            }

        }
    }
}
