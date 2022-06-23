package com.ej.fiveads.classes;

public class RaffleData {

    private final String raffleName;
    private final int raffleImage;
    private final String databaseRef;

    private final int raffleDifficulty;
    public static final int EASY = 1;
    public static final int MEDIUM = 2;
    public static final int HARD = 3;


    public RaffleData(String raffleName, int raffleImage, String databaseRef, int raffleDifficulty) {
        this.raffleName = raffleName;
        this.raffleImage = raffleImage;
        this.databaseRef = databaseRef;
        this.raffleDifficulty = raffleDifficulty;
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

    public int getRaffleDifficulty() {
        return this.raffleDifficulty;
    }
}
