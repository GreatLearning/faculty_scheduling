package org.optaplanner.examples.greatlearning.solver;

import org.apache.commons.lang3.builder.CompareToBuilder;
import org.optaplanner.examples.greatlearning.domain.CourseSchedule;

import java.io.Serializable;
import java.util.Comparator;

public class CourseDifficultyComparator implements Comparator<CourseSchedule>, Serializable {
    @Override
    public int compare(CourseSchedule o1, CourseSchedule o2) {
        return new CompareToBuilder()
                .append(o1.getTeacherList().size(), o2.getTeacherList().size())
                .toComparison();
    }
}
