package com.winthier.quiz;

import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class QuizAdminCommand implements CommandExecutor {
    private final QuizPlugin plugin;

    public QuizAdminCommand(QuizPlugin plugin) {
        this.plugin = plugin;
    }

    public void enable() {
        plugin.getCommand("quizzes").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            return false;
        } else if ("activate".equalsIgnoreCase(args[0]) && args.length == 1) {
            for (Quiz quiz : plugin.getQuizzes()) {
                quiz.activate();
            }
            QuizPlugin.msg(sender, "&eActivated all quizzes");
        } else if ("tick".equalsIgnoreCase(args[0]) && args.length == 1) {
            QuizPlugin.msg(sender, "&eTicking...");
            plugin.tick();
        } else if ("list".equalsIgnoreCase(args[0]) && args.length == 1) {
            plugin.msg(sender, "&eListing of quizzes:");
            for (Quiz quiz : plugin.getQuizzes()) {
                plugin.msg(sender, "&e %s (%d) %s", quiz.getState(), quiz.getTimer(), quiz.getPrize().getDescription());
            }
            QuizPlugin.msg(sender, "&e---");
        } else if ("clear".equalsIgnoreCase(args[0]) && args.length == 1) {
            plugin.clearQuizzes();
            QuizPlugin.msg(sender, "&eQuizzes cleared");
        } else if ("prize".equalsIgnoreCase(args[0]) && args.length >= 1) {
            if (args.length == 1) {
                plugin.msg(sender, "&eListing of prizes:");
                int i = 0;
                for (Prize prize : plugin.getPrizes()) {
                    plugin.msg(sender, "&e %s) %d%% %s", ++i, prize.getChance(), prize.getDescription());
                }
                QuizPlugin.msg(sender, "&e---");
            } else if (args.length == 2) {
                if (!(sender instanceof Player)) return false;
                int i;
                try {
                    i = Integer.parseInt(args[1]);
                } catch (NumberFormatException nfe) {
                    return false;
                }
                List<Prize> prizes = plugin.getPrizes();
                if (i < 0 || i > prizes.size()) return false;
                Prize prize = prizes.get(i - 1);
                plugin.msg(sender, "&eGiving prize #%d: %s", i, prize.getDescription());
                prize.give((Player)sender);
            } else {
                return false;
            }
        } else if ("reload".equalsIgnoreCase(args[0]) && args.length == 1) {
            plugin.reloadConfig();
            plugin.reloadPrizes();
            plugin.reloadQuestions();
            QuizPlugin.msg(sender, "&eConfiguration reloaded");
        } else {
            return false;
        }
        return true;
    }
}
