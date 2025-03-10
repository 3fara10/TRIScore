package org.example.model;

import java.util.Objects;

public class Participant extends Entity<Long>{
    private String nume;

    public Participant(Long id, String nume) {
        super(id);
        this.nume = nume;
    }

    public Participant() {
        super(null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Participant that = (Participant) o;
        return Objects.equals(nume, that.nume);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), nume);
    }


    @Override
    public String toString() {
        return "Participant{" +
                super.toString()+
                ", nume='" + nume + '\'' +
                '}';
    }
}
