package org.example.frontend;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import org.example.model.ParticipantResultDTO;
import org.example.model.Referee;
import org.example.service.*;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class MainFormController implements IObserver {
    private static final Logger logger = LogManager.getLogger(MainFormController.class);

    @FXML private TabPane tabControl;
    @FXML private Tab tabPageParticipants;
    @FXML private Tab tabPageResults;
    @FXML private TableView<ParticipantResultDTO> tableViewParticipants;
    @FXML private TableView<ParticipantResultDTO> tableViewResults;
    @FXML private Button buttonAddResult;
    @FXML private Button buttonLogout;
    @FXML private Spinner<Integer> spinnerPoints;
    @FXML private Label labelEvent;
    @FXML private Label labelReferee;

    private IAuthentificationService authenticationService;
    private IParticipantService participantService;
    private IResultService resultService;
    private Referee currentReferee;
    private boolean isInitialized = false;

    public void initialize(IAuthentificationService authenticationService,
                           IParticipantService participantService,
                           IResultService resultService,
                           Referee currentReferee) {
        logger.info("Initializing MainFormController");

        if (authenticationService == null || participantService == null ||
                resultService == null || currentReferee == null) {
            logger.error("One or more required services or the referee object is null");
            throw new IllegalArgumentException("Services and referee cannot be null");
        }

        this.authenticationService = authenticationService;
        this.participantService = participantService;
        this.resultService = resultService;
        this.currentReferee = currentReferee;

        logger.debug("Current referee: {}", currentReferee.getUsername());
        logger.debug("Authentication service: {}", authenticationService.getClass().getName());
        logger.debug("Participant service: {}", participantService.getClass().getName());
        logger.debug("Result service: {}", resultService.getClass().getName());

        try {
            if (participantService instanceof Service) {
                logger.debug("Registering observer for participant service");
                ((Service)participantService).registerObserver(this, currentReferee.getId());
            } else {
                logger.warn("participantService does not implement Service interface, observer not registered");
            }

            initializeSpinner();
            initializeTableViews();

            try {
                labelEvent.setText("Event: " + currentReferee.getEvent().getName());
                labelReferee.setText("Referee: " + currentReferee.getName());
            } catch (NullPointerException e) {
                logger.error("Error setting labels - event or referee data might be null", e);
                labelEvent.setText("Event: Unknown");
                labelReferee.setText("Referee: " + currentReferee.getName());
            }

            loadDataAsync();

            isInitialized = true;

            logger.info("MainFormController initialized successfully");
        } catch (Exception e) {
            logger.error("Error during MainFormController initialization", e);
            showAlert(Alert.AlertType.ERROR, "Initialization Error",
                    "Failed to initialize application: " + e.getMessage());
        }
    }

    private void initializeSpinner() {
        logger.debug("Initializing spinner");
        SpinnerValueFactory<Integer> valueFactory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 100, 0);
        spinnerPoints.setValueFactory(valueFactory);
    }

    private void initializeTableViews() {
        logger.debug("Initializing table views");

        try {

            TableColumn<ParticipantResultDTO, String> nameColumn = new TableColumn<>("Name");
            nameColumn.setCellValueFactory(new PropertyValueFactory<>("participantName"));
            nameColumn.setPrefWidth(200);

            TableColumn<ParticipantResultDTO, Integer> pointsColumn = new TableColumn<>("Points");
            pointsColumn.setCellValueFactory(new PropertyValueFactory<>("points"));
            pointsColumn.setPrefWidth(100);

            tableViewParticipants.getColumns().clear();
            tableViewParticipants.getColumns().addAll(nameColumn, pointsColumn);

            TableColumn<ParticipantResultDTO, String> resultNameColumn = new TableColumn<>("Name");
            resultNameColumn.setCellValueFactory(new PropertyValueFactory<>("participantName"));
            resultNameColumn.setPrefWidth(200);

            TableColumn<ParticipantResultDTO, Integer> resultPointsColumn = new TableColumn<>("Points");
            resultPointsColumn.setCellValueFactory(new PropertyValueFactory<>("points"));
            resultPointsColumn.setPrefWidth(100);

            tableViewResults.getColumns().clear();
            tableViewResults.getColumns().addAll(resultNameColumn, resultPointsColumn);

            logger.debug("Table views initialized successfully");
        } catch (Exception e) {
            logger.error("Error initializing table views", e);
            throw e;
        }
    }

    private void loadDataAsync() {
        logger.info("Loading data asynchronously");
        tabControl.setCursor(javafx.scene.Cursor.WAIT);

        CompletableFuture.allOf(
                loadParticipantsAsync(),
                loadResultsAsync()
        ).exceptionally(ex -> {
            logger.error("Error loading data", ex);
            Platform.runLater(() -> {
                showAlert(Alert.AlertType.ERROR, "Error", "Error initializing data: " + ex.getMessage());
                tabControl.setCursor(javafx.scene.Cursor.DEFAULT);
            });
            return null;
        }).thenRun(() -> {
            Platform.runLater(() -> tabControl.setCursor(javafx.scene.Cursor.DEFAULT));
            logger.info("Data loading completed");
        });
    }

    private CompletableFuture<Void> loadParticipantsAsync() {
        logger.debug("Loading participants data");
        try {
            if (participantService == null) {
                logger.error("Cannot load participants - participantService is null");
                return CompletableFuture.completedFuture(null);
            }

            return participantService.getAllParticipantsSortedByNameAsync()
                    .thenAccept(participants -> {
                        logger.debug("Participants data received, updating UI");
                        if (participants == null) {
                            logger.warn("Received null participants list");
                            Platform.runLater(() -> {
                                tableViewParticipants.setItems(FXCollections.observableArrayList());
                            });
                            return;
                        }
                        Platform.runLater(() -> {
                            if (tableViewParticipants == null) {
                                logger.error("TableViewParticipants is null, cannot update UI");
                                return;
                            }

                            ObservableList<ParticipantResultDTO> observableList;
                            if (participants instanceof ObservableList) {
                                observableList = (ObservableList<ParticipantResultDTO>) participants;
                            } else if (participants instanceof List) {
                                observableList = FXCollections.observableArrayList((List<ParticipantResultDTO>) participants);
                            } else {
                                logger.warn("Unexpected participants type: {}", participants.getClass().getName());
                                observableList = FXCollections.observableArrayList();
                            }
                            tableViewParticipants.setItems(observableList);
                            logger.info("Updated participants table with {} items", observableList.size());
                        });
                    })
                    .exceptionally(ex -> {
                        logger.error("Error loading participants", ex);
                        Platform.runLater(() -> {
                            showAlert(Alert.AlertType.ERROR, "Error", "Error loading participants: " + ex.getMessage());
                        });
                        return null;
                    });
        } catch (Exception ex) {
            logger.error("Error in loadParticipantsAsync", ex);
            Platform.runLater(() -> {
                showAlert(Alert.AlertType.ERROR, "Error", "Error loading participants: " + ex.getMessage());
            });
            return CompletableFuture.completedFuture(null);
        }
    }

    private CompletableFuture<Void> loadResultsAsync() {
        logger.debug("Loading results data");
        try {
            if (participantService == null) {
                logger.error("Cannot load results - participantService is null");
                return CompletableFuture.completedFuture(null);
            }

            if (currentReferee == null) {
                logger.error("Cannot load results - currentReferee is null");
                return CompletableFuture.completedFuture(null);
            }

            UUID eventId;
            try {
                eventId = currentReferee.getEvent().getId();
                logger.debug("Fetching results for event ID: {}", eventId);
            } catch (NullPointerException e) {
                logger.error("Error retrieving event ID", e);
                Platform.runLater(() -> {
                    showAlert(Alert.AlertType.ERROR, "Error", "Could not retrieve event information");
                });
                return CompletableFuture.completedFuture(null);
            }

            return participantService.getParticipantsWithResultsForEventAsync(eventId)
                    .thenAccept(participants -> {
                        logger.debug("Results data received, updating UI");

                        if (participants == null) {
                            logger.warn("Received null results list");
                            Platform.runLater(() -> {
                                tableViewResults.setItems(FXCollections.observableArrayList());
                            });
                            return;
                        }

                        Platform.runLater(() -> {
                            if (tableViewResults == null) {
                                logger.error("TableViewResults is null, cannot update UI");
                                return;
                            }

                            ObservableList<ParticipantResultDTO> observableList;
                            if (participants instanceof ObservableList) {
                                observableList = (ObservableList<ParticipantResultDTO>) participants;
                            } else if (participants instanceof List) {
                                observableList = FXCollections.observableArrayList((List<ParticipantResultDTO>) participants);
                            } else {
                                logger.warn("Unexpected results type: {}", participants.getClass().getName());
                                observableList = FXCollections.observableArrayList();
                            }
                            tableViewResults.setItems(observableList);
                            logger.info("Updated results table with {} items", observableList.size());
                        });
                    })
                    .exceptionally(ex -> {
                        logger.error("Error generating report", ex);
                        Platform.runLater(() -> {
                            showAlert(Alert.AlertType.ERROR, "Error", "Error in generating the report: " + ex.getMessage());
                        });
                        return null;
                    });
        } catch (Exception ex) {
            logger.error("Error in loadResultsAsync", ex);
            Platform.runLater(() -> {
                showAlert(Alert.AlertType.ERROR, "Error", "Error generating report: " + ex.getMessage());
            });
            return CompletableFuture.completedFuture(null);
        }
    }

    @FXML
    private void handleAddResultButtonAction(ActionEvent event) {
        logger.info("Add result button clicked");
        ParticipantResultDTO selectedParticipant = tableViewParticipants.getSelectionModel().getSelectedItem();
        if (selectedParticipant == null) {
            logger.warn("No participant selected for adding result");
            showAlert(Alert.AlertType.WARNING, "Add Result", "Please select a participant.");
            return;
        }

        try {
            logger.debug("Adding result for participant: {}", selectedParticipant.getParticipantName());
            buttonAddResult.setDisable(true);
            tabControl.setCursor(javafx.scene.Cursor.WAIT);

            UUID participantId = selectedParticipant.getParticipantID();
            UUID eventId = currentReferee.getEvent().getId();
            int points = spinnerPoints.getValue();

            logger.debug("Adding result: participant={}, event={}, points={}",
                    participantId, eventId, points);

            resultService.addResultAsync(participantId, eventId, points)
                    .thenCompose(v -> {
                        logger.debug("Result added, reloading data");
                        return CompletableFuture.allOf(
                                loadParticipantsAsync(),
                                loadResultsAsync()
                        );
                    })
                    .thenRun(() -> {
                        logger.info("Result added successfully");
                        Platform.runLater(() -> {
                            showAlert(Alert.AlertType.INFORMATION, "Success", "Result added successfully!");
                        });
                    })
                    .exceptionally(ex -> {
                        logger.error("Error adding result", ex);
                        Platform.runLater(() -> {
                            showAlert(Alert.AlertType.ERROR, "Error", "Error adding result: " + ex.getMessage());
                        });
                        return null;
                    });
        } catch (Exception ex) {
            logger.error("Error in handleAddResultButtonAction", ex);
            buttonAddResult.setDisable(false);
            tabControl.setCursor(javafx.scene.Cursor.DEFAULT);
            showAlert(Alert.AlertType.ERROR, "Error", "Error adding result: " + ex.getMessage());
        }
    }

    @FXML
    private void handleLogoutButtonAction(ActionEvent event) {
        logger.info("Logout button clicked");
        buttonLogout.setDisable(true);

        if (authenticationService == null) {
            logger.error("Authentication service is null during logout");
            Platform.runLater(() -> {
                buttonLogout.setDisable(false);
                showAlert(Alert.AlertType.ERROR, "Error", "Authentication service not available");
            });
            return;
        }

        logoutAsync().thenRun(() -> {
            logger.info("Logout complete, returning to login screen");
            Platform.runLater(() -> {
                try {
                    Stage stage = (Stage) buttonLogout.getScene().getWindow();

                    FXMLLoader loader = new FXMLLoader(getClass().getResource("LoginForm.fxml"));
                    Parent loginRoot = loader.load();
                    LoginFormController loginController = loader.getController();
                    logger.debug("Passing services to LoginFormController - authService is {}null",
                            (this.authenticationService == null ? "" : "not "));
                    logger.debug("Passing services to LoginFormController - participantService is {}null",
                            (this.participantService == null ? "" : "not "));
                    logger.debug("Passing services to LoginFormController - resultService is {}null",
                            (this.resultService == null ? "" : "not "));

                    loginController.initialize(stage,
                            this.authenticationService,
                            this.participantService,
                            this.resultService);

                    Scene loginScene = new Scene(loginRoot);
                    stage.setScene(loginScene);
                    stage.show();

                    logger.info("Main form closing, showing login form again");

                } catch (IOException ex) {
                    logger.error("Error loading login form", ex);
                    buttonLogout.setDisable(false);
                    showAlert(Alert.AlertType.ERROR, "Error", "Error returning to login: " + ex.getMessage());
                }
            });
        }).exceptionally(ex -> {
            logger.error("Error during logout", ex);
            Platform.runLater(() -> {
                buttonLogout.setDisable(false);
                showAlert(Alert.AlertType.ERROR, "Error", "Error during logout: " + ex.getMessage());
            });
            return null;
        });
    }

    private CompletableFuture<Void> logoutAsync() {
        try {
            if (currentReferee == null) {
                logger.error("Current referee is null during logout");
                return CompletableFuture.completedFuture(null);
            }

            UUID refereeId = currentReferee.getId();
            logger.debug("Logging out referee: {}", refereeId);

            if (authenticationService != null) {
                if (participantService instanceof Service) {
                    logger.debug("Unregistering observer before logout");
                    try {
                        ((Service)participantService).unregisterObserver(refereeId);
                    } catch (Exception e) {
                        logger.error("Error unregistering observer", e);
                    }
                }
                this.cleanup();
                return authenticationService.logoutAsync(refereeId);
            } else {
                logger.error("Authentication service is null during logout");
                return CompletableFuture.completedFuture(null);
            }
        } catch (Exception ex) {
            logger.error("Logout error", ex);
            return CompletableFuture.completedFuture(null);
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
        logger.info("Observer update method called - reloading data");
        if (isInitialized) {
            Platform.runLater(this::loadDataAsync);
        } else {
            logger.warn("Update called before controller was fully initialized");
        }
    }

    public void cleanup() {
        logger.info("Cleaning up resources in MainFormController");
        try {
            if (currentReferee != null && participantService instanceof Service) {
                logger.debug("Unregistering observer during cleanup");
                ((Service)participantService).unregisterObserver(currentReferee.getId());
            }
        } catch (Exception e) {
            logger.error("Error during cleanup", e);
        }
    }
}