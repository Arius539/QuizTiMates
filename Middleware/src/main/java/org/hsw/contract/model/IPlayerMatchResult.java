package org.hsw.contract.model;

import java.util.List;

public interface IPlayerMatchResult {
    IPlayerInfo getPlayer();

    List<Integer> getRoundPoints();

    int getTotalPoints();

    int getTotalAnswerTimeMillis();

    int getRank();
}
