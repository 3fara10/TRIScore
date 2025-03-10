package org.example.model;

import java.util.Objects;

public class Albitru extends Entity<Long>{
    private String nume;
    private Proba proba;
    private String username;
    private String password;

    public Albitru(){
        super(null);
    }

    public Albitru(Long id, String nume, Proba proba, String username, String password) {
        super(id);
        this.nume = nume;
        this.proba = proba;
        this.username = username;
        this.password = password;
    }


    public String getNume() {
        return nume;
    }

    public void setNume(String nume) {
        this.nume = nume;
    }

    public Proba getProba() {
        return proba;
    }

    public void setProba(Proba proba) {
        this.proba = proba;
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
        Albitru albitru = (Albitru) o;
        return Objects.equals(nume, albitru.nume) && Objects.equals(proba, albitru.proba) && Objects.equals(username, albitru.username) && Objects.equals(password, albitru.password);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), nume, proba, username, password);
    }

    @Override
    public String toString() {
        return "Albitru{" +
                "nume='" + nume + '\'' +
                ", proba=" + proba +
                ", username='" + username + '\'' +
                ", password='" + password + '\'' +
                '}';
    }
}
