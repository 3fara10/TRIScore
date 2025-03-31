package org.example.frontend;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

import org.example.model.Referee;
import org.example.service.IAuthentificationService;
import org.example.service.IObserver;
import org.example.service.IParticipantService;
import org.example.service.IResultService;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public class LoginFormController implements IObserver {
    private static final Logger logger = LogManager.getLogger(LoginFormController.class);

    @FXML private TextField textFieldUsername;
    @FXML private PasswordField passwordFieldPassword;
    @FXML private Button buttonLogin;
    @FXML private AnchorPane rootPane;

    private Stage stage;
    private IAuthentificationService authenticationService;
    private IParticipantService participantService;
    private IResultService resultService;
    private Referee currentReferee;

    public void initialize(Stage stage, IAuthentificationService authenticationService,
                           IParticipantService participantService, IResultService resultService) {
        logger.info("Initializing LoginFormController");
        this.stage = stage;
        this.authenticationService = authenticationService;
        this.participantService = participantService;
        this.resultService = resultService;

        if (this.authenticationService == null) {
            logger.error("Authentication service is null during initialization");
        } else {
            logger.debug("Authentication service initialized properly: {}", this.authenticationService.getClass().getName());
        }

        if (this.participantService == null) {
            logger.error("Participant service is null during initialization");
        } else {
            logger.debug("Participant service initialized properly: {}", this.participantService.getClass().getName());
        }

        if (this.resultService == null) {
            logger.error("Result service is null during initialization");
        } else {
            logger.debug("Result service initialized properly: {}", this.resultService.getClass().getName());
        }

        logger.debug("LoginFormController initialized successfully");
    }

    @FXML
    private void handleLoginButtonAction(ActionEvent event) {
        logger.info("Login button clicked");

        String username = textFieldUsername.getText().trim();
        String password = passwordFieldPassword.getText();

        if (username.isEmpty() || password.isEmpty()) {
            logger.warn("Login attempt with empty username or password");
            showAlert(Alert.AlertType.ERROR, "Login Error",
                    "Please enter both username and password.");
            return;
        }

        logger.debug("Attempting login with username: {}", username);

        logger.debug("Authentication service reference check before login: {}",
                (authenticationService == null ? "null" : "not null - " + authenticationService.getClass().getName()));

        if (authenticationService == null) {
            logger.error("Authentication service is null");
            showAlert(Alert.AlertType.ERROR, "System Error",
                    "Authentication service not available. Please contact administrator.");
            return;
        }

        try {
            buttonLogin.setDisable(true);
            textFieldUsername.setDisable(true);
            passwordFieldPassword.setDisable(true);
            rootPane.setCursor(javafx.scene.Cursor.WAIT);

            logger.debug("Calling authentication service loginAsync method");

            CompletableFuture<Referee> loginFuture = authenticationService.loginAsync(username, password, this);
            logger.debug("Called loginAsync with observer");

            loginFuture.thenAccept(referee -> {
                if (referee != null) {
                    logger.info("Login successful for user: {}", username);
                    currentReferee = referee;
                    Platform.runLater(() -> openMainForm());
                } else {
                    logger.warn("Login failed - invalid credentials for user: {}", username);
                    Platform.runLater(() -> showAlert(
                            Alert.AlertType.ERROR,
                            "Login Error",
                            "Invalid username or password."
                    ));
                }
            }).exceptionally(ex -> {
                logger.error("Login error", ex);
                Platform.runLater(() -> showAlert(
                        Alert.AlertType.ERROR,
                        "Error",
                        "Login failed: " + ex.getMessage()
                ));
                return null;
            }).whenComplete((result, ex) -> {
                logger.debug("Login process completed");
                Platform.runLater(() -> {
                    buttonLogin.setDisable(false);
                    textFieldUsername.setDisable(false);
                    passwordFieldPassword.setDisable(false);
                    rootPane.setCursor(javafx.scene.Cursor.DEFAULT);
                });
            });

        } catch (Exception ex) {
            logger.error("Unexpected error during login", ex);
            showAlert(Alert.AlertType.ERROR, "Error", "Login failed: " + ex.getMessage());
            buttonLogin.setDisable(false);
            textFieldUsername.setDisable(false);
            passwordFieldPassword.setDisable(false);
            rootPane.setCursor(javafx.scene.Cursor.DEFAULT);
        }
    }

    private void openMainForm() {
        logger.info("Opening main form");
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("MainForm.fxml"));
            Parent mainFormRoot = loader.load();

            MainFormController controller = loader.getController();
            controller.initialize(authenticationService, participantService, resultService, currentReferee);
            logger.debug("Main form controller initialized");

            Stage mainStage = new Stage();
            mainStage.setTitle("Main - " + currentReferee.getName());
            mainStage.setScene(new Scene(mainFormRoot, 1349, 1137));

            stage.hide();
            logger.debug("Login form hidden");

            mainStage.show();
            logger.info("Main form displayed");

            mainStage.setOnCloseRequest(e -> {
                logger.info("Main form closing by window close request");
                textFieldUsername.clear();
                passwordFieldPassword.clear();
                stage.show();
            });

        } catch (IOException e) {
            logger.error("Error opening main form", e);
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to open main form: " + e.getMessage());
        }
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        logger.debug("Showing alert: {} - {}", title, message);
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @Override
    public void update() {
        logger.debug("Observer update method called in LoginFormController");
    }

    public void reset() {
        logger.debug("Resetting login form");
        textFieldUsername.clear();
        passwordFieldPassword.clear();
        buttonLogin.setDisable(false);
        textFieldUsername.setDisable(false);
        passwordFieldPassword.setDisable(false);
        rootPane.setCursor(javafx.scene.Cursor.DEFAULT);
    }
}