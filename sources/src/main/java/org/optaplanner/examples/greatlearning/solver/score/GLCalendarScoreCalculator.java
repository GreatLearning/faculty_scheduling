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
    private static AtomicLong timer = new AtomicLong(0);

    @Override
    public HardMediumSoftScore calculateScore(GLCalendar solution) {

        long start = System.currentTimeMillis();

        int hardScore = 0;
        int mediumScore = 0;
        int softScore = 0;
        List<String> constraintsBroken = new ArrayList<>();

        Map<DateTimeSlot, Map<String, Set<String>>> courseDateTimeSlotMap = new HashMap<>(); //DateTimeSlot vs batchName vs Courses
        Map<DateTimeSlot, Map<String, Set<String>>> teacherDateTimeSlotMap = new HashMap<>(); //DateTimeSlot vs teacher vs batchName
        Map<LocalDate, Map<Teacher, Set<Batch>>> teacherLocationDateMap = new HashMap<>(); //Date vs teacher vs location
        Map<DateTimeSlot, Map<Location, Set<String>>> locationDateMap = new HashMap<>(); //DateTimeSlot vs location vs batches
        Map<Batch, Map<DateTimeSlot, Set<String>>> courseOrderingMap = new HashMap<>();
        Map<Teacher, Map<DateTimeSlot, Batch>> teacherLocationMap = new HashMap<>();

        List<CourseSchedule> courseScheduleList = solution.getCourseScheduleList();
        for (CourseSchedule courseSchedule : courseScheduleList) {
            populateCourseDateTimeSlot(courseDateTimeSlotMap, courseSchedule);
            populateTeacherDateTimeSlot(teacherDateTimeSlotMap, courseSchedule);
            populateTeacherLocationDates(teacherLocationDateMap, courseSchedule);
            populateLocationBatchDateTimeSlot(locationDateMap, courseSchedule);
            populateTeacherLocationMap(teacherLocationMap, courseSchedule);
            populateCourseOrderingMap(courseOrderingMap, courseSchedule);

        }

        /**
         * 19. batch should follow [ 3,2,3,2]
         */
        for (Map.Entry<Batch, Map<DateTimeSlot, Set<String>>> entry : courseOrderingMap.entrySet()) {
            Map<DateTimeSlot, Set<String>> dateTimeSlotSetMap = entry.getValue();
            Batch batch = entry.getKey();

            List<Integer> monthlyResidencyDays = batch.getMonthlyResidencyDays();
            List<List<LocalDate>> residencies = new ArrayList<>();

            Set<LocalDate> localDateHashSet = new HashSet<>();
            List<DateTimeSlot> dateTimeSlotList = new ArrayList<>();

            for (DateTimeSlot dateTimeSlot : dateTimeSlotSetMap.keySet()) {
                localDateHashSet.add(dateTimeSlot.getDate());
                dateTimeSlotList.add(dateTimeSlot);
            }
            List<LocalDate> localDateList = new ArrayList<>(localDateHashSet);

            Collections.sort(localDateList);
            Collections.sort(dateTimeSlotList);

            softScore = checkContinousSlots(softScore, constraintsBroken, entry, dateTimeSlotList);

            hardScore = checkMonthlyResidencyDays(hardScore, constraintsBroken, entry, monthlyResidencyDays, residencies, localDateList);
        }

//        System.out.println("#1 GLCalendarScoreCalculator Time Taken " + (System.currentTimeMillis() - startTime) + " ms");
        /**
         * Medium. 1. Order of courses  ( deviation from original course order )
         */


        for (Map.Entry<Batch, Map<DateTimeSlot, Set<String>>> entry : courseOrderingMap.entrySet()) {

            List<String> actualCourseListString = new ArrayList<>();

            for (Course course : entry.getKey().getProgram().getCourseList()) {
                actualCourseListString.add(course.getName());
            }

            Map<String, Integer> courseIndices = entry.getKey().getProgram().getCourseIndices();

            List<String> actualCourseList = new ArrayList<>();
            entry.getValue().values().forEach(actualCourseList::addAll);
            actualCourseList = new ArrayList<>(new LinkedHashSet<>(actualCourseList));

            boolean error = false;
            for (int i = 1; i < actualCourseList.size(); i++) {
                if (courseIndices.get(actualCourseList.get(i)) < courseIndices.get(actualCourseList.get(i - 1))) {
                    hardScore--; //TODO : make it medium back ???
                    error = true;
                }
            }

            if (error) {
                constraintsBroken.add(entry.getKey().getName() + System.lineSeparator() + "expected order : " + actualCourseListString
                        + System.lineSeparator() + "Actual order : " + actualCourseList);
            }

            /**
             * #14. Check min/max gap between 2 residencies
             */
            Map<DateTimeSlot, Set<String>> dateTimeSlotSetMap = entry.getValue();
            LocalDate prevResidencyDate = null;
            Batch batch = entry.getKey();
            List<DateTimeSlot> keySets = new ArrayList<>(dateTimeSlotSetMap.keySet());
            Collections.sort(keySets);

            /**
             * #14. Check min/max gap between residencies
             */
            for (DateTimeSlot dateTimeSlot : keySets) {
                if (prevResidencyDate == null) {
                    prevResidencyDate = dateTimeSlot.getDate();
                } else {
                    long daysBetween = ChronoUnit.DAYS.between(prevResidencyDate, dateTimeSlot.getDate());
                    if (daysBetween > 1) {
                        if (batch.getMinGapBetweenResidenciesInDays() > daysBetween) {
                            hardScore -= (batch.getMinGapBetweenResidenciesInDays() - daysBetween);
                            constraintsBroken.add(entry.getKey().getName() + " >>> #14. Check min  gap between 2 residencies " +
                                    " - " + prevResidencyDate + "  > " + dateTimeSlot.getDate() + " : " + daysBetween);
                        }
                        if (batch.getMaxGapBetweenResidenciesInDays() < daysBetween) {
                            List<LocalDate> batchHolidays = batch.getLocation().getHolidays();
                            List<Long> epochDays = new ArrayList<>();
                            for (LocalDate localDate : batchHolidays) {
                                if (prevResidencyDate.compareTo(localDate) < 0 && localDate.compareTo(dateTimeSlot.getDate()) < 0) {
                                    epochDays.add(localDate.getLong(EPOCH_DAY));
                                }
                            }
                            long diff = 7;

                            Collections.sort(epochDays);

                            for (int i = 1; i < epochDays.size(); i++) {
                                if (epochDays.get(i) - epochDays.get(i - 1) > 1) {
                                    diff += 7;
                                }
                            }
                            if (batch.getMaxGapBetweenResidenciesInDays() < (daysBetween - diff)) {
                                hardScore -= (daysBetween - batch.getMaxGapBetweenResidenciesInDays());
                                constraintsBroken.add(entry.getKey().getName() + " >>> #14. Check max gap between 2 residencies " +
                                        " - " + prevResidencyDate + "  > " + dateTimeSlot.getDate() + " : " + daysBetween);
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
                    hardScore -= 2 * setEntry.getValue().size();
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
                for (Batch batch : setEntry.getValue()) {
                    locations.add(batch.getLocation().getName());
                }

                if (setEntry.getValue().size() > 1) {
                    hardScore -= setEntry.getValue().size();
                    constraintsBroken.add("#15. A faculty should not be teaching more than 1 batch on same day " + setEntry.getKey().getName());
                }

                if (locations.size() > 1) {
                    hardScore -= locations.size();
                    constraintsBroken.add("#5. A faculty should not be teaching more than 1 location on same day " + setEntry.getKey().getName());
                }

                if(setEntry.getKey() != null) {
                    Set<LocalDate> holidays = setEntry.getKey().getHolidays();
                    if (holidays != null && setEntry.getKey().getHolidays().contains(entry.getKey())) {
                        hardScore--;
                        constraintsBroken.add("#8. Faculty unavailable days (on leave ) "  +setEntry.getKey().getName() + " : " + entry.getKey());
                    }
                }
                if(setEntry.getKey() != null) {
                    Set<String> availableLocations = setEntry.getKey().getAvailableLocations();
                    if (availableLocations == null) {
                        availableLocations = new HashSet<>();
                    }
                    Collection allLocations = CollectionUtils.union(locations, availableLocations);
                    if (allLocations.size() > availableLocations.size()) {
                        hardScore -= allLocations.size() - availableLocations.size();
                        constraintsBroken.add("#10. Faculty restricted location " + setEntry.getKey().getName());
                    }
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
                    constraintsBroken.add("#6. No. of residencies in a location on same day should not exceed number of rooms. " + entry.getKey()
                            + ">>" + setEntry.getKey() + ">>>" + setEntry.getValue());
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
                constraintsBroken.add("#1. Max inflight course violations");
            }
        }

        /**
         * Medium :: 3. A faculty should not be teaching more than 1 location on consecutive day
         */
        for (Map.Entry<Teacher, Map<DateTimeSlot, Batch>> entry : teacherLocationMap.entrySet()) {
            Map<DateTimeSlot, Batch> dateTimeSlotLocationMap = entry.getValue();
            List<Map.Entry<DateTimeSlot, Batch>> entrySet = new ArrayList<>(dateTimeSlotLocationMap.entrySet());
            Collections.sort(entrySet, (o1, o2) -> o1.getKey().compareTo(o2.getKey()));

            for (int i = 1; i < entrySet.size(); i++) {
                DateTimeSlot preDateTimeSlot = entrySet.get(i - 1).getKey();
                DateTimeSlot currDateTimeSlot = entrySet.get(i).getKey();
                if (ChronoUnit.DAYS.between(preDateTimeSlot.getDate(), currDateTimeSlot.getDate()) <= 1) {
                    Location preLocation = entrySet.get(i - 1).getValue().getLocation();
                    Location currLocation = entrySet.get(i).getValue().getLocation();

                    if (!preLocation.equals(currLocation)) {
                        mediumScore--;
                        constraintsBroken.add("Medium :: 3. A faculty should not be teaching more than 1 location on consecutive day " + entry.getKey() + ">>" + entrySet);
                    }
                }
                /**
                 * #16.A faculty should not be teaching both slots in a day for same batch
                 */
                if (preDateTimeSlot.getDate().equals(currDateTimeSlot.getDate())) {
                    List<CourseSchedule> schedules = calendar.get(preDateTimeSlot);
                    boolean skip = false;
                    if (schedules != null) {
                        skip = isTeachingCapstone(schedules);
                        if (!skip) {
                            schedules = calendar.get(currDateTimeSlot);
                            if (schedules != null) {
                                skip = isTeachingCapstone(schedules);
                            }
                        }
                    } else {
                        schedules = calendar.get(currDateTimeSlot);
                        if (schedules != null) {
                            skip = isTeachingCapstone(schedules);
                        }
                    }
                    if (!skip && entrySet.get(i).getValue().getName().equals(entrySet.get(i - 1).getValue().getName())) {
                        hardScore--;
                        constraintsBroken.add("#16.A faculty should not be teaching both slots in a day for same batch " +
                                entry.getKey() + ">>" + preDateTimeSlot + " | " + currDateTimeSlot);
                    }
                }
            }
        }

        solution.setConstraintsBroken(constraintsBroken);

        timer.addAndGet(System.currentTimeMillis() - start);

        if (counter.incrementAndGet() % 100000 == 0) {
            System.out.println("GLCalendarScoreCalculator " + counter.get());
            System.out.println("Total time taken from start, GLCalendarScoreCalculator " + timer.get());
        }
        return HardMediumSoftScore.valueOf(hardScore, mediumScore, softScore);
    }

    private boolean isTeachingCapstone(List<CourseSchedule> schedules) {
        boolean skip = false;
        for (CourseSchedule courseSchedule : schedules) {
            if ("DVT".equals(courseSchedule.getName()) || "Capstone".equals(courseSchedule.getName())) {
                skip = true;
                break;
            }
        }
        return skip;
    }

    private int checkMonthlyResidencyDays(int hardScore, List<String> constraintsBroken, Map.Entry<Batch, Map<DateTimeSlot, Set<String>>> entry, List<Integer> monthlyResidencyDays, List<List<LocalDate>> residencies, List<LocalDate> localDateList) {
        List<LocalDate> residency = new ArrayList<>();
        LocalDate prevLocalDate = null;
        for (LocalDate localDate : localDateList) {
            if (prevLocalDate == null) {
                prevLocalDate = localDate;
                residency.add(localDate);
            } else {
                long days = ChronoUnit.DAYS.between(prevLocalDate, localDate);
                if (days > 1) {
                    residencies.add(new ArrayList<>(residency));
                    residency.clear();
                }
                prevLocalDate = localDate;
                residency.add(localDate);
            }
        }

        if (residency.size() > 0) {
            residencies.add(residency);
        }

        for (int i = 0; i < residencies.size() - 1; i++) {
            int idx = i;
            int actualDays = residencies.get(idx).size();
            if (idx >= monthlyResidencyDays.size()) {
                idx = idx % monthlyResidencyDays.size();
            }
            int expectedDays = monthlyResidencyDays.get(idx);
            if (expectedDays == 0) {
                expectedDays = monthlyResidencyDays.get(idx + 1);
            }
            if (expectedDays != actualDays) {
                hardScore -= Math.abs(expectedDays - actualDays);
                constraintsBroken.add((entry.getKey().getName()) + " >>>  should follow monthly residency dates  >>> " +
                        " expected :" + expectedDays + " - actual : " + actualDays + " >>>> scheduled dates - " + residencies.get(i));
            }
        }
        return hardScore;
    }

    private int checkContinousSlots(int softScore, List<String> constraintsBroken, Map.Entry<Batch, Map<DateTimeSlot, Set<String>>> entry, List<DateTimeSlot> dateTimeSlotList) {
        int violations = 0;
        /**
         * 20. Both slot should be filled per day.
         */
        List<LocalDate> brokenDays = new ArrayList<>();
        Map<LocalDate, Set<TimeSlot>> localDateSetMap = new HashMap<>();

        for (DateTimeSlot dateTimeSlot : dateTimeSlotList) {
            Set<TimeSlot> timeSlotSet = localDateSetMap.get(dateTimeSlot.getDate());
            if (timeSlotSet == null) {
                timeSlotSet = new HashSet<>();
            }
            timeSlotSet.add(dateTimeSlot.getTimeSlot());
            localDateSetMap.put(dateTimeSlot.getDate(), timeSlotSet);
        }
        for (Map.Entry<LocalDate, Set<TimeSlot>> localDateSetEntry : localDateSetMap.entrySet()) {
            if (localDateSetEntry.getValue().size() < 2) {
                brokenDays.add(localDateSetEntry.getKey());
                violations++;
            }
        }
        if (violations > 1) {
            --softScore;
            constraintsBroken.add(entry.getKey().getName() + " >>> #20. Both slot should be filled per day. >>> " + brokenDays);
        }
        return softScore;
    }

    private void populateTeacherLocationMap(Map<Teacher, Map<DateTimeSlot, Batch>> teacherLocationMap, CourseSchedule courseSchedule) {
        DateTimeSlots dateTimeSlots = getDateTimeSlots(courseSchedule);
        Batch batch = courseSchedule.getBatch();
        Teacher teacher = courseSchedule.getTeacher();
        Map<DateTimeSlot, Batch> batchMap = teacherLocationMap.get(teacher);
        if (batchMap == null) {
            batchMap = new HashMap<>();
        }
        for (DateTimeSlot dateTimeSlot : dateTimeSlots.getDateTimeSlots()) {
            batchMap.put(dateTimeSlot, batch);
        }
        if (teacher != null) {
            teacherLocationMap.put(teacher, batchMap);
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
            Teacher teacher = courseSchedule.getTeacher();
            if(teacher == null){
                continue;
            }
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
