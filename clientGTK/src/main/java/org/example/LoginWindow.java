package org.example;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.model.Referee;
import org.example.service.IObserver;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Login window for the triathlon application.
 * Provides a user interface for referees to log in.
 */
public class LoginWindow extends JFrame implements IObserver {
    private static final Logger logger = LogManager.getLogger(LoginWindow.class);
    private final ClientCtrl ctrl;

    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JButton clearButton;
    private Referee currentReferee;

    /**
     * Creates a new login window with the specified controller.
     *
     * @param ctrl The client controller
     */
    public LoginWindow(ClientCtrl ctrl) {
        super("Event Scoring System - Login");
        this.ctrl = ctrl;

        initializeWindow();
        initializeComponents();

        logger.debug("Login window initialized");
    }

    /**
     * Initializes the window properties.
     */
    private void initializeWindow() {
        setSize(400, 250);
        setLocationRelativeTo(null); // Center on screen
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                logger.debug("Login window closing");
                System.exit(0);
            }
        });
    }

    /**
     * Initializes the UI components.
     */
    private void initializeComponents() {
        // Main panel with padding
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(60, 60, 60, 60));

        // Title
        JLabel titleLabel = new JLabel("Event Scoring System");
        titleLabel.setFont(new Font(titleLabel.getFont().getName(), Font.BOLD, 18));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Username panel
        JPanel usernamePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel usernameLabel = new JLabel("Username:");
        usernameField = new JTextField(20);
        usernamePanel.add(usernameLabel);
        usernamePanel.add(usernameField);

        // Password panel
        JPanel passwordPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel passwordLabel = new JLabel("Password:");
        passwordField = new JPasswordField(20);
        passwordPanel.add(passwordLabel);
        passwordPanel.add(passwordField);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        loginButton = new JButton("Login");
        clearButton = new JButton("Clear");

        ButtonListener buttonListener = new ButtonListener();
        loginButton.addActionListener((ActionListener) buttonListener);
        clearButton.addActionListener(buttonListener);

        buttonPanel.add(loginButton);
        buttonPanel.add(clearButton);

        // Add components to main panel with spacing
        mainPanel.add(Box.createVerticalStrut(10));
        mainPanel.add(titleLabel);
        mainPanel.add(Box.createVerticalStrut(20));
        mainPanel.add(usernamePanel);
        mainPanel.add(Box.createVerticalStrut(5));
        mainPanel.add(passwordPanel);
        mainPanel.add(Box.createVerticalStrut(20));
        mainPanel.add(buttonPanel);

        add(mainPanel);
    }

    /**
     * Listens for button clicks.
     */
    private class ButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (e.getSource() == loginButton) {
                handleLogin();
            } else if (e.getSource() == clearButton) {
                clearFields();
            }
        }
    }

    /**
     * Handles the login process.
     */
    private void handleLogin() {
        String username = usernameField.getText();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            showErrorMessage("Please enter both username and password.");
            return;
        }

        try {
            setUIState(false);

            ctrl.login(username, password);
            this.currentReferee = ctrl.getCurrentReferee();

            if (currentReferee != null) {
                logger.debug("Login successful for: {}", username);

                // Open main window
                MainWindow mainWindow = new MainWindow(ctrl, currentReferee);
                mainWindow.setVisible(true);

                // Close login window
                dispose();
            } else {
                showErrorMessage("Invalid username or password.");
            }
        } catch(IllegalArgumentException e) {
            logger.error("The user is already connected", e);
            showErrorMessage("The user is already connected");
        } catch (Exception ex) {
            logger.error("Login failed", ex);
            showErrorMessage("Login failed: " + ex.getMessage());
        } finally
         {
            setUIState(true);
        }
    }

    /**
     * Clears the username and password fields.
     */
    private void clearFields() {
        usernameField.setText("");
        passwordField.setText("");
        usernameField.requestFocus();
    }

    /**
     * Enables or disables UI components.
     *
     * @param enabled Whether the components should be enabled
     */
    private void setUIState(boolean enabled) {
        usernameField.setEnabled(enabled);
        passwordField.setEnabled(enabled);
        loginButton.setEnabled(enabled);
        clearButton.setEnabled(enabled);
    }

    /**
     * Shows an error message dialog.
     *
     * @param message The error message to display
     */
    private void showErrorMessage(String message) {
        JOptionPane.showMessageDialog(
                this,
                message,
                "Error",
                JOptionPane.ERROR_MESSAGE
        );
    }

    /**
     * Receives update notifications from the server.
     */
    @Override
    public void update() {
        logger.debug("Received update in login window");
    }
}