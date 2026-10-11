package org.hsw.domain.question;

import lombok.Getter;
import org.hsw.contract.model.QuestionType;
import org.hsw.contract.model.Topic;
import org.hsw.domain.match.Answer;
import org.hsw.domain.match.AnswerResult;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public abstract class Question<T> {

    @Getter
    protected String text;
    protected int points;
    protected T solution;
    @Getter
    protected QuestionType type;
    @Getter
    private final Topic topic;

    protected Question(final String text, final int points, final QuestionType type, T solution, final Topic topic) {
        this.text = text;
        this.points = points;
        this.type = type;
        this.solution = solution;
        this.topic = Objects.requireNonNull(topic, "topic");
    }

    public abstract AnswerResult<T> getResult(final Answer<T> answer, final long maxAnswerTimeMillis);

    public boolean checkIfCorrect(final Answer<T> answer) {
        if (!answer.getQuestionType().equals(this.type)) {
            throw new IllegalArgumentException("Question type mismatch");
        }
        if (answer.getAnswer().equals(this.solution)) {
            answer.setCorrect(true);
            return true;
        }
        return false;
    }

    /**
     * Berechnet die max. Punkte, die der Spieler mit seiner Antwortzeit erhalten kann.
     * Der Anteil nimmt alle 5s um 10 Prozentpunkte ab.
     *
     * @param answerTimeMillis    Die Zeit in ms, die der Spieler benötigt hat, um die Frage zu beantworten
     * @param maxAnswerTimeMillis max. Beantwortungszeit in ms
     * @return max. zu erhaltenen Punkte durch den Spieler
     */
    protected int calculateProportionalPoints(final long answerTimeMillis, final long maxAnswerTimeMillis) {
        if (answerTimeMillis >= maxAnswerTimeMillis) {
            return 0;
        }
        final long diff = maxAnswerTimeMillis - answerTimeMillis;
        final long dividendToAchieveFor100Percent = (maxAnswerTimeMillis - 5000) / 5000;
        final long actualDividend = diff / 5000;
        final double proportion = Math.max(0.0, Math.min(1.0, 1 - (0.1 * (dividendToAchieveFor100Percent - actualDividend))));
        return BigDecimal.valueOf(proportion).setScale(1, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(this.points)).intValue();
    }
}
