package org.hsw.domain;

import org.hsw.contract.model.IOption;
import org.hsw.contract.model.QuestionType;

import java.util.List;
import java.util.Optional;

public record ClientQuestion(String questionText, QuestionType questionType, Optional<List<IOption>> options,
                             int roundNumber) {
}
