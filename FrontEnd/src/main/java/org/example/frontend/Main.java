package org.example.frontend;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.example.model.Event;
import org.example.model.Referee;
import org.example.repository.*;
import org.example.service.BcryptPasswordService;
import org.example.service.*;

import java.io.*;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.example.utils.ThreadPoolManager.*;

import static org.example.utils.ThreadPoolManager.shutdownExecutors;

public class Main extends Application {
    Properties props = new Properties();

    @Override
    public void start(Stage primaryStage) {
        try {
            // Load database configuration
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream("bd.config");
            props.load(inputStream);

            // Initialize repositories
            IRepositoryEvent repositoryEvent = new SQLRepositoryEvent(props);
            IRepositoryParticipant repositoryParticipant = new SQLRepositoryParticipant(props);
            IRepositoryResult repositoryResult = new SQLRepositoryResult(props);
            IRepositoryReferee repositoryReferee = new SQLRepositoryReferee(props);
            IRepositoryDTO repositoryDTO = new SQLRepositoryDTO(props);

            // Initialize services
            IPasswordService passwordService = new BcryptPasswordService(12);
            IAuthentificationService authenticationService = new AuthentificationService(repositoryReferee, passwordService);
            IParticipantService participantService = new ParticipantService(repositoryDTO);
            IResultService resultService = new ResultService(repositoryResult);

//            Event atletism=new Event("atletism");
//            repositoryEvent.addAsync(atletism);
//            CompletableFuture<Optional<Event>> atletism1 =repositoryEvent.findOneAsync(UUID.fromString("e60e5e15-5c79-4faf-a54b-c36df814dd61"));
//            String hashedPassword = passwordService.hashPassword("X");
//            var referee = new Referee("Popescu Ion",atletism1.get().get(), "username", hashedPassword);
//            repositoryReferee.addAsync(referee);
//            Event ciclism=new Event("ciclism");
//                 repositoryEvent.addAsync(ciclism);
//            CompletableFuture<Optional<Event>> ciclism =repositoryEvent.findOneAsync(UUID.fromString("668c4dae-3cc8-4922-8deb-2a201d022246"));
//            String hashedPassword = passwordService.hashPassword("1234");
//            var referee = new Referee("Popescu Maria",ciclism.get().get(), "popescu_maria", hashedPassword);
//            repositoryReferee.addAsync(referee);
//            Participant p1=new Participant("Mihai Popescu");
//            Participant p2=new Participant("Vasile George");
//            repositoryParticipant.addAsync(p1);
//            repositoryParticipant.addAsync(p2);
            // Load and display the login form
            FXMLLoader loader = new FXMLLoader(getClass().getResource("LoginForm.fxml"));
            Parent root = loader.load();
            LoginFormController controller = loader.getController();
            controller.initialize(primaryStage, authenticationService, participantService, resultService);
            Scene scene = new Scene(root, 600, 400);
            primaryStage.setTitle("Login");
            primaryStage.setScene(scene);
            primaryStage.show();

            primaryStage.setOnCloseRequest(event -> {
                // Oprim thread-urile și eliberăm resursele
                shutdownExecutors();
                System.exit(0);
                Platform.exit();
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static void main(String[] args) {
        launch(args);

    }
}
