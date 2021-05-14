package ua.lokha.tileentityradiuslimiter;

import org.apache.commons.lang3.StringUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class TileEntityRadiusLimiterCommandExecutor implements CommandExecutor, TabExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(
                    "§e=========[TileEntityRadiusLimiter]=========" +
                            "\n§4/tileentityradiuslimiter reload §7- перезагрузить конфиг"
            );
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            Main.getInstance().reloadConfig();
            Main.getInstance().reloadConfigParams();
            sender.sendMessage("§eКонфиг перезагружен.");
            return true;
        }

        sender.sendMessage("§cАргумент команды не найден.");
        return true;
    }

    @SuppressWarnings({"LambdaBodyCanBeCodeBlock"})
    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String label, String[] args) {
        if (args.length == 1) {
            return filterTabResponse(Collections.singletonList("reload"), args);
        }

        return Collections.emptyList();
    }

    private static List<String> filterTabResponse(List<String> list, String[] args) {
        return list.stream()
                .filter(el -> StringUtils.containsIgnoreCase(el, args[args.length - 1]))
                .collect(Collectors.toList());
    }
}
