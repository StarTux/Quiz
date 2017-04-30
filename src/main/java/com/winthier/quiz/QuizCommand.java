package com.winthier.quiz;

import java.util.UUID;
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
        if (args.length == 2) {
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
