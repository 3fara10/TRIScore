package org.example;

import org.example.model.Event;
import org.example.model.Referee;
import org.example.repository.IRepositoryEvent;
import org.example.repository.IRepositoryReferee;
import org.example.repository.SQLRepositoryEvent;
import org.example.repository.SQLRepositoryReferee;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.Properties;

public class Main {
    public static void main(String[] args) {
        Properties props = new Properties();
        try {
            URL resourceUrl = Main.class.getClassLoader().getResource("bd.config");
            File configFile = new File(resourceUrl.toURI());
            props.load(new FileReader(configFile));

            IRepositoryEvent repoEvent = new SQLRepositoryEvent(props);
            System.out.println("Adaugam in repository...");
            //repoEvent.add(new Event(465L, "Popescu Ion"));
            Event event = new Event();

            event.setName("Olimpiada Națională de Informatică");
            repoEvent.add(event);

            // Insert a referee
            event.setId(2L);
            Referee referee = new Referee();
            referee.setName("Prof. Stanescu");
            referee.setUsername("prof_stanescu");
            referee.setPassword("parola123");
            referee.setEvent(new Event(event.getId(), event.getName()));
            IRepositoryReferee repositoryReferee = new SQLRepositoryReferee(props);
            repositoryReferee.add(referee);

            System.out.println("Afisam datele din repository events...");
            for(Event e : repoEvent.findAll()){
                System.out.println(e);
            }

            System.out.println("Afisam datele din repository referee...");
            for(Referee e : repositoryReferee.findAll()){
                System.out.println(e);
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}