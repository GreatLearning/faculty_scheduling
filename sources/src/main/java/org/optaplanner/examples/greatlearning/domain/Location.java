package org.optaplanner.examples.greatlearning.domain;

import java.time.LocalDate;
import java.util.List;

public class Location {
    private String name;
    private int rooms;
    private List<LocalDate> holidays;

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

    public List<LocalDate> getHolidays() {
        return holidays;
    }

    public void setHolidays(List<LocalDate> holidays) {
        this.holidays = holidays;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Location location = (Location) o;

        if (rooms != location.rooms) return false;
        if (name != null ? !name.equals(location.name) : location.name != null) return false;
        return holidays != null ? holidays.equals(location.holidays) : location.holidays == null;

    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + rooms;
        result = 31 * result + (holidays != null ? holidays.hashCode() : 0);
        return result;
    }
}
