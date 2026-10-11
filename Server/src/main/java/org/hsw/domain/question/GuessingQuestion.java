package org.hsw.domain.question;

import org.hsw.contract.model.*;
import org.hsw.domain.match.*;

import java.math.*;
import java.util.Map;

public class GuessingQuestion extends Question<Double> {

    private final Map<Range, Double> toleranceRangeToPartialPoints;
    private final int toDecimalExact;

    public GuessingQuestion(final String text, final int points, final Double solution, final Map<Range, Double> toleranceRangeToPartialPoints, final int toDecimalExact, final Topic topic) {
        super(text, points, QuestionType.ESTIMATION, solution, topic);
        this.toleranceRangeToPartialPoints = toleranceRangeToPartialPoints;
        this.toDecimalExact = toDecimalExact;
    }

    @Override
    public AnswerResult<Double> getResult(final Answer<Double> answer, final long maxAnswerTimeMillis) {
        if (answer == null) {
            return new AnswerResult<>(false, 0, this.solution);
        }

        final int maxPoints = calculateProportionalPoints(answer.getSpeed(), maxAnswerTimeMillis);
        if (checkIfCorrect(answer)) {
            return new AnswerResult<>(true, maxPoints, this.solution);
        } else {
            final double answerDouble = answer.getAnswer();
            final double accuracy = this.solution == 0.0 ? 0.0 : Math.max(0.0, 1.0 - Math.abs(this.solution - answerDouble) / Math.abs(this.solution));
            final Range range = toleranceRangeToPartialPoints.keySet().stream().filter(r -> r.matches(accuracy)).findAny().orElseThrow(() -> new IllegalStateException("No existing range for accuracy " + accuracy));
            final int points = BigDecimal.valueOf(toleranceRangeToPartialPoints.get(range)).multiply(BigDecimal.valueOf(maxPoints)).setScale(0, RoundingMode.HALF_UP).intValueExact();
            return new AnswerResult<>(false, points, this.solution);
        }
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
