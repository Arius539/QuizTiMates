package org.hsw.domain.question;

import org.hsw.contract.model.*;
import org.hsw.domain.match.*;

import java.math.*;

public class NumericQuestion extends Question<Double> {

    final int toDecimalExact;

    public NumericQuestion(final String text, final int points, final Double solution, final int toDecimalExact, final Topic topic) {
        super(text, points, QuestionType.NUMERIC, solution, topic);
        this.toDecimalExact = toDecimalExact;
    }

    @Override
    public AnswerResult<Double> getResult(final Answer<Double> answer, final long maxAnswerTimeMillis) {
        if (answer == null) {
            return new AnswerResult<>(false, 0, this.solution);
        }

        if (checkIfCorrect(answer)) {
            final int points = calculateProportionalPoints(answer.getSpeed(), maxAnswerTimeMillis);
            return new AnswerResult<>(true, points, this.solution);
        }
        return new AnswerResult<>(false, 0, this.solution);
    }

    @Override
    public boolean checkIfCorrect(final Answer<Double> answer) {
        if (!answer.getQuestionType().equals(this.type)) {
            throw new IllegalArgumentException("Question type mismatch");
        }

        final double rounded = BigDecimal.valueOf(this.solution).setScale(toDecimalExact, RoundingMode.HALF_UP).doubleValue();
        final double answerRounded = BigDecimal.valueOf((double) answer.getAnswer()).setScale(toDecimalExact, RoundingMode.HALF_UP).doubleValue();
        return answerRounded == rounded;
    }
}
