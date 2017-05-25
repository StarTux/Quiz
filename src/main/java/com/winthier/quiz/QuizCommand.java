package com.winthier.quiz;

import com.winthier.playercache.PlayerCache;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class QuizCommand implements CommandExecutor {
    private final QuizPlugin plugin;

    public QuizCommand(QuizPlugin plugin) {
        this.plugin = plugin;
    }

    public void enable() {
        plugin.getCommand("quiz").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Player expected");
            return true;
        }
        final Player player = (Player)sender;
        if (args.length == 0) {
            return false;
        } else if (args.length == 1 && args[0].equalsIgnoreCase("optout")) {
            plugin.getOptouts().add(player.getUniqueId());
            player.sendMessage(ChatColor.AQUA + "Opted out of the quiz.");
        } else if (args.length == 1 && args[0].equalsIgnoreCase("optin")) {
            plugin.getOptouts().remove(player.getUniqueId());
            player.sendMessage(ChatColor.AQUA + "Opted into the quiz.");
        } else if (args.length == 1 && (args[0].equalsIgnoreCase("hi") || args[0].equalsIgnoreCase("highscore"))) {
            Map<UUID, Integer> total = new HashMap<>();
            List<UUID> list = new ArrayList<>();
            for (Map.Entry<UUID, List<Integer>> entry: plugin.getHighscores().entrySet()) {
                total.put(entry.getKey(), entry.getValue().get(0) - entry.getValue().get(1));
                list.add(entry.getKey());
            }
            Collections.sort(list, (a, b) -> Integer.compare(total.get(b), total.get(a)));
            sender.sendMessage("" + ChatColor.DARK_AQUA + ChatColor.BOLD + "Quiz Highscore");
            for (int i = 0; i < 10; i += 1) {
                if (i >= list.size()) break;
                UUID uuid = list.get(i);
                int score = total.get(uuid);
                if (score <= 0) break;
                sender.sendMessage("" + ChatColor.AQUA + (i + 1) + ") " + ChatColor.YELLOW + score + " " + ChatColor.RESET + PlayerCache.nameForUuid(uuid));
            }
        } else if (args.length == 2) {
            UUID uuid;
            try {
                uuid = UUID.fromString(args[0]);
            } catch (IllegalArgumentException iae) {
                return false;
            }
            int answer;
            try {
                answer = Integer.parseInt(args[1]);
            } catch (NumberFormatException nfe) {
                return false;
            }
            Quiz quiz = plugin.getQuiz(uuid);
            if (quiz == null) return false;
            quiz.claim(player, answer);
        } else {
            return false;
        }
        return true;
    }
}
