package org.optaplanner.examples.greatlearning.solver.score;

import org.apache.commons.collections.CollectionUtils;
import org.optaplanner.core.api.score.buildin.hardmediumsoft.HardMediumSoftScore;
import org.optaplanner.core.impl.score.director.easy.EasyScoreCalculator;
import org.optaplanner.examples.greatlearning.domain.*;
import org.optaplanner.examples.greatlearning.util.Util;

import java.time.LocalDate;
import java.util.*;

public class GLCalendarScoreCalculator implements EasyScoreCalculator<GLCalendar> {
    @Override
    public HardMediumSoftScore calculateScore(GLCalendar solution) {
        int hardScore = 0;
        int mediumScore = 0;
        int softScore = 0;
        List<String> constraintsBoken = new ArrayList<>();

        Map<DateTimeSlot, Map<String, Set<String>>> courseDateTimeSlotMap = new HashMap<>(); //DateTimeSlot vs batchName vs Courses
        Map<DateTimeSlot, Map<String, Set<String>>> teacherDateTimeSlotMap = new HashMap<>(); //DateTimeSlot vs teacher vs batchName
        Map<LocalDate, Map<Teacher, Set<String>>> teacherLocationDateMap = new HashMap<>(); //Date vs teacher vs location
        Map<DateTimeSlot, Map<Location, Set<String>>> locationDateMap = new HashMap<>(); //DateTimeSlot vs location vs batches

        List<CourseSchedule> courseScheduleList = solution.getCourseScheduleList();
        for (CourseSchedule courseSchedule : courseScheduleList) {
            populateCourseDateTimeSlot(courseDateTimeSlotMap, courseSchedule);
            populateTeacherDateTimeSlot(teacherDateTimeSlotMap, courseSchedule);
            populateTeacherLocationDates(teacherLocationDateMap, courseSchedule);
            populateLocationBatchDateTimeSlot(locationDateMap, courseSchedule);
        }

        /**
         * //3. Only one course per slot of a batch
         * decrementing hardScore by #conflicting courses
         */
        for (Map.Entry<DateTimeSlot, Map<String, Set<String>>> entry : courseDateTimeSlotMap.entrySet()) {
            for (Map.Entry<String, Set<String>> setEntry : entry.getValue().entrySet()) {
                if (setEntry.getValue().size() > 1) {
                    hardScore -= setEntry.getValue().size();
                    constraintsBoken.add("#3. #conflicting courses for batch");
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
                    constraintsBoken.add("#4. #conflicting batches for faculty");
                }
            }
        }
        /**
         * 5. A faculty should not be teaching more than 1 location on same day
         * decrementing hardScore by #conflicting locations
         *
         * 8. Faculty unavailable days (on leave )
         * 10. Faculty restricted location ( example : can’t teach in gurgaon )
         */
        for (Map.Entry<LocalDate, Map<Teacher, Set<String>>> entry : teacherLocationDateMap.entrySet()) {
            for (Map.Entry<Teacher, Set<String>> setEntry : entry.getValue().entrySet()) {
                if (setEntry.getValue().size() > 1) {
                    hardScore -= setEntry.getValue().size();
                    constraintsBoken.add("#5. A faculty should not be teaching more than 1 location on same day");
                }
                Set<LocalDate> holidays = setEntry.getKey().getHolidays();
                if (holidays != null && setEntry.getKey().getHolidays().contains(entry.getKey())) {
                    hardScore--;
                    constraintsBoken.add("#8. Faculty unavailable days (on leave )");
                }
                Set<String> locations = setEntry.getValue();
                if (locations == null) {
                    locations = new HashSet<>();
                }
                Set<String> availableLocations = setEntry.getKey().getAvailableLocations();
                if (availableLocations == null) {
                    availableLocations = new HashSet<>();
                }
                Collection allLocations = CollectionUtils.union(locations, availableLocations);
                if(allLocations.size() > availableLocations.size()){
                    hardScore -= allLocations.size() - availableLocations.size();
                    constraintsBoken.add("#10. Faculty restricted location");
                }
            }
        }

        /**
         * 6. No. of residency for a location on same day should not exceed number of rooms.
         */
        for (Map.Entry<DateTimeSlot, Map<Location, Set<String>>> entry : locationDateMap.entrySet()) {
            for (Map.Entry<Location, Set<String>> setEntry : entry.getValue().entrySet()) {
                if (setEntry.getValue().size() > setEntry.getKey().getRooms()) {
                    hardScore -= (setEntry.getValue().size() - setEntry.getKey().getRooms());
                    constraintsBoken.add("#6. No. of residency for a location on same day should not exceed number of rooms.");
                }
            }
        }

        /**
         * 7. faculty stickiness ( if a faculty starts teaching a course, then only he should continue )
         *9. Course only in one slot in any day
         * Above should be handled by input generator
         */

        Map<Batch, Map<String, Map.Entry<Integer, Integer>>> inflightCoursesTracker = new HashMap<>();

        Map<DateTimeSlot, List<CourseSchedule>> calendar = Util.convertToCalendar(solution);
        Map<Batch, Integer> batchInflightMap = new HashMap<>();

        /**
         * #1. Max inflight course violations
         */
        for (Map.Entry<DateTimeSlot, List<CourseSchedule>> entry : calendar.entrySet()) {
            List<CourseSchedule> scheduleList = entry.getValue();
            for (CourseSchedule schedule : scheduleList) {
                Batch batch = schedule.getBatch();
                String courseName = schedule.getName();
                Map<String, Map.Entry<Integer, Integer>> item = inflightCoursesTracker.get(batch);
                if (item == null) {
                    item = new HashMap<>();
                }
                Map.Entry<Integer, Integer> integerEntry = item.get(courseName);
                if (integerEntry == null) {
                    integerEntry = new AbstractMap.SimpleEntry<>(schedule.getSlotsNum(), 0);
                }
                int currInflight = integerEntry.getValue();
                currInflight++;
                integerEntry.setValue(currInflight);

                item.put(courseName, integerEntry);
                inflightCoursesTracker.put(batch, item);

                //Check for inflight violation
                int counter = 0;
                for (Map.Entry<String, Map.Entry<Integer, Integer>> entryEntry : item.entrySet()) {
                    int inflight = entryEntry.getValue().getValue();
                    int threshold = entryEntry.getValue().getKey();

                    if (inflight < threshold) {
                        counter++;
                    }
                }
                int inflightViolation = 0;
                if (counter > batch.getMaxCoursesInFLight()) {
                    inflightViolation = counter;
                }
                batchInflightMap.put(batch, inflightViolation);
            }
        }
        for (Map.Entry<Batch, Integer> entry : batchInflightMap.entrySet()) {
            if (entry.getValue() > 0) {
                hardScore--;
                constraintsBoken.add("#1. Max inflight course violations");
            }
        }

        solution.setConstraintsBroken(constraintsBoken);

        return HardMediumSoftScore.valueOf(hardScore, mediumScore, softScore);
    }

    private void populateLocationBatchDateTimeSlot(Map<DateTimeSlot, Map<Location, Set<String>>> locationDateMap, CourseSchedule courseSchedule) {

        DateTimeSlots dateTimeSlots = getDateTimeSlots(courseSchedule);
        for (DateTimeSlot dateTimeSlot : dateTimeSlots.getDateTimeSlots()) {
            Location location = courseSchedule.getBatch().getLocation();
            String batchName = courseSchedule.getBatch().getName();
            Map<Location, Set<String>> map = locationDateMap.get(dateTimeSlot);
            if (map == null) {
                map = new HashMap<>();
            }
            Set<String> batchNames = map.get(location);
            if (batchNames == null) {
                batchNames = new HashSet<>();
            }
            batchNames.add(batchName);
            map.put(location, batchNames);
            locationDateMap.put(dateTimeSlot, map);
        }

    }

    private DateTimeSlots getDateTimeSlots(CourseSchedule courseSchedule) {
        DateTimeSlots dateTimeSlots = courseSchedule.getDateTimeSlots();
        if (dateTimeSlots == null) {
            dateTimeSlots = new DateTimeSlots();
            dateTimeSlots.setDateTimeSlots(new ArrayList<>());
        }
        return dateTimeSlots;
    }

    private void populateTeacherLocationDates(Map<LocalDate, Map<Teacher, Set<String>>> teacherLocationDateMap, CourseSchedule courseSchedule) {
        DateTimeSlots dateTimeSlots = getDateTimeSlots(courseSchedule);
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
        DateTimeSlots dateTimeSlots = getDateTimeSlots(courseSchedule);
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
        DateTimeSlots dateTimeSlots = getDateTimeSlots(courseSchedule);
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
