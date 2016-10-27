package org.optaplanner.examples.gl.solver.score;

import org.apache.commons.collections.CollectionUtils;
import org.optaplanner.core.api.score.buildin.hardmediumsoft.HardMediumSoftScore;
import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.core.impl.score.director.easy.EasyScoreCalculator;
import org.optaplanner.examples.gl.domain.*;

import java.time.LocalDate;
import java.time.Period;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Hard
 * 1. all residencies of batch should happen in order  :N
 * 2. No 2 residency of same batch should happen on same day : D
 * 3. A faculty should not be teaching 2 batch on same day (2 different location) :D
 * 4. No. of residency for a location on same day should not exceed number of rooms. : N
 * 5. faculty teaching a course ( assigned to residency ), should be capable of teaching a course. : D
 * 6. honor minimum gap between any 2 consecutive residency : D
 * 7. faculty stickiness ( if a faculty starts teaching a course, then only he should continue ) : N
 * 8. Max gap between 2 consecutive residency ( of same batch ) should be less than a config value  : D
 * 9. Faculty unavailable days (on leave ) : N
 */
public class GLScoreCalculator implements EasyScoreCalculator<GLResidencyCalendar> {

    private int minGapBetweenResidency = 30;
    private int maxGapBetweenResidency = 40;

    @Override
    public HardMediumSoftScore calculateScore(GLResidencyCalendar glResidencyCalendar) {

        int hardScore = 0;
        int softScore = 0;
        int mediumScore = 0;

        List<Residency> residencyList = glResidencyCalendar.getResidencyList();

        Map<String, List<ResidencyDates>> batchResidencySchedule = new HashMap<>();
        Map<Teacher, Map<ResidencyDates, List<Location>>> teachersSchedule = new HashMap<>();


        for (Residency residency : residencyList) {
            String batch = residency.getBatch();
            ResidencyDates residencyDates = residency.getDates();
            List<ResidencyDates> dates = batchResidencySchedule.get(batch);
            if (dates == null) {
                dates = new ArrayList<>();
            }
            if (residencyDates != null) {
                dates.add(residencyDates);
            }
            batchResidencySchedule.put(batch, dates);

            List<Teacher> teachers = new ArrayList<>();
            if (residency.getTeachers() != null && residency.getTeachers().getTeachers() != null) {
                teachers = residency.getTeachers().getTeachers();
            }

            populateTeacherSchedule(teachersSchedule, residency, residencyDates, teachers);

            //5. faculty teaching a course ( assigned to residency ), should be capable of teaching a course.
            hardScore -= facultyCapabilityScore(residency);
        }

        //#2. Check for overlapping residency of same batch ( No 2 residency of same batch should happen on same day )
        for (Map.Entry<String, List<ResidencyDates>> entry : batchResidencySchedule.entrySet()) {
            List<ResidencyDates> residencyDates = entry.getValue();
            residencyDates.sort(new Comparator<ResidencyDates>() {
                @Override
                public int compare(ResidencyDates o1, ResidencyDates o2) {
                    if (o1.getStartDate().compareTo(o2.getStartDate()) == 0) {
                        return o1.getEndDate().compareTo(o2.getEndDate());
                    } else return o1.getStartDate().compareTo(o2.getStartDate());
                }
            });

            int potentialOverlappingCount = 0;
            for (int i = 1; i < residencyDates.size(); i++) {
                int val = residencyDates.get(i - 1).getEndDate().compareTo(residencyDates.get(i).getStartDate());
                if (val > 0) {
                    potentialOverlappingCount++;
                }
            }
            hardScore -= potentialOverlappingCount;

            for (int i = 1; i < residencyDates.size(); i++) {
                ResidencyDates residencyDates1 = residencyDates.get(0);
                ResidencyDates residencyDates2 = residencyDates.get(i);

                Period period = Period.between(residencyDates2.getStartDate(), residencyDates1.getEndDate());

                //6. honor minimum gap between any 2 consecutive residency
                if (period.getDays() < minGapBetweenResidency) {
                    hardScore -= minGapBetweenResidency - period.getDays();
                }
                //8. Max gap between 2 consecutive residency ( of same batch ) should be less than a config value
                if (period.getDays() > maxGapBetweenResidency) {
                    hardScore -= period.getDays() - maxGapBetweenResidency;
                }
            }
        }
        //#3. A faculty should not be teaching 2 batch on same day (2 different location)
        for (Map.Entry<Teacher, Map<ResidencyDates, List<Location>>> entry : teachersSchedule.entrySet()) {
            for (Map.Entry<ResidencyDates, List<Location>> listEntry : entry.getValue().entrySet()) {
                if (listEntry.getValue().size() > 1) {
                    mediumScore -= listEntry.getValue().size();
                }
            }
        }

        //4. No. of residency for a location on same day should not exceed number of rooms. TODO


        return HardMediumSoftScore.valueOf(hardScore, mediumScore, softScore);
    }

    private int facultyCapabilityScore(Residency residency) {
        List<Course> courses = residency.getCourses();
        List<String> courseNames = courses.stream().map(course -> (course.getName())).collect(Collectors.toList());
        List<Teacher> assignedTeachers = new ArrayList<>();
        if (residency.getTeachers() != null && residency.getTeachers().getTeachers() != null) {
            assignedTeachers = residency.getTeachers().getTeachers();
        }

        int score = 0;

        for (Teacher teacher : assignedTeachers) {
            if (CollectionUtils.intersection(teacher.getCanTeachCourses(), courseNames).size() == 0) {
                score++;
            }
        }

        return score;
    }

    private void populateTeacherSchedule(Map<Teacher, Map<ResidencyDates, List<Location>>> teachersSchedule, Residency residency, ResidencyDates residencyDates, List<Teacher> teachers) {
        for (Teacher teacher : teachers) {
            Map<ResidencyDates, List<Location>> schedule = teachersSchedule.get(teacher);
            if (schedule == null) {
                schedule = new HashMap<>();
                List<Location> locations = new ArrayList<>();
                locations.add(residency.getLocation());
                schedule.put(residencyDates, locations);
                teachersSchedule.put(teacher, schedule);
            } else {
                for (Map.Entry<ResidencyDates, List<Location>> entry : schedule.entrySet()) {
                    if (!(entry.getKey().getEndDate().compareTo(residencyDates.getStartDate()) < 0
                            || entry.getKey().getStartDate().compareTo(residencyDates.getEndDate()) > 0)) {

                        LocalDate newStartDate = entry.getKey().getStartDate();
                        LocalDate newEndDate = entry.getKey().getEndDate();

                        if (newStartDate.compareTo(residencyDates.getStartDate()) < -1) {
                            newStartDate = residencyDates.getStartDate();
                        }
                        if (newEndDate.compareTo(residencyDates.getEndDate()) < -1) {
                            newEndDate = residencyDates.getEndDate();
                        }

                        List<Location> locations = entry.getValue();
                        locations.add(residency.getLocation());

                        entry.getKey().setEndDate(newStartDate);
                        entry.getKey().setEndDate(newEndDate);
                    }
                }

            }
        }
    }
}
