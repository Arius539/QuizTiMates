package org.hsw.application.service;

import org.hsw.application.error.RemoteRejectionMapper;
import org.hsw.application.error.ServiceErrorKind;
import org.hsw.application.error.ServiceException;
import org.hsw.application.error.StandardErrorMessages;
import org.hsw.application.session.ClientContext;
import org.hsw.contract.IMatch;
import org.hsw.contract.model.IOption;
import org.hsw.contract.model.QuestionType;
import org.hsw.domain.ClientQuestion;
import org.hsw.exceptions.RemoteActionException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Component
public class MatchService {
    private ClientContext clientContext;
    private IMatch match;
    private ClientQuestion currentQuestion;

    public void setSession(ClientContext clientContext, IMatch match) {
        this.clientContext = Objects.requireNonNull(clientContext, "clientContext");
        this.match = Objects.requireNonNull(match, "match");
        currentQuestion = null;
    }

    public void setCurrentQuestion(ClientQuestion question) {
        currentQuestion = question;
    }

    public void submitOptionAnswer(int selectedIndex) throws ServiceException {
        try {
            requireSession();
            List<IOption> options = getAnswerOptions();
            if (selectedIndex < 0 || selectedIndex >= options.size()) {
                throw new ServiceException(ServiceErrorKind.VALIDATION, "Bitte wähle eine gültige Antwort aus.");
            }
            String answerText = String.valueOf(options.get(selectedIndex).index());
            match.answer(clientContext.player(), answerText, currentQuestion.roundNumber());
        } catch (ServiceException failure) {
            throw failure;
        } catch (RemoteActionException rejection) {
            throw RemoteRejectionMapper.serviceExceptionFromRemote(rejection);
        } catch (RuntimeException | Error failure) {
            throw new ServiceException(ServiceErrorKind.SERVER_ERROR, StandardErrorMessages.TRY_AGAIN_LATER, failure);
        }
    }

    public void submitNumericAnswer(String value) throws ServiceException {
        try {
            requireSession();
            requireCurrentQuestion();
            if (!isNumeric(value)) {
                throw new ServiceException(ServiceErrorKind.VALIDATION, "Bitte gib eine endliche Zahl ein.");
            }
            String answerText = Double.toString(Double.parseDouble(value));
            match.answer(clientContext.player(), answerText, currentQuestion.roundNumber());
        } catch (ServiceException failure) {
            throw failure;
        } catch (RemoteActionException rejection) {
            throw RemoteRejectionMapper.serviceExceptionFromRemote(rejection);
        } catch (RuntimeException | Error failure) {
            throw new ServiceException(ServiceErrorKind.SERVER_ERROR, StandardErrorMessages.TRY_AGAIN_LATER, failure);
        }
    }

    public void leaveMatch() throws ServiceException {
        try {
            requireSession();
            match.leave(clientContext.player());
        } catch (ServiceException failure) {
            throw failure;
        } catch (RemoteActionException rejection) {
            throw RemoteRejectionMapper.serviceExceptionFromRemote(rejection);
        } catch (RuntimeException | Error failure) {
            throw new ServiceException(ServiceErrorKind.SERVER_ERROR, StandardErrorMessages.TRY_AGAIN_LATER, failure);
        }
    }

    public UUID getMatchId() throws ServiceException {
        try {
            requireSession();
            return match.getId();
        } catch (ServiceException failure) {
            throw failure;
        } catch (RemoteActionException rejection) {
            throw RemoteRejectionMapper.serviceExceptionFromRemote(rejection);
        } catch (RuntimeException | Error failure) {
            throw new ServiceException(ServiceErrorKind.SERVER_ERROR, StandardErrorMessages.TRY_AGAIN_LATER, failure);
        }
    }

    public List<IOption> getAnswerOptions() throws ServiceException {
        try {
            requireCurrentQuestion();
            if (currentQuestion.options().isEmpty()) {
                String message = currentQuestion.questionType() == QuestionType.TRUE_OR_FALSE ? "True-or-False Frage ohne Optionen" : "Single-Choice Frage ohne Options";
                throw new ServiceException(ServiceErrorKind.INVALID_STATE, message);
            }
            List<IOption> options = new ArrayList<>(currentQuestion.options().get());
            if (currentQuestion.questionType() == QuestionType.TRUE_OR_FALSE && options.size() != 2) {
                throw new ServiceException(ServiceErrorKind.INVALID_STATE, "True-or-False Frage hat eine unerwartete Optionenanzahl");
            }
            return options;
        } catch (ServiceException failure) {
            throw failure;
        } catch (RuntimeException | Error failure) {
            throw new ServiceException(ServiceErrorKind.SERVER_ERROR, StandardErrorMessages.TRY_AGAIN_LATER, failure);
        }
    }

    public boolean isNumeric(String value) throws ServiceException {
        if (value == null || value.isBlank()) {
            return false;
        }
        try {
            return Double.isFinite(Double.parseDouble(value));
        } catch (NumberFormatException exception) {
            return false;
        } catch (RuntimeException | Error failure) {
            throw new ServiceException(ServiceErrorKind.SERVER_ERROR, StandardErrorMessages.TRY_AGAIN_LATER, failure);
        }
    }

    public void resetSession() {
        currentQuestion = null;
        match = null;
        clientContext = null;
    }

    private void requireSession() throws ServiceException {
        if (clientContext == null) {
            throw new ServiceException(ServiceErrorKind.INVALID_STATE, StandardErrorMessages.SIGN_IN_AGAIN);
        }
        if (match == null) {
            throw new ServiceException(ServiceErrorKind.INVALID_STATE, "Es ist kein Match geladen.");
        }
    }

    private void requireCurrentQuestion() throws ServiceException {
        if (currentQuestion == null) {
            throw new ServiceException(ServiceErrorKind.INVALID_STATE, "Es liegt keine aktuelle Frage vor.");
        }
    }
}
