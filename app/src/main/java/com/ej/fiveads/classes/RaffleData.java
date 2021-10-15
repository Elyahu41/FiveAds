package com.ej.fiveads.classes;

public class RaffleData {

    private final String raffleName;
    private final int raffleImage;
    private final String databaseRef;

    public RaffleData(String raffleName, int raffleImage, String databaseRef) {
        this.raffleName = raffleName;
        this.raffleImage = raffleImage;
        this.databaseRef = databaseRef;
    }

    public String getRaffleName() {
        return raffleName;
    }

    public int getRaffleImage() {
        return raffleImage;
    }

    public String getDatabaseRef() {
        return databaseRef;
    }
}
