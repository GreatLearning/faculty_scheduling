package org.optaplanner.examples.gl.domain;

import java.util.Set;

/**
 * Created by vinodvr on 21/10/16.
 */
public class Teacher {

    String name;
    Set<String> canTeachCourses;

    public Teacher(String name, Set<String> canTeachCourses) {
        this.name = name;
        this.canTeachCourses = canTeachCourses;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Teacher teacher = (Teacher) o;

        if (name != null ? !name.equals(teacher.name) : teacher.name != null) return false;
        return canTeachCourses != null ? canTeachCourses.equals(teacher.canTeachCourses) : teacher.canTeachCourses == null;

    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (canTeachCourses != null ? canTeachCourses.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Teacher{" +
                "name='" + name + '\'' +
                ", canTeachCourses=" + canTeachCourses +
                '}';
    }
}
