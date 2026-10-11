package org.hsw.contract;

import org.hsw.contract.model.IPlayerInfo;

public interface IHome {
    IPlayerInfo createPlayer(String name);

    ILobby createLobby(IPlayerInfo host);

    boolean lobbyExists(String lobbyCode);

    ILobby joinLobby(String code, IPlayerInfo player);
}
