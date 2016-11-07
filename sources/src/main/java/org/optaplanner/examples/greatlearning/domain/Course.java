package org.optaplanner.examples.greatlearning.domain;

import java.util.List;
import java.util.Map;

public class Course {
    private String name;
    private List<Teacher> teachers;
    /**
     * Course slots
     */
    private int slotsNum;

    private List<Map.Entry<String, Integer>> slots;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Course course = (Course) o;

        if (slotsNum != course.slotsNum) return false;
        if (name != null ? !name.equals(course.name) : course.name != null) return false;
        return teachers != null ? teachers.equals(course.teachers) : course.teachers == null;

    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (teachers != null ? teachers.hashCode() : 0);
        result = 31 * result + slotsNum;
        return result;
    }

    public List<Map.Entry<String, Integer>> getSlots() {
        return slots;
    }

    public void setSlots(List<Map.Entry<String, Integer>> slots) {
        this.slots = slots;
    }
}
