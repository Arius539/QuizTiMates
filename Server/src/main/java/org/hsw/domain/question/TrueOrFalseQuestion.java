package org.hsw.domain.question;

import lombok.Getter;
import org.hsw.contract.model.*;
import org.hsw.domain.match.*;

import java.util.List;

public class TrueOrFalseQuestion extends Question<Option> {

    @Getter
    private final List<Option> options = Option.getOptionsForTrueOrFalse();

    public TrueOrFalseQuestion(final String text, final int points, Option solution, final Topic topic) {
        super(text, points, QuestionType.TRUE_OR_FALSE, solution, topic);
    }

    @Override
    public AnswerResult<Option> getResult(final Answer<Option> answer, final long maxAnswerTimeMillis) {
        if (answer == null) {
            return new AnswerResult<>(false, 0, this.solution);
        }


        if (checkIfCorrect(answer)) {
            final int points = calculateProportionalPoints(answer.getSpeed(), maxAnswerTimeMillis);
            return new AnswerResult<>(true, points, this.solution);
        }
        return new AnswerResult<>(false, 0, this.solution);
    }
}
