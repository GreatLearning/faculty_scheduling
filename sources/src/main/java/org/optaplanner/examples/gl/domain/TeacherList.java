package org.optaplanner.examples.gl.domain;

import java.util.List;

public class TeacherList {
    private List<Teacher> teachers;

    public List<Teacher> getTeachers() {
        return teachers;
    }

    public void setTeachers(List<Teacher> teachers) {
        this.teachers = teachers;
    }

    @Override
    public String toString() {
        return "TeacherList{" +
                "teachers=" + teachers +
                '}';
    }
}
