package org.example.model;

import java.util.Objects;

public class Referee extends Entity<Long>{
    private String name;
    private Event event;
    private String username;
    private String password;

    public Referee(){
        super(null);
    }

    public Referee(Long id, String name, Event event, String username, String password) {
        super(id);
        this.name = name;
        this.event = event;
        this.username = username;
        this.password = password;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Event getEvent() {
        return event;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Referee referee = (Referee) o;
        return Objects.equals(name, referee.name) && Objects.equals(event, referee.event) && Objects.equals(username, referee.username) && Objects.equals(password, referee.password);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), name, event, username, password);
    }

    @Override
    public String toString() {
        return "Referee{" +
                "name='" + name + '\'' +
                ", event=" + event +
                ", username='" + username + '\'' +
                ", password='" + password + '\'' +
                '}';
    }
}