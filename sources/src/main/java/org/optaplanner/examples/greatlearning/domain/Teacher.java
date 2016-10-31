package org.optaplanner.examples.greatlearning.domain;

import java.time.LocalDate;
import java.util.Set;

public class Teacher {
    private String name;
    private Set<String> canTeachCourses;
    private Set<String> availableLocations;
    private Set<LocalDate> holidays;

    public Set<LocalDate> getHolidays() {
        return holidays;
    }

    public void setHolidays(Set<LocalDate> holidays) {
        this.holidays = holidays;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<String> getCanTeachCourses() {
        return canTeachCourses;
    }

    public void setCanTeachCourses(Set<String> canTeachCourses) {
        this.canTeachCourses = canTeachCourses;
    }

    public Set<String> getAvailableLocations() {
        return availableLocations;
    }

    public void setAvailableLocations(Set<String> availableLocations) {
        this.availableLocations = availableLocations;
    }

    @Override
    public String toString() {
        return "Teacher{" +
                "name='" + name + '\'' +
                ", canTeachCourses=" + canTeachCourses +
                ", availableLocations=" + availableLocations +
                ", holidays=" + holidays +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Teacher teacher = (Teacher) o;

        if (name != null ? !name.equals(teacher.name) : teacher.name != null) return false;
        if (canTeachCourses != null ? !canTeachCourses.equals(teacher.canTeachCourses) : teacher.canTeachCourses != null)
            return false;
        if (availableLocations != null ? !availableLocations.equals(teacher.availableLocations) : teacher.availableLocations != null)
            return false;
        return holidays != null ? holidays.equals(teacher.holidays) : teacher.holidays == null;

    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (canTeachCourses != null ? canTeachCourses.hashCode() : 0);
        result = 31 * result + (availableLocations != null ? availableLocations.hashCode() : 0);
        result = 31 * result + (holidays != null ? holidays.hashCode() : 0);
        return result;
    }
}
