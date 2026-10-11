package org.hsw.domain.match;

import org.hsw.contract.model.IAnswerResult;

public record AnswerResult<T>(boolean correct, int points, T correctAnswer) implements IAnswerResult<T> {
}
