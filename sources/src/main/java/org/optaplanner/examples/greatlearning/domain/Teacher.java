package org.optaplanner.examples.greatlearning.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public class Teacher {
    private String name;
    private Set<String> canTeachCourses;
    private Set<String> restrictedLocations;

    public List<LocalDate> getHolidays() {
        return holidays;
    }

    public void setHolidays(List<LocalDate> holidays) {
        this.holidays = holidays;
    }

    private List<LocalDate> holidays;

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

    public Set<String> getRestrictedLocations() {
        return restrictedLocations;
    }

    public void setRestrictedLocations(Set<String> restrictedLocations) {
        this.restrictedLocations = restrictedLocations;
    }

    @Override
    public String toString() {
        return "Teacher{" +
                "name='" + name + '\'' +
                ", canTeachCourses=" + canTeachCourses +
                ", restrictedLocations=" + restrictedLocations +
                ", holidays=" + holidays +
                '}';
    }
}
