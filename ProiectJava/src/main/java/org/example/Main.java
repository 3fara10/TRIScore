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
            Event event = new Event("Popescu Ion");
            repoEvent.add(event);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}