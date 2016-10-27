package org.optaplanner.examples.greatlearning.domain;

public class Location {
    private String name;
    private int rooms;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getRooms() {
        return rooms;
    }

    public void setRooms(int rooms) {
        this.rooms = rooms;
    }

    @Override
    public String toString() {
        return "Location{" +
                "name='" + name + '\'' +
                ", rooms=" + rooms +
                '}';
    }
}
