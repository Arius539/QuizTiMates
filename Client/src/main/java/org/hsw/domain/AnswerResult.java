package org.hsw.domain;

import org.hsw.contract.model.IAnswerResult;

public record AnswerResult(boolean correct, int points, String correctAnswer) implements IAnswerResult<String> {
}
