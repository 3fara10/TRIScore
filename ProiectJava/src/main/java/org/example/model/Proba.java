package org.example.model;

import java.util.Objects;

public class Proba extends Entity<Long>{
    private String nume;

    public Proba() {
        super(null);
    }

    public Proba(Long id, String nume) {
        super(id);
        this.nume = nume;
    }


    public String getNume() {
        return nume;
    }

    public void setNume(String nume) {
        this.nume = nume;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Proba proba = (Proba) o;
        return Objects.equals(nume, proba.nume);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), nume);
    }

    @Override
    public String toString() {
        return "Proba{" +super.toString()+
                ", nume='" + nume + '\'' +
                '}';
    }
}
