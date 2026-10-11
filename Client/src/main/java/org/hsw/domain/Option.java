package org.hsw.domain;

import org.hsw.contract.model.IOption;

public record Option(char index, String text) implements IOption {
}
