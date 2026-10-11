package org.hsw.domain.match;

import lombok.Getter;
import lombok.Setter;
import org.hsw.contract.model.QuestionType;

public class Answer<T> {
    @Getter
    private final MatchParticipant participant;
    @Getter
    private final QuestionType questionType;
    @Getter
    private final T answer;
    @Setter
    @Getter
    private long speed;
    @Setter
    @Getter
    private boolean correct;

    public Answer(MatchParticipant participant, long speed, QuestionType questionType, T answer) {
        this.participant = participant;
        this.speed = speed;
        this.questionType = questionType;
        this.answer = answer;
        this.correct = false;
    }


}
