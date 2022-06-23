package com.ej.fiveads.classes;

public class UserData {

    private int rank;
    private final String name;
    private String raffleAmount;

    public UserData(String name, String raffleAmount) {
        this.name = name;
        this.raffleAmount = raffleAmount;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    public int getRank() {
        return rank;
    }

    public String getName() {
        return name;
    }

    public String getRaffleAmount() {
        return raffleAmount;
    }

    public void setRaffleAmount(String raffleAmount) {
        this.raffleAmount = raffleAmount;
    }
}
