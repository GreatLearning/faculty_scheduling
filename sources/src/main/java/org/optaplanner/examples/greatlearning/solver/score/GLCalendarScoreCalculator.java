package org.optaplanner.examples.greatlearning.solver.score;

import org.apache.commons.collections.CollectionUtils;
import org.optaplanner.core.api.score.buildin.hardmediumsoft.HardMediumSoftScore;
import org.optaplanner.core.impl.score.director.easy.EasyScoreCalculator;
import org.optaplanner.examples.greatlearning.domain.*;
import org.optaplanner.examples.greatlearning.util.Util;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

import static java.time.temporal.ChronoField.EPOCH_DAY;

public class GLCalendarScoreCalculator implements EasyScoreCalculator<GLCalendar> {
    private static AtomicLong counter = new AtomicLong(0);

    @Override
    public HardMediumSoftScore calculateScore(GLCalendar solution) {
//        long startTime = System.currentTimeMillis();

        int hardScore = 0;
        int mediumScore = 0;
        int softScore = 0;
        List<String> constraintsBroken = new ArrayList<>();

        Map<DateTimeSlot, Map<String, Set<String>>> courseDateTimeSlotMap = new HashMap<>(); //DateTimeSlot vs batchName vs Courses
        Map<DateTimeSlot, Map<String, Set<String>>> teacherDateTimeSlotMap = new HashMap<>(); //DateTimeSlot vs teacher vs batchName
        Map<LocalDate, Map<Teacher, Set<Batch>>> teacherLocationDateMap = new HashMap<>(); //Date vs teacher vs location
        Map<DateTimeSlot, Map<Location, Set<String>>> locationDateMap = new HashMap<>(); //DateTimeSlot vs location vs batches
        Map<Batch, Map<DateTimeSlot, Set<String>>> courseOrderingMap = new HashMap<>();
        Map<Teacher, Map<DateTimeSlot, Location>> teacherLocationMap = new HashMap<>();

        List<CourseSchedule> courseScheduleList = solution.getCourseScheduleList();
        for (CourseSchedule courseSchedule : courseScheduleList) {
            populateCourseDateTimeSlot(courseDateTimeSlotMap, courseSchedule);
            populateTeacherDateTimeSlot(teacherDateTimeSlotMap, courseSchedule);
            populateTeacherLocationDates(teacherLocationDateMap, courseSchedule);
            populateLocationBatchDateTimeSlot(locationDateMap, courseSchedule);
            populateTeacherLocationMap(teacherLocationMap, courseSchedule);
            populateCourseOrderingMap(courseOrderingMap, courseSchedule);

        }

//        System.out.println("#1 GLCalendarScoreCalculator Time Taken " + (System.currentTimeMillis() - startTime) + " ms");
        /**
         * Medium. 1. Order of courses  ( deviation from original course order )
         */


        for (Map.Entry<Batch, Map<DateTimeSlot, Set<String>>> entry : courseOrderingMap.entrySet()) {

            Map<String, Integer> courseIndices = entry.getKey().getProgram().getCourseIndices();

            List<String> actualCourseList = new ArrayList<>();
            entry.getValue().values().forEach(actualCourseList::addAll);
            actualCourseList = new ArrayList<>(new LinkedHashSet<>(actualCourseList));

            for (int i = 1; i < actualCourseList.size(); i++) {
                if (courseIndices.get(actualCourseList.get(i)) < courseIndices.get(actualCourseList.get(i - 1))) {
                    mediumScore--;
                    constraintsBroken.add("Meduim #1.  Order of courses " + actualCourseList + " >> " + entry.getKey().getName());
                }
            }

            /**
             * #14. Check min/max gap between 2 residencies
             */
            Map<DateTimeSlot, Set<String>> dateTimeSlotSetMap = entry.getValue();
            LocalDate prevResidencyDate = null;
            Batch batch = entry.getKey();
            List<DateTimeSlot> keySets = new ArrayList<>(dateTimeSlotSetMap.keySet());
            Collections.sort(keySets);

            for (DateTimeSlot dateTimeSlot : keySets) {
                if (prevResidencyDate == null) {
                    prevResidencyDate = dateTimeSlot.getDate();
                } else {
                    long daysBetween = ChronoUnit.DAYS.between(prevResidencyDate, dateTimeSlot.getDate());
                    if (daysBetween > 1) {
                        if (batch.getMinGapBetweenResidenciesInDays() > daysBetween) {
                            hardScore -= (batch.getMinGapBetweenResidenciesInDays() - daysBetween);
                            constraintsBroken.add("#14. Check min  gap between 2 residencies " +
                                    entry.getKey().getName() + " - " + prevResidencyDate + "  > " + dateTimeSlot.getDate() + " : " + daysBetween);
                        }
                        if (batch.getMaxGapBetweenResidenciesInDays() < daysBetween) {
                            List<LocalDate> batchHolidays = batch.getLocation().getHolidays();
                            List<Long> epochDays = new ArrayList<>();
                            for(LocalDate localDate : batchHolidays){
                                if(prevResidencyDate.compareTo(localDate) < 0 && localDate.compareTo(dateTimeSlot.getDate()) < 0){
                                    epochDays.add(localDate.getLong(EPOCH_DAY));
                                }
                            }
                            long diff = 7;

                            Collections.sort(epochDays);

                            for (int i = 1; i < epochDays.size(); i++) {
                                if(epochDays.get(i) - epochDays.get(i-1) > 1){
                                    diff += 7;
                                }
                            }
                            if(batch.getMaxGapBetweenResidenciesInDays() < (daysBetween-diff)){
                                hardScore -= (daysBetween - batch.getMaxGapBetweenResidenciesInDays());
                                constraintsBroken.add("#14. Check max gap between 2 residencies " +
                                        entry.getKey().getName() + " - " + prevResidencyDate + "  > " + dateTimeSlot.getDate() + " : " + daysBetween);
                            }
                        }
                    }
                    prevResidencyDate = dateTimeSlot.getDate();
                }
            }
        }

        /**
         * //3. Only one course per slot of a batch
         * decrementing hardScore by #conflicting courses
         */
        for (Map.Entry<DateTimeSlot, Map<String, Set<String>>> entry : courseDateTimeSlotMap.entrySet()) {
            for (Map.Entry<String, Set<String>> setEntry : entry.getValue().entrySet()) {
                if (setEntry.getValue().size() > 1) {
                    hardScore -= setEntry.getValue().size();
                    constraintsBroken.add("#3. #conflicting courses for batch " + entry.getKey() + " ; " + setEntry);
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
                    constraintsBroken.add("#4. #conflicting batches for faculty");
                }
            }
        }
        /**
         * 5. A faculty should not be teaching more than 1 location on same day
         * decrementing hardScore by #conflicting locations
         *
         * 8. Faculty unavailable days (on leave )
         * 10. Faculty restricted location ( example : can’t teach in gurgaon )
         * 15 . Faculty should not be teaching more than 1 batch on same day
         */
        for (Map.Entry<LocalDate, Map<Teacher, Set<Batch>>> entry : teacherLocationDateMap.entrySet()) {
            for (Map.Entry<Teacher, Set<Batch>> setEntry : entry.getValue().entrySet()) {

                Set<String> locations = new HashSet<>();
                for(Batch batch : setEntry.getValue()){
                    locations.add(batch.getLocation().getName());
                }

                if(setEntry.getValue().size() > 1){
                    hardScore -= setEntry.getValue().size();
                    constraintsBroken.add("#15. A faculty should not be teaching more than 1 batch on same day " + setEntry.getKey().getName());
                }

                if (locations.size() > 1) {
                    hardScore -= locations.size();
                    constraintsBroken.add("#5. A faculty should not be teaching more than 1 location on same day " + setEntry.getKey().getName());
                }

                Set<LocalDate> holidays = setEntry.getKey().getHolidays();
                if (holidays != null && setEntry.getKey().getHolidays().contains(entry.getKey())) {
                    hardScore--;
                    constraintsBroken.add("#8. Faculty unavailable days (on leave )");
                }
                Set<String> availableLocations = setEntry.getKey().getAvailableLocations();
                if (availableLocations == null) {
                    availableLocations = new HashSet<>();
                }
                Collection allLocations = CollectionUtils.union(locations, availableLocations);
                if (allLocations.size() > availableLocations.size()) {
                    hardScore -= allLocations.size() - availableLocations.size();
                    constraintsBroken.add("#10. Faculty restricted location");
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
                    constraintsBroken.add("#6. No. of residency for a location on same day should not exceed number of rooms.");
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
         * #13.No more than 1 free slot in a residency
         */
        int violations = 0;
        DateTimeSlot lastDateTimeSlot = null;

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
            if (lastDateTimeSlot == null) {
                lastDateTimeSlot = entry.getKey();
            } else {
                DateTimeSlot currDateTimeSlot = entry.getKey();
                long days = ChronoUnit.DAYS.between(lastDateTimeSlot.getDate(), currDateTimeSlot.getDate());
                if (days <= 1) {
                    if (lastDateTimeSlot.getTimeSlot() == TimeSlot.AFTERNOON && currDateTimeSlot.getTimeSlot() == TimeSlot.AFTERNOON ||
                            lastDateTimeSlot.getTimeSlot() == TimeSlot.MORNING && currDateTimeSlot.getTimeSlot() == TimeSlot.MORNING ||
                            lastDateTimeSlot.getTimeSlot() == TimeSlot.MORNING && currDateTimeSlot.getTimeSlot() == TimeSlot.AFTERNOON) {
                        violations++;
                    }
                    if (violations > 1) {
                        hardScore--;
                        constraintsBroken.add("#13.No more than 1 free slot in a residency");
                    }
                } else {
                    violations = 0;
                }
            }
        }
        for (Map.Entry<Batch, Integer> entry : batchInflightMap.entrySet()) {
            if (entry.getValue() > 0) {
                hardScore--;
                constraintsBroken.add("#1. Max inflight course violations");
            }
        }

        /**
         * Medium :: 3. A faculty should not be teaching more than 1 location on consecutive day
         */
        for (Map.Entry<Teacher, Map<DateTimeSlot, Location>> entry : teacherLocationMap.entrySet()) {
            Map<DateTimeSlot, Location> dateTimeSlotLocationMap = entry.getValue();
            List<Map.Entry<DateTimeSlot, Location>> entrySet = new ArrayList<>(dateTimeSlotLocationMap.entrySet());
            Collections.sort(entrySet, new Comparator<Map.Entry<DateTimeSlot, Location>>() {
                @Override
                public int compare(Map.Entry<DateTimeSlot, Location> o1, Map.Entry<DateTimeSlot, Location> o2) {
                    return o1.getKey().compareTo(o2.getKey());
                }
            });

            for (int i = 1; i < entrySet.size(); i++) {
                DateTimeSlot preDateTimeSlot = entrySet.get(i - 1).getKey();
                DateTimeSlot currDateTimeSlot = entrySet.get(i).getKey();
                if (ChronoUnit.DAYS.between(preDateTimeSlot.getDate(), currDateTimeSlot.getDate()) <= 1) {
                    Location preLocation = entrySet.get(i - 1).getValue();
                    Location currLocation = entrySet.get(i).getValue();

                    if (!preLocation.equals(currLocation)) {
                        mediumScore--;
                        constraintsBroken.add("Medium :: 3. A faculty should not be teaching more than 1 location on consecutive day " + entrySet);
                    }
                }
            }
        }

        solution.setConstraintsBroken(constraintsBroken);

//        long endTime = System.currentTimeMillis();
//
//        System.out.println("GLCalendarScoreCalculator Time Taken " + (endTime - startTime) + " ms");

        if (counter.incrementAndGet() % 100000 == 0) {
            System.out.println("GLCalendarScoreCalculator " + counter.get());
        }
        return HardMediumSoftScore.valueOf(hardScore, mediumScore, softScore);
    }

    private void populateTeacherLocationMap(Map<Teacher, Map<DateTimeSlot, Location>> teacherLocationMap, CourseSchedule courseSchedule) {
        DateTimeSlots dateTimeSlots = getDateTimeSlots(courseSchedule);
        Batch batch = courseSchedule.getBatch();
        Location location = batch.getLocation();
        Teacher teacher = courseSchedule.getTeacher();
        Map<DateTimeSlot, Location> locationMap = teacherLocationMap.get(teacher);
        if (locationMap == null) {
            locationMap = new HashMap<>();
        }
        for (DateTimeSlot dateTimeSlot : dateTimeSlots.getDateTimeSlots()) {
            locationMap.put(dateTimeSlot, location);
        }
        if(teacher != null) {
            teacherLocationMap.put(teacher, locationMap);
        }
    }

    private void populateCourseOrderingMap(Map<Batch, Map<DateTimeSlot, Set<String>>> courseOrderingMap, CourseSchedule courseSchedule) {
        DateTimeSlots dateTimeSlots = getDateTimeSlots(courseSchedule);
        Batch batch = courseSchedule.getBatch();
        String courseName = courseSchedule.getName();
        Map<DateTimeSlot, Set<String>> dateTimeSlotSetMap = courseOrderingMap.get(batch);
        if (dateTimeSlotSetMap == null) {
            dateTimeSlotSetMap = new TreeMap<>();
        }
        for (DateTimeSlot dateTimeSlot : dateTimeSlots.getDateTimeSlots()) {
            Set<String> courseNames = dateTimeSlotSetMap.get(dateTimeSlot);
            if (courseNames == null) {
                courseNames = new LinkedHashSet<>();
            }
            courseNames.add(courseName);
            dateTimeSlotSetMap.put(dateTimeSlot, courseNames);
        }
        courseOrderingMap.put(batch, dateTimeSlotSetMap);
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

    private void populateTeacherLocationDates(Map<LocalDate, Map<Teacher, Set<Batch>>> teacherLocationDateMap, CourseSchedule courseSchedule) {
        DateTimeSlots dateTimeSlots = getDateTimeSlots(courseSchedule);
        for (DateTimeSlot dateTimeSlot : dateTimeSlots.getDateTimeSlots()) {
            Batch batch = courseSchedule.getBatch();

            Teacher teacherName = courseSchedule.getTeacher();
            Map<Teacher, Set<Batch>> map = teacherLocationDateMap.get(dateTimeSlot.getDate());
            if (map == null) {
                map = new HashMap<>();
            }
            Set<Batch> batches = map.get(teacherName);
            if (batches == null) {
                batches = new HashSet<>();
            }
            batches.add(batch);
            map.put(teacherName, batches);
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
