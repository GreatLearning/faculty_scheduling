package org.optaplanner.examples.greatlearning.domain;

import java.util.Set;

public class Teacher {
    private String name;
    private Set<String> canTeachCourses;

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
    public String toString() {
        return "Teacher{" +
                "name='" + name + '\'' +
                ", canTeachCourses=" + canTeachCourses +
                '}';
    }
}
