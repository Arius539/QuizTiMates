package org.hsw.application.session;

import org.hsw.contract.IHome;
import org.hsw.contract.model.IPlayerInfo;

import java.util.Objects;
import java.util.UUID;

public record ClientContext(UUID sessionId, IHome home, IPlayerInfo player) {
    public ClientContext {
        Objects.requireNonNull(sessionId, "sessionId");
        Objects.requireNonNull(home, "home");
        Objects.requireNonNull(player, "player");
    }
}
