package com.winthier.quiz;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.entity.Player;

class Prize {
    private final QuizPlugin plugin;
    private final int chance;
    private final String description;
    private final List<String> commands;

    Prize(QuizPlugin plugin, int chance, String description, List<String> commands) {
        this.plugin = plugin;
        this.chance = chance;
        this.description = description;
        this.commands = commands;
    }

    Prize(QuizPlugin plugin) {
        this(plugin, 1, "Nothing", new ArrayList<String>());
    }

    String getDescription() {
        return description;
    }

    void give(Player player) {
        for (String command : commands) {
            command = command.replace("{player}", player.getName());
            plugin.getLogger().info("Running console command: /" + command);
            plugin.consoleCommand(command);
        }
    }

    int getChance() {
        return chance;
    }

    static List<Prize> loadPrizes(QuizPlugin plugin) {
        List<Prize> result = new ArrayList<>();
        for (Map<?, ?> map : plugin.getConfig().getMapList(Config.PRIZE_SECTION.key)) {
            ConfigurationSection config = new MemoryConfiguration().createSection("tmp", map);
            final Prize prize = loadPrize(plugin, config);
            if (prize != null) result.add(prize);
        }
        return result;
    }

    static Prize loadPrize(QuizPlugin plugin, ConfigurationSection config) {
        int chance = config.getInt("Chance", 1);
        String description = config.getString("Description", "Something");
        List<String> commands = config.getStringList("Commands");
        return new Prize(plugin, chance, description, commands);
    }
}
