package org.hsw.contract.model;

public interface IAnswerResult<T> {
    boolean correct();

    int points();

    T correctAnswer();
}
