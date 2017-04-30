package com.winthier.quiz;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.entity.Player;

/**
 * A quiz is a prize which can be claimed by clicking the
 * displayed line in chat first.
 */
final class Quiz {
    final QuizPlugin plugin;
    private final UUID uuid;
    private final Question question;
    private final Prize prize;
    // State
    private State state = State.WARMUP;
    private int timer;
    private final Set<UUID> answeredWrong = new HashSet<>();

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
        final int delay = plugin.randomInt(median, variance);
        return new Quiz(plugin, uuid, question, prize, delay);
    }

    void tick() {
        final int newTimer = this.timer--;
        switch (state) {
        case WARMUP:
            if (newTimer <= 0) {
                state = State.ACTIVE;
                announce();
            }
            break;
        case ACTIVE:
            announce();
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
            if (answeredWrong.contains(player.getUniqueId())) {
                QuizPlugin.msg(player, "&cYou already answered.");
            } else if (answer != question.correctAnswer) {
                answeredWrong.add(player.getUniqueId());
                QuizPlugin.msg(player, "&c%s is not the right answer.", question.getAnswer(answer));
            } else {
                state = State.CLAIMED;
                QuizPlugin.announce("&3&lQuiz &r%s answered &a%s&r and wins &a%s&r.", player.getName(), question.getCorrectAnswer(), prize.getDescription());
                prize.give(player);
            }
            break;
        case CLAIMED:
            QuizPlugin.msg(player, "&cYou are too late. Better luck next time!");
            break;
        default:
            return;
        }
    }

    UUID getUuid() {
        return uuid;
    }

    Prize getPrize() {
        return prize;
    }

    void activate() {
        if (state == State.WARMUP) state = State.ACTIVE;
    }

    boolean isActive() {
        return state == State.ACTIVE;
    }

    boolean isClaimed() {
        return state == State.CLAIMED;
    }

    String getState() {
        return state.name();
    }

    int getTimer() {
        return timer;
    }

    void announce() {
        QuizPlugin.announce("");
        QuizPlugin.announce("&3&lQuiz &7&oBe first to click the right answer.");
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

    enum State {
        WARMUP,
        ACTIVE,
        CLAIMED;
    }
}
