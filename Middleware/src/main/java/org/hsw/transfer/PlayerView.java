package org.hsw.transfer;

import java.util.Objects;
import java.util.UUID;

public record PlayerView(UUID id, String name) {
    public PlayerView {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
    }
}
