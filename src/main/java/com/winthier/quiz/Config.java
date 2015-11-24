package com.winthier.quiz;

public enum Config {
    INTERVAL_MEDIAN("interval.Median"),
    INTERVAL_VARIANCE("interval.Variance"),
    INTERVAL_MIN_PLAYERS("interval.MinPlayers"),

    PRIZE_SECTION("prizes"),
    QUESTION_SECTION("questions"),
    ;
    public final String key;
    Config(String key) {
        this.key = key;
    }
}
