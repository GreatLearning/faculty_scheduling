package org.optaplanner.examples.greatlearning.solver.score;

import org.apache.commons.collections.CollectionUtils;
import org.optaplanner.core.api.score.buildin.hardmediumsoft.HardMediumSoftScore;
import org.optaplanner.core.impl.score.director.easy.EasyScoreCalculator;
import org.optaplanner.examples.greatlearning.domain.*;

import java.time.LocalDate;
import java.util.*;

public class GLCalendarScoreCalculator implements EasyScoreCalculator<GLCalendar> {
    @Override
    public HardMediumSoftScore calculateScore(GLCalendar solution) {
        int hardScore = 0;
        int mediumScore = 0;
        int softScore = 0;

        Map<DateTimeSlot, Map<String, Set<String>>> courseDateTimeSlotMap = new HashMap<>(); //DateTimeSlot vs batchName vs Courses
        Map<DateTimeSlot, Map<String, Set<String>>> teacherDateTimeSlotMap = new HashMap<>(); //DateTimeSlot vs teacher vs batchName
        Map<LocalDate, Map<Teacher, Set<String>>> teacherLocationDateMap = new HashMap<>(); //Date vs teacher vs location


        List<CourseSchedule> courseScheduleList = solution.getCourseScheduleList();
        for (CourseSchedule courseSchedule : courseScheduleList) {
            populateCourseDateTimeSlot(courseDateTimeSlotMap, courseSchedule);
            populateTeacherDateTimeSlot(teacherDateTimeSlotMap, courseSchedule);
            populateTeacherLocationDates(teacherLocationDateMap, courseSchedule);

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
        /**
         * 5. A faculty should not be teaching more than 1 location on same day
         * decrementing hardScore by #conflicting locations
         *
         * 8. Faculty unavailable days (on leave )
         */
        for (Map.Entry<LocalDate, Map<Teacher, Set<String>>> entry : teacherLocationDateMap.entrySet()) {
            for (Map.Entry<Teacher, Set<String>> setEntry : entry.getValue().entrySet()) {
                if (setEntry.getValue().size() > 1) {
                    hardScore -= setEntry.getValue().size();
                }
                List<LocalDate> holidays = setEntry.getKey().getHolidays();
                if (holidays != null && setEntry.getKey().getHolidays().contains(entry.getKey())) {
                    hardScore--;
                }
                Set<String> locations = setEntry.getValue();
                if (locations == null) {
                    locations = new HashSet<>();
                }
                Set<String> restrictedLocations = setEntry.getKey().getRestrictedLocations();
                if (restrictedLocations == null) {
                    restrictedLocations = new HashSet<>();
                }
                Collection overlappingLocations = CollectionUtils.intersection(locations, restrictedLocations);
                hardScore -= overlappingLocations.size();
            }
        }

        return HardMediumSoftScore.valueOf(hardScore, mediumScore, softScore);
    }

    private void populateTeacherLocationDates(Map<LocalDate, Map<Teacher, Set<String>>> teacherLocationDateMap, CourseSchedule courseSchedule) {
        DateTimeSlots dateTimeSlots = courseSchedule.getDateTimeSlots();
        if (dateTimeSlots == null) {
            dateTimeSlots = new DateTimeSlots();
            dateTimeSlots.setDateTimeSlots(new ArrayList<>());
        }
        for (DateTimeSlot dateTimeSlot : dateTimeSlots.getDateTimeSlots()) {
            String location = courseSchedule.getBatch().getLocation().getName();
            Teacher teacherName = courseSchedule.getTeacher();
            Map<Teacher, Set<String>> map = teacherLocationDateMap.get(dateTimeSlot.getDate());
            if (map == null) {
                map = new HashMap<>();
            }
            Set<String> locationNames = map.get(teacherName);
            if (locationNames == null) {
                locationNames = new HashSet<>();
            }
            locationNames.add(location);
            map.put(teacherName, locationNames);
            teacherLocationDateMap.put(dateTimeSlot.getDate(), map);
        }
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
