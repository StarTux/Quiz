package com.winthier.quiz;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.json.simple.JSONValue;

public final class QuizPlugin extends JavaPlugin {
    @Getter private static QuizPlugin instance;
    private final Map<UUID, Quiz> quizzes = new HashMap<>();
    private Random random = null;
    private List<Prize> prizes;
    private List<Question> questions;
    private int questionIndex = 0;
    private BukkitRunnable task = null;

    @Override
    public void onEnable() {
        instance = this;
        reloadConfig();
        saveDefaultConfig();
        getCommand("quiz").setExecutor(new QuizCommand(this));
        getCommand("quizadmin").setExecutor(new QuizAdminCommand(this));
        task = new BukkitRunnable() {
            @Override public void run() {
                tick();
            }
        };
        task.runTaskTimer(this, 20L * 60L + 27L, 20L * 60L);
    }

    @Override
    public void onDisable() {
        task.cancel();
    }

    Random getRandom() {
        if (random == null) {
            random = new Random(System.currentTimeMillis());
        }
        return random;
    }

    Quiz getQuiz(UUID uuid) {
        return quizzes.get(uuid);
    }

    List<Quiz> getQuizzes() {
        return new ArrayList<Quiz>(quizzes.values());
    }

    void clearQuizzes() {
        quizzes.clear();
    }

    static void consoleCommand(String command) {
        Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), command);
    }

    int randomInt(int median, int variance) {
        if (variance == 0) return median;
        Random rnd = getRandom();
        return median
            + rnd.nextInt(variance)
            - rnd.nextInt(variance);
    }

    void reloadPrizes() {
        prizes = null;
    }

    void reloadQuestions() {
        questions = null;
        questionIndex = 0;
    }

    private void loadPrizes() {
        prizes = Prize.loadPrizes(this);
    }

    private void loadQuestions() {
        questions = Question.loadQuestions();
    }

    Prize dealPrize() {
        if (prizes == null) loadPrizes();
        if (prizes.isEmpty()) return new Prize(this);
        int total = 0;
        for (Prize prize : prizes) total += prize.getChance();
        int rnd = getRandom().nextInt(total);
        for (Prize prize : prizes) {
            rnd -= prize.getChance();
            if (rnd < 0) return prize;
        }
        return new Prize(this);
    }

    Question dealQuestion() {
        if (questions == null) loadQuestions();
        if (questions.isEmpty()) return new Question();
        if (questionIndex >= questions.size()) questionIndex = 0;
        Question result = questions.get(questionIndex).shuffle();
        questionIndex += 1;
        return result;
    }

    List<Prize> getPrizes() {
        if (prizes == null) loadPrizes();
        return prizes;
    }

    static String format(String msg, Object... args) {
        msg = ChatColor.translateAlternateColorCodes('&', msg);
        if (args.length > 0) msg = String.format(msg, args);
        return msg;
    }

    static void msg(CommandSender sender, String msg, Object... args) {
        sender.sendMessage(format(msg, args));
    }

    static void announce(String msg, Object... args) {
        msg = format(msg, args);
        for (Player player : Bukkit.getServer().getOnlinePlayers()) {
            player.sendMessage(msg);
        }
    }

    static void announceRaw(Object json) {
        String msg = JSONValue.toJSONString(json);
        for (Player player : Bukkit.getServer().getOnlinePlayers()) {
            consoleCommand("tellraw " + player.getName() + " " + msg);
        }
    }

    /**
     * Called once per minute.
     */
    void tick() {
        final int playerCount = getServer().getOnlinePlayers().size();
        final int minPlayers = getConfig().getInt(Config.INTERVAL_MIN_PLAYERS.key);
        if (playerCount < minPlayers) return;
        int unclaimedCount = 0;
        for (Quiz quiz : quizzes.values()) {
            quiz.tick();
            if (!quiz.isClaimed()) unclaimedCount++;
        }
        if (unclaimedCount == 0) {
            Quiz quiz = Quiz.createQuiz(this);
            quizzes.put(quiz.getUuid(), quiz);
        }
    }
}
