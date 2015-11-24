package com.winthier.quiz;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;

class Question {
    final String question;
    final List<String> answers;
    final int correctAnswer;

    Question() {
        question = "Foo";
        answers = Arrays.asList("A", "B", "C", "D");
        correctAnswer = 0;
    }

    Question(ConfigurationSection config) {
        question = config.getString("Question");
        answers = config.getStringList("Answers");
        correctAnswer = config.getInt("Correct", 0);
    }

    Question(Question orig) {
        question = orig.question;
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < orig.answers.size(); ++i) indices.add(i);
        Collections.shuffle(indices, QuizPlugin.getInstance().getRandom());
        answers = new ArrayList<>(indices.size());
        int correctAnswer = 0;
        for (int i = 0; i < indices.size(); ++i) {
            int index = indices.get(i);
            answers.add(orig.answers.get(index));
            if (index == orig.correctAnswer) correctAnswer = i;
        }
        this.correctAnswer = correctAnswer;
    }

    Question shuffle() {
        return new Question(this);
    }

    static List<Question> loadQuestions() {
        final QuizPlugin plugin = QuizPlugin.getInstance();
        MemoryConfiguration config = new MemoryConfiguration();
        List<Question> result = new ArrayList<>();
        for (Map<?, ?> map: plugin.getConfig().getMapList(Config.QUESTION_SECTION.key)) {
            ConfigurationSection section = config.createSection("tmp", map);
            Question question = new Question(section);
            result.add(question);
        }
        Collections.shuffle(result, plugin.getRandom());
        return result;
    }

    String getAnswer(int i) {
        if (i >= answers.size()) return "";
        return answers.get(i);
    }

    String getCorrectAnswer() {
        return answers.get(correctAnswer);
    }
}
