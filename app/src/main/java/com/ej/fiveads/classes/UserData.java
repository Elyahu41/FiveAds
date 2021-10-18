package com.ej.fiveads.classes;

public class UserData implements Comparable<UserData> {

    private int rank;
    private final String name;
    private final int ticketsSubmitted;

    public UserData(String name, int ticketsSubmitted) {
        this.name = name;
        this.ticketsSubmitted = ticketsSubmitted;
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

    public int getTicketsSubmitted() {
        return ticketsSubmitted;
    }

    @Override
    public int compareTo(UserData otherUser) {
        if (this.getTicketsSubmitted() > otherUser.getTicketsSubmitted()) {
            return 1;
        } else if (this.getTicketsSubmitted() == ((UserData) otherUser).getTicketsSubmitted()) {
            return 0;
        }
        return -1;
    }
}
