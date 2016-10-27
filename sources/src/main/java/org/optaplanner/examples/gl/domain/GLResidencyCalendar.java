package org.optaplanner.examples.gl.domain;

import org.optaplanner.core.api.domain.solution.PlanningEntityCollectionProperty;
import org.optaplanner.core.api.domain.solution.PlanningSolution;
import org.optaplanner.core.api.domain.solution.Solution;
import org.optaplanner.core.api.domain.valuerange.ValueRangeProvider;
import org.optaplanner.core.api.score.buildin.hardmediumsoft.HardMediumSoftScore;
import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by vinodvr on 24/10/16.
 */
@PlanningSolution
public class GLResidencyCalendar implements Solution<HardMediumSoftScore> {


    private List<Teacher> teacherList;
    private List<Location> locationList;
    private List<Residency> residencyList;


    private HardMediumSoftScore score;

    @PlanningEntityCollectionProperty
    public List<Residency> getResidencyList() {
        return residencyList;
    }


    @Override
    public Collection<?> getProblemFacts() {
        return null;    //as we are not using Drools
    }

    @Override
    public HardMediumSoftScore getScore() {
        return score;
    }

    @Override
    public void setScore(HardMediumSoftScore score) {
        this.score = score;
    }


    public List<Teacher> getTeacherList() {
        return teacherList;
    }

    public void setTeacherList(List<Teacher> teacherList) {
        this.teacherList = teacherList;
    }

    public List<Location> getLocationList() {
        return locationList;
    }

    public void setLocationList(List<Location> locationList) {
        this.locationList = locationList;
    }

    public void setResidencyList(List<Residency> residencyList) {
        this.residencyList = residencyList;
    }

    @Override
    public String toString() {
        return "GLResidencyCalendar{" +
                "teacherList=" + teacherList +
                ", locationList=" + locationList +
                ", residencyList=" + residencyList +
                ", score=" + score +
                '}';
    }
}
