package org.hsw.contract;

import org.hsw.contract.model.IPlayerInfo;

import java.util.UUID;

public interface IMatch {
    UUID getId();

    void answer(IPlayerInfo player, String answerText, int round);

    void leave(IPlayerInfo playerInfo);
}
