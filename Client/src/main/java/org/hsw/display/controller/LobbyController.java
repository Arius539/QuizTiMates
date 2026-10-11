package org.hsw.display.controller;

import javafx.application.Platform;
import javafx.collections.*;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.hsw.application.adapter.MatchEventCoordinator;
import org.hsw.application.error.*;
import org.hsw.application.port.*;
import org.hsw.application.service.*;
import org.hsw.application.session.ClientContext;
import org.hsw.config.BusinessLimits;
import org.hsw.contract.*;
import org.hsw.contract.model.*;
import org.hsw.display.helper.ValidationPopupService;
import org.hsw.display.navigation.*;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class LobbyController implements IViewController {
    private final IPlayerCallbackEventRegister eventRegister;
    private final MatchEventCoordinator matchEventCoordinator;
    private final ViewNavigator viewNavigator;
    private final LobbyService lobbyService;
    private final ISessionClosedCallbackRegistry sessionclosedRegistry;
    private final ValidationPopupService validationPopupService;
    private final PlayerSetupService playerSetupService;

    private ClientContext clientContext;
    private ILobby lobby;
    private final ObservableList<String> playerDataList;
    private UUID registerId;
    private final Map<Topic, CheckBox> topicCheckBoxes = new EnumMap<>(Topic.class);


    @FXML
    private ListView<String> players;
    @FXML
    private Label lobbyCode;
    @FXML
    private Label hostName;
    @FXML
    private Button startMatchButton;
    @FXML
    private VBox hostSettings;
    @FXML
    private Spinner<Integer> roundCount;
    @FXML
    private GridPane topicGrid;
    @FXML
    private Button showLastResultButton;
    @FXML
    private VBox lobbyContent;
    @FXML
    private Parent matchResultView;
    @FXML
    private MatchResultController matchResultViewController;

    public LobbyController(IPlayerCallbackEventRegister eventRegister, MatchEventCoordinator matchEventCoordinator, ViewNavigator viewNavigator, LobbyService lobbyService, ISessionClosedCallbackRegistry sessionclosedRegistry, ValidationPopupService validationPopupService, PlayerSetupService playerSetupService) {
        this.eventRegister = eventRegister;
        this.matchEventCoordinator = matchEventCoordinator;
        this.lobbyService = lobbyService;
        this.sessionclosedRegistry = sessionclosedRegistry;
        this.validationPopupService = validationPopupService;
        this.playerSetupService = playerSetupService;
        this.playerDataList = FXCollections.observableArrayList();
        this.viewNavigator = viewNavigator;
    }

    @FXML
    private void initialize() {
        roundCount.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, BusinessLimits.MAX_ROUNDS, BusinessLimits.MAX_ROUNDS));
        topicCheckBoxes.clear();
        topicGrid.getChildren().clear();
        for (Topic topic : Topic.values()) {
            CheckBox checkBox = new CheckBox(topicLabel(topic));
            checkBox.setSelected(true);
            topicCheckBoxes.put(topic, checkBox);
            topicGrid.add(checkBox, topic.ordinal() % 2, topic.ordinal() / 2);
        }
    }

    private static String topicLabel(Topic topic) {
        return switch (topic) {
            case HISTORY -> "Geschichte";
            case ANIMALS -> "Tiere";
            case MATHEMATICS -> "Mathematik";
            case GEOGRAPHY -> "Geografie";
            case CHEMISTRY -> "Chemie";
            case COMPUTER_SCIENCE -> "Informatik";
            case ASTRONOMY -> "Astronomie";
            case BIOLOGY -> "Biologie";
            case ECONOMICS -> "Wirtschaft";
            case PHYSICS -> "Physik";
            case GENERAL_KNOWLEDGE -> "Allgemeinwissen";
            case EVERYDAY_MYTHS -> "Alltagsmythen";
            case FILMS_AND_SERIES -> "Filme & Serien";
            case MUSIC -> "Musik";
            case OLD_YOUTH_WORDS -> "Jugendwörter";
            case ABSURD_INVENTIONS -> "Erfindungen";
            case INTERNET_CULTURE -> "Netzkultur";
            case TECH_PITFALLS -> "Technik";
            case WORD_CURIOSITIES -> "Wortkurioses";
            case ADVERTISING_TRICKS -> "Kauftricks";
            case LOGIC_TRAPS -> "Logik";
        };
    }

    public void setSession(ClientContext clientContext, ILobby lobby) {
        this.clientContext = Objects.requireNonNull(clientContext, "clientContext");
        this.lobby = Objects.requireNonNull(lobby, "lobby");
    }

    @Override
    public void initilizeView() {
        showLobbyContent();
        lobbyService.setSession(clientContext, lobby);
        ClientContext subscribedContext = clientContext;
        ILobby subscribedLobby = lobby;
        Parent subscribedView = lobbyContent;
        eventRegister.subscribeQuestion(matchEventCoordinator::notifyQuestion);
        eventRegister.subscribeResultForRound(matchEventCoordinator::notifyResultForRound);
        players.setItems(playerDataList);
        eventRegister.subscribeMatchFinished((matchId, results) -> matchFinished(subscribedContext, subscribedLobby, subscribedView, matchId, results));
        eventRegister.subscribeLobbyStateChanged(lobbyId -> lobbyStateChanged(subscribedContext, subscribedLobby, subscribedView, lobbyId));
        if (registerId != null) {
            sessionclosedRegistry.unsubscribeSessionClosed(registerId);
        }
        registerId = sessionclosedRegistry.subscribeSessionClosed(result -> {
            if (clientContext == null || !result.sessionId().equals(clientContext.sessionId())) return;
            resetSession();
            resetMatchEvents();
            try {
                viewNavigator.loadPlayerSetup();
            } catch (ServiceException failure) {
                validationPopupService.showError(StandardErrorMessages.ERROR_OCCURRED, failure.kindMessage(), failure.userMessage());
            }
        });
        eventRegister.subscribeMatchStarted(matchId -> matchStarted(subscribedContext, subscribedLobby, subscribedView, matchId));
        updateView();
        try {
            lobbyService.getRunningMatch().ifPresent(match -> matchStarted(subscribedContext, subscribedLobby, subscribedView, match.getId()));
        } catch (ServiceException failure) {
            validationPopupService.showError(StandardErrorMessages.ERROR_OCCURRED, failure.kindMessage(), failure.userMessage());
        }
    }

    public void updateView() {
        try {
            boolean currentPlayerHost = lobbyService.isCurrentPlayerHost();
            String currentHostName = lobbyService.getHost().getName();
            String currentLobbyCode = lobbyService.getLobbyCode();
            List<String> currentPlayers = lobbyService.getLobbyPlayers();

            startMatchButton.setVisible(currentPlayerHost);
            startMatchButton.setManaged(currentPlayerHost);
            hostSettings.setVisible(currentPlayerHost);
            hostSettings.setManaged(currentPlayerHost);
            hostName.setText(currentHostName);
            lobbyCode.setText(currentLobbyCode);
            playerDataList.setAll(currentPlayers);
        } catch (ServiceException failure) {
            validationPopupService.showError(StandardErrorMessages.ERROR_OCCURRED, failure.kindMessage(), failure.userMessage());
        }
    }

    public void showMatchResult(UUID matchId, List<IPlayerMatchResult> results) {
        matchResultViewController.showResult(matchId, results, this::showLobbyContent);
        lobbyContent.setVisible(false);
        lobbyContent.setManaged(false);
        matchResultView.setVisible(true);
        matchResultView.setManaged(true);
        updateResultButton();
    }

    @FXML
    private void showLastResult() {
        matchEventCoordinator.getLatestFinishedMatchId().ifPresent(this::showCachedResult);
    }

    @FXML
    private void startMatch() {
        try {
            Set<Topic> selectedTopics = EnumSet.noneOf(Topic.class);
            topicCheckBoxes.forEach((topic, checkBox) -> {
                if (checkBox.isSelected()) selectedTopics.add(topic);
            });
            lobbyService.startMatch(roundCount.getValue(), selectedTopics);
        } catch (ServiceException failure) {
            validationPopupService.showError(StandardErrorMessages.ERROR_OCCURRED, failure.kindMessage(), failure.userMessage());
        }
    }

    @FXML
    private void leaveLobby() {
        if (clientContext == null) return;
        ClientContext departingContext = clientContext;
        try {
            lobbyService.leaveLobby();
        } catch (ServiceException failure) {
            validationPopupService.showError(StandardErrorMessages.ERROR_OCCURRED, failure.kindMessage(), failure.userMessage());
            return;
        }
        try {
            viewNavigator.loadHome(departingContext);
            resetSession();
            resetMatchEvents();
        } catch (ServiceException failure) {
            discardView();
            playerSetupService.closeSession(departingContext.home());
            try {
                viewNavigator.loadPlayerSetup();
            } catch (ServiceException navigationFailure) {
                validationPopupService.showError(StandardErrorMessages.ERROR_OCCURRED, navigationFailure.kindMessage(), navigationFailure.userMessage());
            }
            validationPopupService.showError(StandardErrorMessages.ERROR_OCCURRED, failure.kindMessage(), failure.userMessage());
        }
    }

    @Override
    public void discardView() {
        resetSession();
        resetMatchEvents();
    }

    private void lobbyStateChanged(ClientContext expectedContext, ILobby expectedLobby, Parent expectedView, UUID lobbyId) {
        Platform.runLater(() -> {
            if (!isCurrentLobby(expectedContext, expectedLobby, expectedView)) return;
            if (!lobbyId.equals(lobby.getId())) return;
            updateView();
        });

    }

    private void matchStarted(ClientContext expectedContext, ILobby expectedLobby, Parent expectedView, UUID matchId) {
        Platform.runLater(() -> {
            if (!isCurrentLobby(expectedContext, expectedLobby, expectedView)) return;
            if (matchEventCoordinator.isMatchFinished(matchId)) {
                showCachedResult(matchId);
                return;
            }
            try {
                IMatch runningMatch = lobbyService.getRunningMatch(matchId);
                viewNavigator.loadMatch(clientContext, runningMatch, lobby);
                resetSession();
            } catch (ServiceException failure) {
                if (matchEventCoordinator.isMatchFinished(matchId)) {
                    showCachedResult(matchId);
                    return;
                }
                validationPopupService.showError(StandardErrorMessages.ERROR_OCCURRED, failure.kindMessage(), failure.userMessage());
            }
        });

    }

    private void matchFinished(ClientContext expectedContext, ILobby expectedLobby, Parent expectedView, UUID matchId, List<IPlayerMatchResult> results) {
        boolean alreadyFinished = matchEventCoordinator.isMatchFinished(matchId);
        matchEventCoordinator.notifyMatchFinished(matchId, results);
        if (alreadyFinished) return;
        Platform.runLater(() -> {
            if (!isCurrentLobby(expectedContext, expectedLobby, expectedView)) return;
            showCachedResult(matchId);
        });
    }

    private void showCachedResult(UUID matchId) {
        matchEventCoordinator.getFinishedResults(matchId).ifPresent(results -> {
            matchEventCoordinator.clearMatch(matchId);
            if (!matchId.equals(matchResultViewController.getMatchId())) {
                showMatchResult(matchId, results);
            }
        });
    }

    private void showLobbyContent() {
        if (matchResultViewController != null) matchResultViewController.clearResult();
        if (matchResultView != null) {
            matchResultView.setVisible(false);
            matchResultView.setManaged(false);
        }
        if (lobbyContent != null) {
            lobbyContent.setVisible(true);
            lobbyContent.setManaged(true);
        }
        updateResultButton();
    }

    private void updateResultButton() {
        if (showLastResultButton != null) {
            showLastResultButton.setDisable(matchEventCoordinator.getLatestFinishedMatchId().isEmpty());
        }
    }

    private void resetSession() {
        eventRegister.unsubscribeLobbyStateChanged();
        eventRegister.unsubscribeMatchStarted();
        if (registerId != null) {
            sessionclosedRegistry.unsubscribeSessionClosed(registerId);
            registerId = null;
        }
        lobbyService.resetSession();
        lobby = null;
        clientContext = null;
        showLobbyContent();
        if (showLastResultButton != null) showLastResultButton.setDisable(true);
    }

    private void resetMatchEvents() {
        eventRegister.unsubscribeQuestion();
        eventRegister.unsubscribeResultForRound();
        eventRegister.unsubscribeMatchFinished();
        matchEventCoordinator.resetSession();
        updateResultButton();
    }

    private boolean isCurrentLobby(ClientContext expectedContext, ILobby expectedLobby, Parent expectedView) {
        return clientContext == expectedContext && lobby == expectedLobby && lobby != null && lobbyContent == expectedView;
    }
}
