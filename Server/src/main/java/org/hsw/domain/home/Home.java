package org.hsw.domain.home;

import org.hsw.application.LobbyCodeGenerator;
import org.hsw.contract.IHome;
import org.hsw.contract.model.IPlayerInfo;
import org.hsw.contract.model.PlayerInfo;
import org.hsw.domain.exception.BusinessOperationException;
import org.hsw.domain.exception.BusinessStateException;
import org.hsw.domain.lobby.Lobby;
import org.hsw.domain.player.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class Home implements IHome {

    private static final Home instance = new Home();

    private final Map<String, Lobby> lobbiesByCode;
    private final Map<UUID, Lobby> lobbiesById;
    private final Map<UUID, ServerPlayer> players;

    private Home() {
        this.lobbiesByCode = new HashMap<>();
        this.lobbiesById = new HashMap<>();
        this.players = new ConcurrentHashMap<>();
    }

    public static Home getInstance() {
        return instance;
    }

    @Override
    public IPlayerInfo createPlayer(String name) {
        String confirmedName = name == null ? "" : name.trim();
        if (confirmedName.isBlank() || confirmedName.length() > 256) {
            throw new BusinessOperationException("Spielername muss 1 bis 256 Zeichen enthalten.");
        }
        return new PlayerInfo(UUID.randomUUID(), confirmedName);
    }

    @Override
    public Lobby createLobby(IPlayerInfo host) {
        ServerPlayer registeredHost = requireRegisteredPlayer(host);
        String code;
        do {
            code = LobbyCodeGenerator.generateRandomString();
        } while (lobbiesByCode.containsKey(code));
        Lobby lobby = new Lobby(code, registeredHost);
        lobbiesByCode.put(code, lobby);
        lobbiesById.put(lobby.getId(), lobby);
        return lobby;
    }

    @Override
    public boolean lobbyExists(String lobbyCode) {
        return lobbyCode != null && !lobbyCode.isBlank() && lobbiesByCode.containsKey(lobbyCode);
    }

    @Override
    public Lobby joinLobby(String code, IPlayerInfo player) {
        if (code == null || code.isBlank()) {
            throw new BusinessOperationException("Lobbycode darf nicht leer sein.");
        }
        Lobby lobby = findLobby(code).orElseThrow(() -> new BusinessOperationException("Lobby nicht gefunden."));
        lobby.join(requireRegisteredPlayer(player));
        return lobby;
    }

    public Optional<ServerPlayer> findPlayer(UUID playerId) {
        return Optional.ofNullable(players.get(playerId));
    }

    public Optional<Lobby> findLobby(String code) {
        return Optional.ofNullable(lobbiesByCode.get(code));
    }

    public Optional<Lobby> findLobby(UUID id) {
        return Optional.ofNullable(lobbiesById.get(id));
    }

    public void registerPlayer(ServerPlayer player) {
        if (players.putIfAbsent(player.getId(), player) != null) {
            throw new IllegalStateException("Spieler bereits angemeldet.");
        }
    }

    public void unregisterPlayer(UUID playerId) {
        players.remove(playerId);
    }

    public void unregisterPlayer(ServerPlayer player) {
        players.remove(player.getId(), player);
    }

    public void unregisterLobbyIfEmpty(Lobby lobby) {
        if (lobby.isEmpty()) {
            lobbiesByCode.remove(lobby.getCode(), lobby);
            lobbiesById.remove(lobby.getId(), lobby);
        }
    }

    private ServerPlayer requireRegisteredPlayer(IPlayerInfo player) {
        if (player == null) {
            throw new BusinessOperationException("Spieler fehlt.");
        }
        return findPlayer(player.getId()).orElseThrow(() -> new BusinessStateException("Spieler ist nicht angemeldet."));
    }
}
