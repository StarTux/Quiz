package com.winthier.quiz;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;
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
        final int median = plugin.getConfig().getInt(Config.INTERVAL_MEDIAN.key);
        final int variance = plugin.getConfig().getInt(Config.INTERVAL_VARIANCE.key);
        final int delay = plugin.randomInt(median * 60, variance * 60);
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
                if (answer == question.correctAnswer) winners.add(player.getUniqueId());
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
        QuizPlugin.announce("");
        QuizPlugin.announce("&3&lQuiz &7&oClick the right answer within %d seconds.", answerTimer);
        QuizPlugin.announce(" &3Q &r%s", question.question);
        List<Object> message = new ArrayList<>();
        message.add(QuizPlugin.format(" &3A"));
        for (int i = 0; i < question.answers.size(); ++i) {
            message.add(" ");
            Map<String, Object> button = new HashMap<>();
            message.add(button);
            String answer = question.answers.get(i);
            button.put("text", QuizPlugin.format("&r[&a%s&r]", answer));
            button.put("color", "green");
            Map<String, Object> clickEvent = new HashMap<>();
            button.put("clickEvent", clickEvent);
            clickEvent.put("action", "run_command");
            clickEvent.put("value", "/quiz " + uuid + " " + i);
            Map<String, Object> hoverEvent = new HashMap<>();
            button.put("hoverEvent", hoverEvent);
            hoverEvent.put("action", "show_text");
            hoverEvent.put("value", QuizPlugin.format("&a%s\n&oAnswer", answer));
        }
        QuizPlugin.announceRaw(message);
        QuizPlugin.announce(" &7&oThe winner gets %s.", prize.getDescription());
        QuizPlugin.announce("");
    }

    void pickWinner() {
        if (winners.isEmpty()) {
            plugin.getLogger().info("No winners!");
        } else {
            UUID winnerId = new ArrayList<UUID>(winners).get(plugin.getRandom().nextInt(winners.size()));
            Player player = plugin.getServer().getPlayer(winnerId);
            if (player == null) {
                plugin.getLogger().info("Winner left: " + winnerId);
            } else {
                plugin.getLogger().info(player.getName() + " wins " + prize.getDescription());
                QuizPlugin.announce("&3&lQuiz &r%s answered &a%s&r and wins &a%s&r.", player.getName(), question.getCorrectAnswer(), prize.getDescription());
                prize.give(player);
            }
        }
    }

    enum State {
        WARMUP,
        ACTIVE,
        CLAIMED;
    }
}
