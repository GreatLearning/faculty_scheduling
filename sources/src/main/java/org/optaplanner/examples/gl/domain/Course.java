package org.optaplanner.examples.gl.domain;

import java.util.List;

/**
 * Created by vinodvr on 21/10/16.
 */
public class Course {
    private String name;
    private List<Teacher> teachers;

    public Course(String name, List<Teacher> teachers) {
        this.name = name;
        this.teachers = teachers;
    }

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

    @Override
    public String toString() {
        return "Course{" +
                "name='" + name + '\'' +
                ", teachers=" + teachers +
                '}';
    }
}
