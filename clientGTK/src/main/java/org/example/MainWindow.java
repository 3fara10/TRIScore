package org.example;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.model.Referee;
import org.example.networking.dto.ParticipantResultDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

/**
 * Main window for the triathlon application.
 * Displays participants, results, and allows adding new results.
 */
public class MainWindow extends JFrame implements ClientCtrl.UpdateListener {
    private static final Logger logger = LogManager.getLogger(MainWindow.class);

    private final ClientCtrl ctrl;
    private final Referee currentReferee;

    private JTabbedPane tabbedPane;
    private JTable participantsTable;
    private JTable resultsTable;
    private DefaultTableModel participantsTableModel;
    private DefaultTableModel resultsTableModel;
    private JSpinner pointsSpinner;
    private JButton addResultButton;
    private JButton logoutButton;
    private JLabel statusLabel;
    private JLabel eventLabel;
    private JLabel refereeLabel;

    // Column indexes for participants table
    private enum ParticipantColumns {
        ID(0),
        NAME(1),
        TOTAL_POINTS(2);

        private final int index;

        ParticipantColumns(int index) {
            this.index = index;
        }

        public int getIndex() {
            return index;
        }
    }

    // Column indexes for results table
    private enum ResultColumns {
        ID(0),
        NAME(1),
        POINTS(2);

        private final int index;

        ResultColumns(int index) {
            this.index = index;
        }

        public int getIndex() {
            return index;
        }
    }

    /**
     * Creates a new main window with the specified controller and referee.
     */
    public MainWindow(ClientCtrl ctrl, Referee currentReferee) {
        super("Event Scoring System");
        this.ctrl = ctrl;
        this.currentReferee = currentReferee;

        initializeWindow();
        createUI();
        loadData();

        // Register for updates
        ctrl.addUpdateListener(this);

        logger.debug("Main window initialized");
    }

    /**
     * Initializes the window properties.
     */
    private void initializeWindow() {
        setSize(1000, 700);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                closeWindow();
            }
        });
    }

    /**
     * Creates the user interface components.
     */
    private void createUI() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Header panel with referee and event info
        JPanel headerPanel = new JPanel(new BorderLayout(20, 0));
        refereeLabel = new JLabel("Referee: " + currentReferee.getName());
        eventLabel = new JLabel("Event: " + currentReferee.getEvent().getName());
        eventLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        headerPanel.add(refereeLabel, BorderLayout.WEST);
        headerPanel.add(eventLabel, BorderLayout.EAST);

        // Tabbed pane for participants and results
        tabbedPane = new JTabbedPane();

        // Create participants panel
        JPanel participantsPanel = createParticipantsPanel();
        tabbedPane.addTab("Participants", participantsPanel);

        // Create results panel
        JPanel resultsPanel = createResultsPanel();
        tabbedPane.addTab("Results", resultsPanel);

        // Controls panel
        JPanel controlsPanel = createControlsPanel();

        // Status panel
        JPanel statusPanel = createStatusPanel();

        // Add components to main panel
        mainPanel.add(headerPanel, BorderLayout.NORTH);
        mainPanel.add(tabbedPane, BorderLayout.CENTER);

        // Bottom panel contains controls and status
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(controlsPanel, BorderLayout.NORTH);
        bottomPanel.add(statusPanel, BorderLayout.SOUTH);

        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        // Add main panel to frame
        setContentPane(mainPanel);

        // Add event listeners
        addEventListeners();
    }

    /**
     * Creates the participants panel.
     */
    private JPanel createParticipantsPanel() {
        JPanel participantsPanel = new JPanel(new BorderLayout(0, 5));
        String[] participantColumns = {"ID", "Name", "Total Points"};
        participantsTableModel = new DefaultTableModel(participantColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        participantsTable = new JTable(participantsTableModel);
        participantsTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Set column widths
        participantsTable.getColumnModel().getColumn(ParticipantColumns.NAME.getIndex()).setPreferredWidth(300);
        participantsTable.getColumnModel().getColumn(ParticipantColumns.TOTAL_POINTS.getIndex()).setPreferredWidth(100);

        // Hide the ID column
        participantsTable.getColumnModel().getColumn(ParticipantColumns.ID.getIndex()).setMinWidth(0);
        participantsTable.getColumnModel().getColumn(ParticipantColumns.ID.getIndex()).setMaxWidth(0);
        participantsTable.getColumnModel().getColumn(ParticipantColumns.ID.getIndex()).setWidth(0);

        participantsPanel.add(new JScrollPane(participantsTable), BorderLayout.CENTER);
        return participantsPanel;
    }

    /**
     * Creates the results panel.
     */
    private JPanel createResultsPanel() {
        JPanel resultsPanel = new JPanel(new BorderLayout(0, 5));
        String[] resultColumns = {"ID", "Name", "Points"};
        resultsTableModel = new DefaultTableModel(resultColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        resultsTable = new JTable(resultsTableModel);
        resultsTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Set column widths
        resultsTable.getColumnModel().getColumn(ResultColumns.NAME.getIndex()).setPreferredWidth(300);
        resultsTable.getColumnModel().getColumn(ResultColumns.POINTS.getIndex()).setPreferredWidth(100);

        // Hide the ID column
        resultsTable.getColumnModel().getColumn(ResultColumns.ID.getIndex()).setMinWidth(0);
        resultsTable.getColumnModel().getColumn(ResultColumns.ID.getIndex()).setMaxWidth(0);
        resultsTable.getColumnModel().getColumn(ResultColumns.ID.getIndex()).setWidth(0);

        resultsPanel.add(new JScrollPane(resultsTable), BorderLayout.CENTER);
        return resultsPanel;
    }

    /**
     * Creates the controls panel.
     */
    private JPanel createControlsPanel() {
        JPanel controlsPanel = new JPanel(new BorderLayout(10, 0));
        controlsPanel.setBorder(new EmptyBorder(10, 0, 10, 0));

        // Left controls (points and add result button)
        JPanel leftControls = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        JLabel pointsLabel = new JLabel("Points:");
        SpinnerNumberModel spinnerModel = new SpinnerNumberModel(0, 0, 100, 1);
        pointsSpinner = new JSpinner(spinnerModel);
        ((JSpinner.DefaultEditor) pointsSpinner.getEditor()).getTextField().setColumns(3);

        addResultButton = new JButton("Add Result");
        addResultButton.setEnabled(false);
        addResultButton.setPreferredSize(new Dimension(120, 28));

        leftControls.add(pointsLabel);
        leftControls.add(pointsSpinner);
        leftControls.add(addResultButton);

        // Right controls (logout button)
        JPanel rightControls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        logoutButton = new JButton("Logout");
        logoutButton.setPreferredSize(new Dimension(100, 28));
        rightControls.add(logoutButton);

        controlsPanel.add(leftControls, BorderLayout.WEST);
        controlsPanel.add(rightControls, BorderLayout.EAST);

        return controlsPanel;
    }

    /**
     * Creates the status panel.
     */
    private JPanel createStatusPanel() {
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBorder(new EmptyBorder(5, 0, 0, 0));
        statusLabel = new JLabel("Ready");
        statusPanel.add(statusLabel, BorderLayout.WEST);
        return statusPanel;
    }

    /**
     * Adds event listeners to components.
     */
    private void addEventListeners() {
        addResultButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onAddResultClicked();
            }
        });

        logoutButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onLogoutClicked();
            }
        });

        participantsTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    onParticipantSelectionChanged();
                }
            }
        });
    }

    /**
     * Loads data into the tables.
     */
    private void loadData() {
        try {
            setSensitive(false);
            statusLabel.setText("Loading data...");

            loadParticipants();
            loadResults();

            statusLabel.setText("Data loaded successfully");
        } catch (Exception ex) {
            logger.error("Error loading data", ex);
            statusLabel.setText("Error loading data: " + ex.getMessage());
            showErrorDialog("Failed to load data", ex.getMessage());
        } finally {
            setSensitive(true);
        }
    }

    /**
     * Loads participants data into the participants table.
     */
    private void loadParticipants() {
        try {
            statusLabel.setText("Loading participants...");
            setSensitive(false);

            ParticipantResultDTO[] participantsWithResults = ctrl.getAllParticipantsSortedByName();
            Map<UUID, Integer> pointsMap = new HashMap<>();

            // Clear the table
            participantsTableModel.setRowCount(0);

            for (ParticipantResultDTO participant : participantsWithResults) {
                pointsMap.put(participant.getParticipantId(), participant.getPoints());

                logger.debug("Adding participant: {}, ID: {}, Points: {}",
                        participant.getParticipantName(),
                        participant.getParticipantId(),
                        participant.getPoints());

                participantsTableModel.addRow(new Object[]{
                        participant.getParticipantId().toString(),
                        participant.getParticipantName(),
                        participant.getPoints()
                });
            }

            logger.debug("Loaded {} participants into the UI", participantsTableModel.getRowCount());
            statusLabel.setText("Participants loaded successfully");
        } catch (Exception ex) {
            logger.error("Error loading participants", ex);
            statusLabel.setText("Error loading participants");
            showErrorDialog("Failed to load participants", ex.getMessage());
            throw new RuntimeException(ex);
        } finally {
            setSensitive(true);
        }
    }

    /**
     * Loads results data into the results table.
     */
    private void loadResults() {
        try {
            // Clear the table
            resultsTableModel.setRowCount(0);

            ParticipantResultDTO[] results = ctrl.getParticipantsWithResultsForEvent();
            logger.debug("Got {} results from service", results.length);

            for (ParticipantResultDTO result : results) {
                logger.debug("Adding result: {}, ID: {}, Points: {}",
                        result.getParticipantName(),
                        result.getParticipantId(),
                        result.getPoints());

                resultsTableModel.addRow(new Object[]{
                        result.getParticipantId().toString(),
                        result.getParticipantName(),
                        result.getPoints()
                });
            }

            logger.debug("Loaded {} results into the UI", resultsTableModel.getRowCount());
        } catch (Exception ex) {
            logger.error("Error loading results", ex);
            throw new RuntimeException(ex);
        }
    }

    /**
     * Handles the add result button click event.
     */
    private void onAddResultClicked() {
        int selectedRow = participantsTable.getSelectedRow();
        if (selectedRow < 0) {
            showErrorDialog("Selection Error", "Please select a participant first");
            return;
        }

        String participantId = (String) participantsTableModel.getValueAt(selectedRow, ParticipantColumns.ID.getIndex());
        String participantName = (String) participantsTableModel.getValueAt(selectedRow, ParticipantColumns.NAME.getIndex());
        int points = (Integer) pointsSpinner.getValue();

        try {
            setSensitive(false);
            statusLabel.setText("Adding result for " + participantName + "...");

            // Use SwingWorker to do the work in the background
            SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    ctrl.addResultAsync(UUID.fromString(participantId), points).get();
                    return null;
                }

                @Override
                protected void done() {
                    try {
                        get();
                        loadParticipants();
                        loadResults();
                        statusLabel.setText("Added " + points + " points for " + participantName);
                        pointsSpinner.setValue(0);
                    } catch (InterruptedException | ExecutionException ex) {
                        logger.error("Error adding result", ex);
                        statusLabel.setText("Error adding result: " + ex.getMessage());
                        showErrorDialog("Failed to add result", ex.getMessage());
                    } finally {
                        setSensitive(true);
                    }
                }
            };

            worker.execute();
        } catch (Exception ex) {
            logger.error("Error adding result", ex);
            statusLabel.setText("Error adding result: " + ex.getMessage());
            showErrorDialog("Failed to add result", ex.getMessage());
            setSensitive(true);
        }
    }

    /**
     * Handles the logout button click event.
     */
    private void onLogoutClicked() {
        try {
            setSensitive(false);
            statusLabel.setText("Logging out...");

            ctrl.logout();

            LoginWindow loginWindow = new LoginWindow(ctrl);
            loginWindow.setVisible(true);

            // Remove this window's update listener
            ctrl.removeUpdateListener(this);

            dispose();
        } catch (Exception ex) {
            logger.error("Error during logout", ex);
            statusLabel.setText("Error logging out: " + ex.getMessage());
            showErrorDialog("Logout failed", ex.getMessage());
            setSensitive(true);
        }
    }

    /**
     * Handles participant selection change event.
     */
    private void onParticipantSelectionChanged() {
        int selectedRow = participantsTable.getSelectedRow();
        boolean hasSelection = selectedRow >= 0;

        addResultButton.setEnabled(hasSelection);

        if (hasSelection) {
            String participantName = (String) participantsTableModel.getValueAt(selectedRow, ParticipantColumns.NAME.getIndex());
            statusLabel.setText("Selected: " + participantName);
        }
    }

    /**
     * Handles client update events.
     */
    private void onClientUpdate(ClientCtrl.ClientEvent event) {
        logger.debug("Received update event: {}", event);

        SwingUtilities.invokeLater(() -> {
            switch (event) {
                case EVENT_STATUS_CHANGED:
                    loadData();
                    break;
            }
        });
    }

    /**
     * Enables or disables UI components.
     */
    private void setSensitive(boolean sensitive) {
        tabbedPane.setEnabled(sensitive);
        pointsSpinner.setEnabled(sensitive);
        logoutButton.setEnabled(sensitive);
        addResultButton.setEnabled(sensitive && participantsTable.getSelectedRow() >= 0);
    }

    /**
     * Shows an error dialog with the specified title and message.
     */
    private void showErrorDialog(String title, String message) {
        JOptionPane.showMessageDialog(
                this,
                message,
                title,
                JOptionPane.ERROR_MESSAGE
        );
    }

    /**
     * Closes the window and performs cleanup.
     */
    private void closeWindow() {
        ctrl.removeUpdateListener(this);

        try {
            ctrl.logout();
        } catch (Exception ex) {
            logger.error("Error during logout on window close", ex);
        }

        dispose();
        System.exit(0);
    }

    /**
     * Receives update notifications from the controller.
     */
    @Override
    public void onUpdate(ClientCtrl.ClientEvent event) {
        onClientUpdate(event);
    }
}