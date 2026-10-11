package org.hsw.contract.model;

import java.util.Objects;
import java.util.UUID;

public record PlayerInfo(UUID id, String name) implements IPlayerInfo {
    public PlayerInfo {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }
}
