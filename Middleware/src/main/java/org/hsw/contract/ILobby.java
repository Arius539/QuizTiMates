package org.hsw.contract;

import org.hsw.contract.model.IPlayerInfo;
import org.hsw.contract.model.Topic;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface ILobby {
    UUID getId();

    String getCode();

    IPlayerInfo getHost();

    List<IPlayerInfo> getPlayers();

    boolean isMatchRunning();

    Optional<IMatch> getRunningMatch();

    void startMatch(IPlayerInfo player, int roundCount, Set<Topic> topics);

    void leave(IPlayerInfo player);
}
