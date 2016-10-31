package org.optaplanner.examples.greatlearning.domain;

import java.time.LocalDate;
import java.util.List;

public class Program {
    private String name;
    private List<Course> courseList;
    private LocalDate startDate;
    private List<Integer> monthlyResidencyDays;

    private int minGapBetweenResidenciesInDays;
    private int maxGapBetweenResidenciesInDays;


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Course> getCourseList() {
        return courseList;
    }

    public void setCourseList(List<Course> courseList) {
        this.courseList = courseList;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    @Override
    public String toString() {
        return "Program{" +
                "name='" + name + '\'' +
                ", courseList=" + courseList +
                ", startDate=" + startDate +
                '}';
    }

    public List<Integer> getMonthlyResidencyDays() {
        return monthlyResidencyDays;
    }

    public void setMonthlyResidencyDays(List<Integer> monthlyResidencyDays) {
        this.monthlyResidencyDays = monthlyResidencyDays;
    }

    public int getMinGapBetweenResidenciesInDays() {
        return minGapBetweenResidenciesInDays;
    }

    public void setMinGapBetweenResidenciesInDays(int minGapBetweenResidenciesInDays) {
        this.minGapBetweenResidenciesInDays = minGapBetweenResidenciesInDays;
    }

    public int getMaxGapBetweenResidenciesInDays() {
        return maxGapBetweenResidenciesInDays;
    }

    public void setMaxGapBetweenResidenciesInDays(int maxGapBetweenResidenciesInDays) {
        this.maxGapBetweenResidenciesInDays = maxGapBetweenResidenciesInDays;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Program program = (Program) o;

        if (minGapBetweenResidenciesInDays != program.minGapBetweenResidenciesInDays) return false;
        if (maxGapBetweenResidenciesInDays != program.maxGapBetweenResidenciesInDays) return false;
        if (name != null ? !name.equals(program.name) : program.name != null) return false;
        if (courseList != null ? !courseList.equals(program.courseList) : program.courseList != null) return false;
        if (startDate != null ? !startDate.equals(program.startDate) : program.startDate != null) return false;
        return monthlyResidencyDays != null ? monthlyResidencyDays.equals(program.monthlyResidencyDays) : program.monthlyResidencyDays == null;

    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (courseList != null ? courseList.hashCode() : 0);
        result = 31 * result + (startDate != null ? startDate.hashCode() : 0);
        result = 31 * result + (monthlyResidencyDays != null ? monthlyResidencyDays.hashCode() : 0);
        result = 31 * result + minGapBetweenResidenciesInDays;
        result = 31 * result + maxGapBetweenResidenciesInDays;
        return result;
    }
}
