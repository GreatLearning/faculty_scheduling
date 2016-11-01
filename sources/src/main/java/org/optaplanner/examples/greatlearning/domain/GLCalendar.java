package org.optaplanner.examples.greatlearning.domain;

import org.optaplanner.core.api.domain.solution.PlanningEntityCollectionProperty;
import org.optaplanner.core.api.domain.solution.PlanningSolution;
import org.optaplanner.core.api.domain.solution.Solution;
import org.optaplanner.core.api.score.buildin.hardmediumsoft.HardMediumSoftScore;

import java.util.Collection;
import java.util.List;

@PlanningSolution
public class GLCalendar implements Solution<HardMediumSoftScore> {

    private List<String> constraintsBroken;

    private HardMediumSoftScore score;

    @PlanningEntityCollectionProperty
    private List<CourseSchedule> courseScheduleList;

    @Override
    public HardMediumSoftScore getScore() {
        return score;
    }

    @Override
    public void setScore(HardMediumSoftScore score) {
        this.score = score;
    }

    @Override
    public Collection<?> getProblemFacts() {
        return null;
    }

    public List<CourseSchedule> getCourseScheduleList() {
        return courseScheduleList;
    }

    public void setCourseScheduleList(List<CourseSchedule> courseScheduleList) {
        this.courseScheduleList = courseScheduleList;
    }

    public List<String> getConstraintsBroken() {
        return constraintsBroken;
    }

    public void setConstraintsBroken(List<String> constraintsBroken) {
        this.constraintsBroken = constraintsBroken;
    }
}
