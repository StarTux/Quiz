package com.winthier.quiz;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

/**
 * A quiz is a prize which can be claimed by clicking the
 * displayed line in chat first.
 */
@Getter
final class Quiz {
    final QuizPlugin plugin;
    private final UUID uuid;
    private final Question question;
    private final Prize prize;
    // State
    private State state = State.WARMUP;
    private int timer;
    private int answerTimer;
    private final Set<UUID> playersAnswered = new HashSet<>();
    private final Set<UUID> winners = new HashSet<>();
    private UUID winner;

    /**
     * @arg delay Delay until activation, in seconds
     */
    private Quiz(QuizPlugin plugin, UUID uuid, Question question, Prize prize, int delay) {
        this.plugin = plugin;
        this.uuid = uuid;
        this.question = question;
        this.prize = prize;
        this.timer = delay;
    }

    static Quiz createQuiz(QuizPlugin plugin) {
        final UUID uuid = UUID.randomUUID();
        final Question question = plugin.dealQuestion();
        final Prize prize = plugin.dealPrize();
        final int seconds = plugin.getConfig().getInt(Config.INTERVAL_SECONDS.key);
        final int delay;
        if (seconds > 0) {
            delay = seconds;
        } else {
            final int median = plugin.getConfig().getInt(Config.INTERVAL_MEDIAN.key);
            final int variance = plugin.getConfig().getInt(Config.INTERVAL_VARIANCE.key);
            delay = plugin.randomInt(median * 60, variance * 60);
        }
        return new Quiz(plugin, uuid, question, prize, delay);
    }

    void tick() {
        final int newTimer = this.timer--;
        switch (state) {
        case WARMUP:
            if (newTimer <= 0) {
                activate();
            }
            break;
        case ACTIVE:
            answerTimer -= 1;
            if (answerTimer <= 0) {
                state = State.CLAIMED;
                pickWinner();
                if (!playersAnswered.isEmpty()) plugin.saveHighscores();
            }
            break;
        default:
            return;
        }
    }

    void claim(Player player, int answer) {
        switch (state) {
        case WARMUP:
            break;
        case ACTIVE:
            if (playersAnswered.contains(player.getUniqueId())) {
                QuizPlugin.msg(player, "&cYou already answered.");
            } else {
                playersAnswered.add(player.getUniqueId());
                QuizPlugin.msg(player, "&aYour answer is \"%s\". A winner will be picked shortly.", question.getAnswer(answer));
                if (answer == question.correctAnswer) {
                    winners.add(player.getUniqueId());
                    plugin.addHighscore(player, 1, 0);
                } else {
                    plugin.addHighscore(player, 0, 1);
                }
            }
            break;
        case CLAIMED:
            QuizPlugin.msg(player, "&cYou are too late. Better luck next time!");
            break;
        default:
            break;
        }
    }

    UUID getUuid() {
        return uuid;
    }

    Prize getPrize() {
        return prize;
    }

    void activate() {
        if (state == State.WARMUP) {
            state = State.ACTIVE;
            plugin.getLogger().info("Activate: " + question.question + " " + question.answers);
            answerTimer = plugin.getConfig().getInt(Config.INTERVAL_ANSWER_TIME.key);
            announce();
        }
    }

    boolean isActive() {
        return state == State.ACTIVE;
    }

    boolean isClaimed() {
        return state == State.CLAIMED;
    }

    void announce() {
        plugin.announce("");
        Map<String, Object> button = new HashMap<>();
        button.put("text", plugin.format("&3&lQuiz &8&oClick the right answer within %d seconds.", answerTimer));
        button.put("color", "dark_gray");
        Map<String, Object> clickEvent = new HashMap<>();
        clickEvent.put("action", "run_command");
        clickEvent.put("value", "/quiz");
        button.put("clickEvent", clickEvent);
        Map<String, Object> hoverEvent = new HashMap<>();
        hoverEvent.put("action", "show_text");
        hoverEvent.put("value", QuizPlugin.format("&a/quiz"));
        button.put("hoverEvent", hoverEvent);
        plugin.announceRaw(button);
        plugin.announce(" %s", question.question);
        List<Object> message = new ArrayList<>();
        message.add(" ");
        List<ChatColor> colors = Arrays.asList(ChatColor.AQUA,
                                               ChatColor.BLUE,
                                               ChatColor.GOLD,
                                               ChatColor.GREEN,
                                               ChatColor.LIGHT_PURPLE,
                                               ChatColor.YELLOW);
        Collections.shuffle(colors, plugin.getRandom());
        for (int i = 0; i < question.answers.size(); ++i) {
            if (i > 0) message.add("  ");
            button = new HashMap<>();
            message.add(button);
            String answer = question.answers.get(i);
            button.put("text", "[" + answer + "]");
            button.put("color", colors.get(i % colors.size()).name().toLowerCase());
            clickEvent = new HashMap<>();
            button.put("clickEvent", clickEvent);
            clickEvent.put("action", "run_command");
            clickEvent.put("value", "/quiz " + uuid + " " + i);
            hoverEvent = new HashMap<>();
            button.put("hoverEvent", hoverEvent);
            hoverEvent.put("action", "show_text");
            hoverEvent.put("value", QuizPlugin.format("&a%s\n&oAnswer", answer));
        }
        plugin.announceRaw(message);
        plugin.announce(" &8&oThe winner gets %s.", prize.getDescription());
        plugin.announce("");
    }

    void pickWinner() {
        Player player = null;
        List<UUID> winnerList = new ArrayList<>(winners);
        while (player == null && !winnerList.isEmpty()) {
            UUID winnerId = winnerList.remove(plugin.getRandom().nextInt(winnerList.size()));
            player = plugin.getServer().getPlayer(winnerId);
            if (player == null) plugin.getLogger().info("Winner left: " + winnerId);
        }
        if (player == null) {
            plugin.getLogger().info("No winners!");
            plugin.announce("&3&lQuiz &rNobody knew the correct answer: &a%s&r.", question.getCorrectAnswer());
        } else {
            this.winner = player.getUniqueId();
            plugin.getLogger().info(player.getName() + " wins " + prize.getDescription());
            plugin.announce("&3&lQuiz &r%s answered &a%s&r and wins &a%s&r.", player.getName(), question.getCorrectAnswer(), prize.getDescription());
            prize.give(player);
        }
    }

    enum State {
        WARMUP,
        ACTIVE,
        CLAIMED;
    }
}
