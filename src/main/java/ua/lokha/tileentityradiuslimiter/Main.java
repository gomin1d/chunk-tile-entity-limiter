package ua.lokha.tileentityradiuslimiter;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;

/**
 * Главный класс плагина
 */
@Getter
public class Main extends JavaPlugin {

    @Getter
    private static Main instance;

    private Map<String, Integer> tileEntityLimitMap = new HashMap<>();
    private int minTileEntityLimit; // минимальный из лимитов, чтобы сразу отсеивать большую часть чанков
    private int commonLimit;

    private int periodTicks;
    private int checkWarnTimeMillis;

    private BukkitTask timer;

    public Main() {
        instance = this;
    }

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        this.reloadConfigParams();

        this.getCommand("tileentityradiuslimiter").setExecutor(new TileEntityRadiusLimiterCommandExecutor());
    }

    public void reloadConfigParams() {
        if (timer != null) {
            try {
                timer.cancel();
            } catch (Exception ignored) {}
            timer = null;
        }

        FileConfiguration config = this.getConfig();

        tileEntityLimitMap.clear();
        for (String type : config.getConfigurationSection("limits").getKeys(false)) {
            try {
                Number limitNearby = (Number) config.get("limits." + type + ".limit");
                tileEntityLimitMap.put(type, limitNearby.intValue());
                this.getLogger().info("Загрузили лимит: " + type + " " + limitNearby);
            } catch (Exception e) {
                this.getLogger().severe("Ошибка обработки limits." + type);
                e.printStackTrace();
            }
        }

        minTileEntityLimit = tileEntityLimitMap.values().stream()
                .mapToInt(Integer::intValue)
                .min()
                .orElse(Integer.MAX_VALUE);

        try {
            commonLimit = ((Number) config.get("common-limit.limit")).intValue();
        } catch (Exception e) {
            commonLimit = 256;
            this.getLogger().severe("Ошибка загрузки common-limit");
            e.printStackTrace();
        }

        periodTicks = this.getConfig().getInt("period-ticks", 200);
        checkWarnTimeMillis = this.getConfig().getInt("check-warn-time-millis", 5);

        timer = Bukkit.getScheduler().runTaskTimerAsynchronously(this, new ChunkChecker(), periodTicks, periodTicks);
    }
}
