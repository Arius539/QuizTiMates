package org.hsw.display.controller;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.hsw.application.adapter.MatchEventCoordinator;
import org.hsw.application.error.ServiceException;
import org.hsw.application.error.StandardErrorMessages;
import org.hsw.application.port.ISessionClosedCallbackRegistry;
import org.hsw.application.service.MatchService;
import org.hsw.application.session.ClientContext;
import org.hsw.config.BusinessLimits;
import org.hsw.contract.ILobby;
import org.hsw.contract.IMatch;
import org.hsw.contract.model.IAnswerResult;
import org.hsw.contract.model.IOption;
import org.hsw.contract.model.IPlayerMatchResult;
import org.hsw.display.helper.ValidationPopupService;
import org.hsw.display.navigation.IViewController;
import org.hsw.display.navigation.ViewNavigator;
import org.hsw.domain.ClientQuestion;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Component
public class MatchController implements IViewController {
    private final MatchService matchService;
    private final ValidationPopupService validationPopupService;
    private final MatchEventCoordinator matchEventCoordinator;
    private final ViewNavigator viewNavigator;
    private final ISessionClosedCallbackRegistry sessionClosedRegistry;
    private ClientContext clientContext;
    private ILobby lobby;
    private UUID matchId;
    private UUID sessionClosedRegistration;
    private boolean returnToLobbyPending;
    private int currentQuestionRound = -1;
    private MatchEventCoordinator.RoundResultSnapshot lastRoundResult;
    private Timeline timeline;
    private int secondsRemaining = BusinessLimits.ANSWER_TIME_SECONDS;

    @FXML
    private Label questionLabel;

    @FXML
    private Label lastAnswerCorrectLabel;

    @FXML
    private Label lastPointsLabel;

    @FXML
    private Label lastCorrectAnswerLabel;

    @FXML
    private VBox answerContainer;

    @FXML
    private Label currentRoundNumberLabel;

    @FXML
    private Label timerLabel;


    public MatchController(MatchService matchService, ValidationPopupService validationPopupService, MatchEventCoordinator matchEventCoordinator, ViewNavigator viewNavigator, ISessionClosedCallbackRegistry sessionClosedRegistry) {
        this.matchService = matchService;
        this.validationPopupService = validationPopupService;
        this.matchEventCoordinator = matchEventCoordinator;
        this.viewNavigator = viewNavigator;
        this.sessionClosedRegistry = sessionClosedRegistry;
    }


    public void setSession(ClientContext clientContext, IMatch match, ILobby lobby) {
        Objects.requireNonNull(clientContext, "clientContext");
        Objects.requireNonNull(match, "match");
        Objects.requireNonNull(lobby, "lobby");
        resetSession();
        this.clientContext = clientContext;
        matchService.setSession(clientContext, match);
        this.lobby = lobby;
    }

    @Override
    public void initilizeView() throws ServiceException {
        try {
            answerContainer.setDisable(false);
            matchId = matchService.getMatchId();
            UUID expectedMatchId = matchId;
            sessionClosedRegistration = sessionClosedRegistry.subscribeSessionClosed(result -> {
                if (clientContext == null || !result.sessionId().equals(clientContext.sessionId())) return;
                resetSession();
                matchEventCoordinator.resetSession();
                try {
                    viewNavigator.loadPlayerSetup();
                } catch (ServiceException failure) {
                    validationPopupService.showError(StandardErrorMessages.ERROR_OCCURRED, failure.kindMessage(), failure.userMessage());
                }
            });
            matchEventCoordinator.subscribeQuestion(expectedMatchId, question -> receiveQuestion(expectedMatchId, question));
            matchEventCoordinator.subscribeResultForRound(expectedMatchId, result -> showLastRoundResult(expectedMatchId, result));
            matchEventCoordinator.subscribeMatchFinished(expectedMatchId, results -> matchFinished(expectedMatchId, results));
        } catch (ServiceException failure) {
            resetSession();
            throw failure;
        }
    }


    public void showQuestion(ClientQuestion question) {
        currentQuestionRound = question.roundNumber();
        if (lastRoundResult == null || lastRoundResult.roundNumber() != question.roundNumber() - 1) {
            lastRoundResult = null;
            clearLastRoundResult();
        }
        questionLabel.setText(question.questionText());

        answerContainer.getChildren().clear();

        switch (question.questionType()) {
            case SINGLE_CHOICE -> setupSingleChoice();
            case TRUE_OR_FALSE -> setupTrueOrFalse();
            case ESTIMATION -> setupEstimation();
            case NUMERIC -> setupNumeric();
        }
        startCountdown();
    }

    @FXML
    private void leaveMatch() {
        try {
            if (!returnToLobbyPending) {
                matchService.leaveMatch();
                returnToLobbyPending = true;
                stopMatchUpdates();
            }
            returnToLobby();
        } catch (ServiceException failure) {
            validationPopupService.showError(StandardErrorMessages.ERROR_OCCURRED, failure.kindMessage(), failure.userMessage());
        }
    }

    private void receiveQuestion(UUID expectedMatchId, ClientQuestion question) {
        Platform.runLater(() -> {
            if (!isCurrentMatch(expectedMatchId)) return;
            try {
                matchService.setCurrentQuestion(question);
                showQuestion(question);
                currentRoundNumberLabel.setText(String.valueOf(question.roundNumber() + 1));
            } catch (ServiceException failure) {
                validationPopupService.showError(StandardErrorMessages.ERROR_OCCURRED, failure.kindMessage(), failure.userMessage());
            }
        });
    }

    private void showLastRoundResult(UUID expectedMatchId, MatchEventCoordinator.RoundResultSnapshot snapshot) {
        Platform.runLater(() -> {
            if (!isCurrentMatch(expectedMatchId)) return;
            if (currentQuestionRound >= 0 && (snapshot.roundNumber() < currentQuestionRound - 1 || snapshot.roundNumber() > currentQuestionRound))
                return;
            if (lastRoundResult != null && snapshot.roundNumber() < lastRoundResult.roundNumber()) return;
            lastRoundResult = snapshot;
            IAnswerResult<?> result = snapshot.result();
            lastAnswerCorrectLabel.setText(result.correct() ? "Ja" : "Nein");
            lastPointsLabel.setText(String.valueOf(result.points()));
            lastCorrectAnswerLabel.setText(String.valueOf(result.correctAnswer()));
        });
    }

    private void matchFinished(UUID expectedMatchId, List<IPlayerMatchResult> results) {
        Platform.runLater(() -> {
            if (!isCurrentMatch(expectedMatchId)) return;
            returnToLobbyPending = true;
            stopMatchUpdates();
            try {
                viewNavigator.loadMatchResult(clientContext, lobby, expectedMatchId, results);
                resetSession();
            } catch (ServiceException failure) {
                validationPopupService.showError(StandardErrorMessages.ERROR_OCCURRED, failure.kindMessage(), failure.userMessage());
            }
        });
    }

    private void startCountdown() {
        if (timeline != null) {
            timeline.stop();
        }
        secondsRemaining = BusinessLimits.ANSWER_TIME_SECONDS;
        timerLabel.setText(String.valueOf(secondsRemaining));

        timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            secondsRemaining--;
            if (secondsRemaining > 0) {
                timerLabel.setText(String.valueOf(secondsRemaining));
            } else {
                timerLabel.setText("Zeit abgelaufen!");
                timeline.stop();
            }
        }));
        timeline.setCycleCount(secondsRemaining);
        timeline.play();
    }

    private void setupSingleChoice() {
        List<IOption> options = matchService.getAnswerOptions();
        List<String> optionsText = options.stream().map(IOption::text).toList();

        ComboBox<String> answerComboBox = new ComboBox<>();

        answerComboBox.getItems().addAll(optionsText);

        Button answerButton = new Button("Antworten");

        answerButton.setOnAction(event -> {
            try {
                matchService.submitOptionAnswer(answerComboBox.getSelectionModel().getSelectedIndex());
                answerButton.setDisable(true);
            } catch (ServiceException failure) {
                validationPopupService.showError(StandardErrorMessages.ERROR_OCCURRED, failure.kindMessage(), failure.userMessage());
            }
        });

        answerContainer.getChildren().addAll(answerComboBox, answerButton);
    }

    private void setupTrueOrFalse() {
        List<IOption> options = matchService.getAnswerOptions();

        Button trueButton = new Button(options.get(0).text());
        Button falseButton = new Button(options.get(1).text());

        trueButton.setOnAction(event -> {
            try {
                matchService.submitOptionAnswer(0);
                trueButton.setDisable(true);
                falseButton.setDisable(true);
            } catch (ServiceException failure) {
                validationPopupService.showError(StandardErrorMessages.ERROR_OCCURRED, failure.kindMessage(), failure.userMessage());
            }
        });
        falseButton.setOnAction(event -> {
            try {
                matchService.submitOptionAnswer(1);
                falseButton.setDisable(true);
                trueButton.setDisable(true);
            } catch (ServiceException failure) {
                validationPopupService.showError(StandardErrorMessages.ERROR_OCCURRED, failure.kindMessage(), failure.userMessage());
            }
        });

        answerContainer.getChildren().addAll(trueButton, falseButton);
    }

    private void setupEstimation() {
        TextField answerField = new TextField();
        answerField.setPromptText("Gib deinen Schätzwert ein:");

        Button answerButton = new Button("Antworten");
        answerButton.setDisable(true);

        answerField.textProperty().addListener((observable, oldValue, newValue) -> {
            try {
                boolean valid = matchService.isNumeric(newValue);
                answerButton.setDisable(!valid);
            } catch (ServiceException failure) {
                validationPopupService.showError(StandardErrorMessages.ERROR_OCCURRED, failure.kindMessage(), failure.userMessage());
            }
        });

        answerButton.setOnAction(event -> {
            try {
                matchService.submitNumericAnswer(answerField.getText());
                answerButton.setDisable(true);
                answerField.setDisable(true);
            } catch (ServiceException failure) {
                validationPopupService.showError(StandardErrorMessages.ERROR_OCCURRED, failure.kindMessage(), failure.userMessage());
            }
        });
        answerContainer.getChildren().addAll(answerField, answerButton);
    }

    private void setupNumeric() {
        TextField answerField = new TextField();
        answerField.setPromptText("Gib eine Nummer ein:");

        Button answerButton = new Button("Antworten");
        answerButton.setDisable(true);

        answerField.textProperty().addListener((observable, oldValue, newValue) -> {
            try {
                boolean valid = matchService.isNumeric(newValue);
                answerButton.setDisable(!valid);
            } catch (ServiceException failure) {
                validationPopupService.showError(StandardErrorMessages.ERROR_OCCURRED, failure.kindMessage(), failure.userMessage());
            }
        });

        answerButton.setOnAction(event -> {
            try {
                matchService.submitNumericAnswer(answerField.getText());
                answerButton.setDisable(true);
                answerField.setDisable(true);
            } catch (ServiceException failure) {
                validationPopupService.showError(StandardErrorMessages.ERROR_OCCURRED, failure.kindMessage(), failure.userMessage());
            }
        });
        answerContainer.getChildren().addAll(answerField, answerButton);
    }

    private void clearLastRoundResult() {
        lastAnswerCorrectLabel.setText("–");
        lastPointsLabel.setText("–");
        lastCorrectAnswerLabel.setText("–");
    }

    private void returnToLobby() {
        viewNavigator.loadLobby(clientContext, lobby);
        if (matchId == null && returnToLobbyPending) {
            resetSession();
        }
    }

    private void stopMatchUpdates() {
        if (timeline != null) {
            timeline.stop();
            timeline = null;
        }
        if (matchId != null) {
            matchEventCoordinator.clearMatch(matchId);
            matchId = null;
        }
        if (answerContainer != null) {
            answerContainer.setDisable(true);
        }
        matchService.resetSession();
        currentQuestionRound = -1;
        lastRoundResult = null;
        if (lastAnswerCorrectLabel != null) clearLastRoundResult();
    }

    private void resetSession() {
        stopMatchUpdates();
        if (sessionClosedRegistration != null) {
            sessionClosedRegistry.unsubscribeSessionClosed(sessionClosedRegistration);
            sessionClosedRegistration = null;
        }
        clientContext = null;
        lobby = null;
        returnToLobbyPending = false;
    }

    private boolean isCurrentMatch(UUID expectedMatchId) {
        return matchId != null && matchId.equals(expectedMatchId) && clientContext != null && !returnToLobbyPending;
    }
}
