package org.optaplanner.examples.greatlearning.solver.score;

import org.optaplanner.core.api.score.buildin.hardmediumsoft.HardMediumSoftScore;
import org.optaplanner.core.impl.score.director.easy.EasyScoreCalculator;
import org.optaplanner.examples.greatlearning.domain.CourseSchedule;
import org.optaplanner.examples.greatlearning.domain.DateTimeSlot;
import org.optaplanner.examples.greatlearning.domain.DateTimeSlots;
import org.optaplanner.examples.greatlearning.domain.GLCalendar;

import java.util.*;

public class GLCalendarScoreCalculator implements EasyScoreCalculator<GLCalendar> {
    @Override
    public HardMediumSoftScore calculateScore(GLCalendar solution) {
        int hardScore = 0;
        int mediumScore = 0;
        int softScore = 0;

        Map<DateTimeSlot, Map<String, Set<String>>> courseDateTimeSlotMap = new HashMap<>(); //DateTimeSlot vs batchName vs Courses
        Map<DateTimeSlot, Map<String, Set<String>>> teacherDateTimeSlotMap = new HashMap<>();


        List<CourseSchedule> courseScheduleList = solution.getCourseScheduleList();
        for (CourseSchedule courseSchedule : courseScheduleList) {
            populateCourseDateTimeSlot(courseDateTimeSlotMap, courseSchedule);
            populateTeacherDateTimeSlot(teacherDateTimeSlotMap, courseSchedule);

        }

        /**
         * //3. Only one course per slot of a batch
         * decrementing hardScore by #conflicting courses
         */
        for (Map.Entry<DateTimeSlot, Map<String, Set<String>>> entry : courseDateTimeSlotMap.entrySet()) {
            for (Map.Entry<String, Set<String>> setEntry : entry.getValue().entrySet()) {
                if (setEntry.getValue().size() > 1) {
                    hardScore -= setEntry.getValue().size();
                }
            }
        }
        /**
         * 4. Faculty should not be teaching more than 1 batch in a slot
         * decrementing hardScore by #conflicting batches
         */
        for (Map.Entry<DateTimeSlot, Map<String, Set<String>>> entry : teacherDateTimeSlotMap.entrySet()) {
            for (Map.Entry<String, Set<String>> setEntry : entry.getValue().entrySet()) {
                if (setEntry.getValue().size() > 1) {
                    hardScore -= setEntry.getValue().size();
                }
            }
        }

        return HardMediumSoftScore.valueOf(hardScore, mediumScore, softScore);
    }

    private void populateTeacherDateTimeSlot(Map<DateTimeSlot, Map<String, Set<String>>> teacherDateTimeSlotMap, CourseSchedule courseSchedule) {
        DateTimeSlots dateTimeSlots = courseSchedule.getDateTimeSlots();
        if (dateTimeSlots == null) {
            dateTimeSlots = new DateTimeSlots();
            dateTimeSlots.setDateTimeSlots(new ArrayList<>());
        }
        for (DateTimeSlot dateTimeSlot : dateTimeSlots.getDateTimeSlots()) {
            String batchName = courseSchedule.getBatch().getName();
            String teacherName = courseSchedule.getTeacher().getName();
            Map<String, Set<String>> map = teacherDateTimeSlotMap.get(dateTimeSlot);
            if (map == null) {
                map = new HashMap<>();
            }
            Set<String> batchNames = map.get(teacherName);
            if (batchNames == null) {
                batchNames = new HashSet<>();
            }
            batchNames.add(batchName);
            map.put(batchName, batchNames);
            teacherDateTimeSlotMap.put(dateTimeSlot, map);
        }
    }

    private void populateCourseDateTimeSlot(Map<DateTimeSlot, Map<String, Set<String>>> courseDateTimeSlotMap, CourseSchedule courseSchedule) {
        DateTimeSlots dateTimeSlots = courseSchedule.getDateTimeSlots();
        if (dateTimeSlots == null) {
            dateTimeSlots = new DateTimeSlots();
            dateTimeSlots.setDateTimeSlots(new ArrayList<>());
        }
        for (DateTimeSlot dateTimeSlot : dateTimeSlots.getDateTimeSlots()) {
            String courseName = courseSchedule.getName();
            String batchName = courseSchedule.getBatch().getName();

            Map<String, Set<String>> map = courseDateTimeSlotMap.get(dateTimeSlot);
            if (map == null) {
                map = new HashMap<>();
            }
            Set<String> courseNames = map.get(batchName);
            if (courseNames == null) {
                courseNames = new HashSet<>();
            }
            courseNames.add(courseName);
            map.put(batchName, courseNames);
            courseDateTimeSlotMap.put(dateTimeSlot, map);
        }
    }
}
