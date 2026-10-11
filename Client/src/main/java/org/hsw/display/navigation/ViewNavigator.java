package org.hsw.display.navigation;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.hsw.application.error.ServiceErrorKind;
import org.hsw.application.error.ServiceException;
import org.hsw.application.session.ClientContext;
import org.hsw.contract.ILobby;
import org.hsw.contract.IMatch;
import org.hsw.contract.model.IPlayerMatchResult;
import org.hsw.display.Branding;
import org.hsw.display.controller.HomeController;
import org.hsw.display.controller.LobbyController;
import org.hsw.display.controller.MatchController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

@Component
public class ViewNavigator {

    private final ApplicationContext context;
    private Stage stage;
    private Scene scene;

    @Autowired
    public ViewNavigator(ApplicationContext context) {
        this.context = Objects.requireNonNull(context);
    }

    /**
     * Übernimmt einmalig die Hauptstage auf dem JavaFX Application Thread.
     */
    public void initialize(Stage stage) throws ServiceException {
        requireFxThread();
        if (stage == null) {
            throw new ServiceException(ServiceErrorKind.INVALID_STATE, "Hauptstage fehlt.");
        }
        if (this.stage != null) {
            throw new ServiceException(ServiceErrorKind.INVALID_STATE, "Hauptstage bereits initialisiert.");
        }
        this.stage = stage;
    }

    /**
     * Lädt eine FXML aus /fxml/ und ersetzt den Inhalt derselben Scene.
     * Laden und Controllerprüfung erfolgen vor dem Wechsel; bei einem Fehler
     * bleibt die bisherige Ansicht erhalten. Auf dem JavaFX Application Thread aufrufen.
     *
     * @return der Controller der neu geladenen Ansicht
     * @throws ServiceException wenn der Navigationszustand ungültig ist oder die Ansicht nicht geladen werden kann
     */
    public <T> T loadView(String fxml, String title, Class<T> controllerType) throws ServiceException {
        return loadView(fxml, title, controllerType, controller -> {
        });
    }

    public <T> T loadView(String fxml, String title, Class<T> controllerType, Consumer<T> prepareController) throws ServiceException {
        requireFxThread();
        if (stage == null) {
            throw new ServiceException(ServiceErrorKind.INVALID_STATE, "Hauptstage noch nicht initialisiert.");
        }
        FXMLLoader loader = null;
        try {
            Objects.requireNonNull(fxml, "fxml");
            Objects.requireNonNull(title, "title");
            Objects.requireNonNull(controllerType, "controllerType");
            Objects.requireNonNull(prepareController, "prepareController");
            URL resource = getClass().getResource("/fxml/" + fxml);
            if (resource == null) {
                throw new IOException("FXML nicht gefunden: /fxml/" + fxml);
            }

            loader = new FXMLLoader(resource);
            loader.setControllerFactory(context::getBean);
            Parent root = loader.load();
            Object controller = loader.getController();
            if (!controllerType.isInstance(controller)) {
                String actualType = controller == null ? "kein Controller" : controller.getClass().getName();
                throw new IllegalStateException("Controller für " + fxml + " hat nicht den erwarteten Typ " + controllerType.getName() + ", sondern " + actualType);
            }

            T typedController = controllerType.cast(controller);
            prepareController.accept(typedController);

            if (typedController instanceof IViewController viewController) {
                viewController.initilizeView();
            }

            if (scene == null) {
                scene = new Scene(root, 400, 400);
                stage.setScene(scene);
            } else {
                scene.setRoot(root);
            }
            stage.setTitle(Branding.APP_NAME + ": " + title);
            stage.setIconified(false);
            stage.show();
            stage.toFront();
            stage.requestFocus();
            return typedController;
        } catch (ServiceException exception) {
            discardFailedView(loader, exception);
            throw exception;
        } catch (IOException | RuntimeException exception) {
            discardFailedView(loader, exception);
            throw new ServiceException(ServiceErrorKind.VIEW_ERROR, ServiceErrorKind.VIEW_ERROR.defaultUserMessage(), exception);
        }
    }

    private static void discardFailedView(FXMLLoader loader, Exception failure) {
        if (loader == null || !(loader.getController() instanceof IViewController controller)) return;
        try {
            controller.discardView();
        } catch (RuntimeException cleanupFailure) {
            if (cleanupFailure != failure) failure.addSuppressed(cleanupFailure);
        }
    }

    public void loadPlayerSetup() throws ServiceException {
        loadView("playerSetup.fxml", "Player Setup", Object.class);
    }

    public void loadHome(ClientContext clientContext) throws ServiceException {
        loadView("home.fxml", "Home", HomeController.class, controller -> controller.setSession(clientContext));
    }

    public void loadLobby(ClientContext clientContext, ILobby lobby) throws ServiceException {
        loadView("lobby.fxml", "Lobby", LobbyController.class, controller -> controller.setSession(clientContext, lobby));
    }

    public void loadMatch(ClientContext clientContext, IMatch match, ILobby lobby) throws ServiceException {
        loadView("match.fxml", "Match", MatchController.class, controller -> controller.setSession(clientContext, match, lobby));
    }

    public void loadMatchResult(ClientContext clientContext, ILobby lobby, UUID matchId, List<IPlayerMatchResult> results) throws ServiceException {
        LobbyController controller = loadView("lobby.fxml", "Lobby", LobbyController.class, loaded -> loaded.setSession(clientContext, lobby));
        controller.showMatchResult(matchId, results);
    }

    private static void requireFxThread() {
        if (!Platform.isFxApplicationThread()) {
            throw new ServiceException(ServiceErrorKind.INVALID_STATE, "Navigation muss auf dem JavaFX Application Thread erfolgen.");
        }
    }
}
