package org.example.model;

import java.util.Objects;

public class Rezultat extends Entity<Long>{
    private Proba proba;
    private Participant participant;
    private int puncte;

    public Rezultat(){
        super(null);
    }
    public Rezultat(Long id, Proba proba, Participant participant, int puncte) {
        super(id);
        this.proba = proba;
        this.participant = participant;
        this.puncte = puncte;
    }


    public Proba getProba() {
        return proba;
    }

    public void setProba(Proba proba) {
        this.proba = proba;
    }

    public Participant getParticipant() {
        return participant;
    }

    public void setParticipant(Participant participant) {
        this.participant = participant;
    }

    public int getPuncte() {
        return puncte;
    }

    public void setPuncte(int puncte) {
        this.puncte = puncte;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Rezultat rezultat = (Rezultat) o;
        return puncte == rezultat.puncte && Objects.equals(proba, rezultat.proba) && Objects.equals(participant, rezultat.participant);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), proba, participant, puncte);
    }

    @Override
    public String toString() {
        return "Rezultat{" +
                super.toString() +
                ", proba=" + proba +
                ", participant=" + participant +
                ", puncte=" + puncte +
                '}';
    }
}
