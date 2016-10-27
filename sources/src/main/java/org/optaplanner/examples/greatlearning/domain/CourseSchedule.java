package org.optaplanner.examples.greatlearning.domain;

import org.optaplanner.core.api.domain.entity.PlanningEntity;
import org.optaplanner.core.api.domain.valuerange.ValueRangeProvider;
import org.optaplanner.core.api.domain.variable.PlanningVariable;
import org.optaplanner.examples.greatlearning.solver.CourseDifficultyComparator;

import java.util.List;

@PlanningEntity(difficultyComparatorClass = CourseDifficultyComparator.class)
public class CourseSchedule {
    /**
     * Course name
     */
    private String name;
    /**
     * Course slots
     */
    private int slotsNum;
    /**
     * Batch
     */
    private Batch batch;

    /**
     * Domain or valueRangeProviders
     */
    private List<Teacher> teacherList;
    private List<DateTimeSlots> dateTimeSlotsList;

    @PlanningVariable(valueRangeProviderRefs = {"teacherRange"})
    private Teacher teacher;
    @PlanningVariable(valueRangeProviderRefs = {"dateSlotsRange"})
    private DateTimeSlots dateTimeSlots;


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Batch getBatch() {
        return batch;
    }

    public void setBatch(Batch batch) {
        this.batch = batch;
    }

    @ValueRangeProvider(id = "teacherRange")
    public List<Teacher> getTeacherList() {
        return teacherList;
    }

    public void setTeacherList(List<Teacher> teacherList) {
        this.teacherList = teacherList;
    }

    @ValueRangeProvider(id = "dateSlotsRange")
    public List<DateTimeSlots> getDateTimeSlotsList() {
        return dateTimeSlotsList;
    }

    public void setDateTimeSlotsList(List<DateTimeSlots> dateTimeSlotsList) {
        this.dateTimeSlotsList = dateTimeSlotsList;
    }

    public Teacher getTeacher() {
        return teacher;
    }

    public void setTeacher(Teacher teacher) {
        this.teacher = teacher;
    }

    public DateTimeSlots getDateTimeSlots() {
        return dateTimeSlots;
    }

    public void setDateTimeSlots(DateTimeSlots dateTimeSlots) {
        this.dateTimeSlots = dateTimeSlots;
    }

    public int getSlotsNum() {
        return slotsNum;
    }

    public void setSlotsNum(int slotsNum) {
        this.slotsNum = slotsNum;
    }
}
