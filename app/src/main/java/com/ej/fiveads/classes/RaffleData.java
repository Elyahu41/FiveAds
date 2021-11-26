package com.ej.fiveads.classes;

public class RaffleData {

    private final String raffleName;
    private final int raffleImage;
    private final String databaseRef;
    public static final int WEEKLY = 1;
    public static final int MONTHLY = 2;
    private final int raffleType;

    public RaffleData(String raffleName, int raffleImage, String databaseRef, int type) {
        this.raffleName = raffleName;
        this.raffleImage = raffleImage;
        this.databaseRef = databaseRef;
        this.raffleType = type;
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

    public int getRaffleType() {
        return raffleType;
    }
}
