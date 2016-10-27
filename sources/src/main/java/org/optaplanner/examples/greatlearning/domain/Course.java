package org.optaplanner.examples.greatlearning.domain;

import java.util.List;

public class Course {
    private String name;
    private List<Teacher> teachers;
    /**
     * Course slots
     */
    private int slotsNum;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Teacher> getTeachers() {
        return teachers;
    }

    public void setTeachers(List<Teacher> teachers) {
        this.teachers = teachers;
    }

    public int getSlotsNum() {
        return slotsNum;
    }

    public void setSlotsNum(int slotsNum) {
        this.slotsNum = slotsNum;
    }

    @Override
    public String toString() {
        return "Course{" +
                "name='" + name + '\'' +
                ", teachers=" + teachers +
                ", slotsNum=" + slotsNum +
                '}';
    }
}
