package org.optaplanner.examples.gl.domain;

/**
 * Created by vinodvr on 21/10/16.
 */
public class Location {

    String name;
    int rooms;

    public Location(String name, int rooms) {
        this.name = name;
        this.rooms = rooms;
    }

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
