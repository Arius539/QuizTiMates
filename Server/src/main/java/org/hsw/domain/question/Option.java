package org.hsw.domain.question;

import lombok.NonNull;
import org.hsw.contract.model.IOption;

import java.util.List;

public record Option(char index, String text) implements IOption {

    public static List<Option> getOptionsForTrueOrFalse() {
        return List.of(new Option('A', "Richtig"), new Option('B', "Falsch"));
    }

    @Override
    @NonNull
    public String toString() {
        return String.valueOf(index);
    }
}
