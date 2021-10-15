package com.ej.fiveads.classes;

public class UserData implements Comparable<UserData> {

    private final int rank;
    private final String name;
    private final int ticketsSubmitted;

    public UserData(int rank, String name, int ticketsSubmitted) {
        this.rank = rank;
        this.name = name;
        this.ticketsSubmitted = ticketsSubmitted;
    }

    public int getRank() {
        return rank;
    }

    public String getName() {
        return name;
    }

    public int getTicketsSubmitted() {
        return ticketsSubmitted;
    }

    @Override
    public int compareTo(UserData otherUser) {
        if (this.getRank() > otherUser.getRank()) {
            return 1;
        } else if (this.getRank() == ((UserData) otherUser).getRank()) {
            return 0;
        }
        return -1;
    }
}
