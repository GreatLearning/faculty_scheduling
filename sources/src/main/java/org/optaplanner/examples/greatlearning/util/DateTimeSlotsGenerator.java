package org.optaplanner.examples.greatlearning.util;

import org.apache.commons.lang3.builder.CompareToBuilder;
import org.optaplanner.examples.greatlearning.domain.*;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

public class DateTimeSlotsGenerator {
    public static List<DateTimeSlots> generate(Batch batch, Course course, List<Course> courses, Map<String, Course> courseMap) {
        LocalDate startDateBatch = batch.getStartDate();
        LocalDate yearTracker = LocalDate.of(startDateBatch.getYear(), startDateBatch.getMonth(), startDateBatch.getDayOfMonth());
        Map<Integer, LocalDate> startLocalDateIndices = new HashMap<>();
        yearTracker = yearTracker.minusMonths(1);
        List<Integer> monthlyResidencyDays = batch.getMonthlyResidencyDays();
        for (int i = 0; i < monthlyResidencyDays.size() + 3; i++) {
            yearTracker = yearTracker.plusMonths(1);
            LocalDate possibleStartDate = LocalDate.of(yearTracker.getYear(), yearTracker.getMonth(), 1);
            startLocalDateIndices.put(i, possibleStartDate);
        }

        int myCount = 0;
        for (Course course1 : courses) {
            Course course2 = courseMap.get(course1.getName());
            myCount += course2.getSlots().size();
            if (course2.getName().equals(course.getName())) {
                break;
            }
        }

        int temp = (myCount - course.getSlots().size()) / 2;
        int idx = 0;
        boolean even = true;
        while (temp > 0) {
            if (even) {
                if (batch.getName().startsWith("PGPM-Ex")) {
                    temp -= 2;
                } else if ("PGPBABI Chennai Jan17".equals(batch.getName())) {
                    temp -= 5;
                } else {
                    temp -= 3;
                }
                even = false;
            } else {
                if (!"PGPBABI Chennai Jan17".equals(batch.getName())) {
                    temp -= 2;
                }
                even = true;
            }
            idx++;
        }
        if (idx > 0) {
            idx--;
        }

        LocalDate ppLocalDate = null;
        ppLocalDate = startLocalDateIndices.get(idx);

        ppLocalDate = LocalDate.of(ppLocalDate.getYear(), ppLocalDate.getMonth(), 1);

        LocalDate apLocalDate = null;
        if (!"PGPBABI Chennai Jan17".equals(batch.getName())) {
            apLocalDate = ppLocalDate.plusMonths(3);
        }else{
            apLocalDate = ppLocalDate.plusMonths(3);
        }

        apLocalDate = LocalDate.of(apLocalDate.getYear(), apLocalDate.getMonth(), 1);


        List<List<LocalDate>> residencies = batch.getPossibleResidencyDates();

        int startIdx = -1;
        int endIdx = residencies.size();

        while (startIdx == -1) {
            for (int i = 0; i < residencies.size(); i++) {
                List<LocalDate> residency = residencies.get(i);
                if (startIdx == -1 && residency.get(0).compareTo(ppLocalDate) >= 0) {
                    startIdx = i;
                }
                if (endIdx == residencies.size() && residency.get(0).compareTo(apLocalDate) > 0) {
                    endIdx = i;
                }
            }
            if (startIdx == -1) {
                --idx;
                ppLocalDate = startLocalDateIndices.get(idx);

                ppLocalDate = LocalDate.of(ppLocalDate.getYear(), ppLocalDate.getMonth(), 1);

                if (!"PGPBABI Chennai Jan17".equals(batch.getName())) {
                    apLocalDate = ppLocalDate.plusMonths(2);
                }else{
                    apLocalDate = ppLocalDate.plusMonths(3);
                }
                apLocalDate = LocalDate.of(apLocalDate.getYear(), apLocalDate.getMonth(), 1);
            }
        }
        return getDates(residencies, startIdx, endIdx, batch, course);
    }

    private static List<DateTimeSlots> getDates(List<List<LocalDate>> residencies, int startIdx, int endIdx, Batch batch, Course course) {
        if (startIdx == endIdx) {
            endIdx++;
        }
        List<List<LocalDate>> workingResidencies = residencies.subList(startIdx, endIdx);
        List<DateTimeSlots> dateTimeSlotsList = new ArrayList<>();


        int num = (1 << workingResidencies.size());
        for (int i = 1; i < num; i++) {
            List<Integer> bits = new ArrayList<>();
            for (int bit = 0; bit < workingResidencies.size(); bit++) {
                if ((i & (1 << bit)) != 0) {
                    bits.add(bit);
                }
            }
            //Sanity check
            boolean sane = true;
            for (int idx = 1; idx < bits.size(); idx++) {
                long daysBetween = ChronoUnit.DAYS.between(workingResidencies.get(bits.get(idx - 1)).get(0), workingResidencies.get(bits.get(idx)).get(0));
                if (daysBetween > 1 && (daysBetween < batch.getMinGapBetweenResidenciesInDays() || daysBetween > batch.getMaxGapBetweenResidenciesInDays() + 7)) {
                    sane = false;
                    break;
                }
            }
            if (sane) {
                //Compatible residencies
                List<LocalDate> flattenedDates = new ArrayList<>();
                for (Integer idx : bits) {
                    flattenedDates.addAll(workingResidencies.get(idx));
                }
                if ((flattenedDates.size() < course.getSlots().size()) || (flattenedDates.size() > 10)) {
                    continue;
                }
                Collections.sort(flattenedDates);
                long seed = 0;
                for (int j = 0; j < course.getSlots().size(); j++) {
                    seed += (1L << j);
                }

                long maxNum = 1L << flattenedDates.size();

                long nextHigherNum = seed;
                while (nextHigherNum < maxNum) {

                    generateSlots(batch, dateTimeSlotsList, flattenedDates, nextHigherNum);

                    long rightOne = nextHigherNum & -(nextHigherNum);
                    long nextHigherOneBit = nextHigherNum + rightOne;
                    long rightOnesPattern = nextHigherNum ^ nextHigherOneBit;
                    rightOnesPattern = rightOnesPattern / rightOne;
                    rightOnesPattern >>= 2;

                    long newVal = nextHigherOneBit | rightOnesPattern;
                    if (newVal > maxNum) {
                        break;
                    } else {
                        nextHigherNum = newVal;
                    }
                }
            }
        }
        /**
         * Do 8hr hack :(:(
         */
        int eightHrsSlot = -1;

        for (int i = 0; i < course.getSlots().size(); i++) {
            Map.Entry<String, Integer> slot = course.getSlots().get(i);
            if (slot.getValue() == 8) {
                eightHrsSlot = i;
            }
        }
        if (eightHrsSlot != -1) {
            for (DateTimeSlots dateTimeSlots1 : dateTimeSlotsList) {
                List<DateTimeSlot> dateTimeSlotList = dateTimeSlots1.getDateTimeSlots();
                DateTimeSlot dateTimeSlot = new DateTimeSlot();
                dateTimeSlot.setDate(dateTimeSlotList.get(eightHrsSlot).getDate());

                if (dateTimeSlotList.get(eightHrsSlot).getTimeSlot().equals(TimeSlot.AFTERNOON)) {
                    dateTimeSlot.setTimeSlot(TimeSlot.MORNING);
                    dateTimeSlotList.add(eightHrsSlot, dateTimeSlot);
                } else {
                    dateTimeSlot.setTimeSlot(TimeSlot.AFTERNOON);
                    dateTimeSlotList.add(eightHrsSlot + 1, dateTimeSlot);
                }
            }
        }
        dateTimeSlotsList = new ArrayList<>(new HashSet<>(dateTimeSlotsList));

        Collections.sort(dateTimeSlotsList, new Comparator<DateTimeSlots>() {
            @Override
            public int compare(DateTimeSlots o1, DateTimeSlots o2) {
                CompareToBuilder builder = new CompareToBuilder();
                for (int i = 0; i < o1.getDateTimeSlots().size(); i++) {
                    builder.append(o1.getDateTimeSlots().get(i), o2.getDateTimeSlots().get(i));
                }
                return builder.build();
            }
        });
        return dateTimeSlotsList;
    }


    private static void generateSlots(Batch batch, List<DateTimeSlots> dateTimeSlotsList, List<LocalDate> flattenedDates, long nextHigherNum) {
        List<Integer> setBits = new ArrayList<>();
        List<LocalDate> workingDates = new ArrayList<>();
        int numBits = 0;
        long targetNum = nextHigherNum;
        while (targetNum > 0) {
            targetNum >>= 1;
            numBits++;
        }

        for (int bit = 0; bit < numBits; bit++) {
            if ((nextHigherNum & (1L << bit)) != 0) {
                setBits.add(bit);
            }
        }
        workingDates.addAll(setBits.stream().map(flattenedDates::get).collect(Collectors.toList()));

        Collections.sort(workingDates);

        int target = 1 << workingDates.size();
        for (int i = 0; i < target; i++) {
            List<DateTimeSlot> slots = new ArrayList<>();
            for (int bit = 0; bit < workingDates.size(); bit++) {
                DateTimeSlot dateTimeSlot;
                if ((i & (1 << bit)) == 0) {
                    dateTimeSlot = new DateTimeSlot(workingDates.get(bit), TimeSlot.MORNING);
                } else {
                    dateTimeSlot = new DateTimeSlot(workingDates.get(bit), TimeSlot.AFTERNOON);
                }
                slots.add(dateTimeSlot);
            }
            DateTimeSlots dateTimeSlots = new DateTimeSlots();
            dateTimeSlots.setDateTimeSlots(slots);

            dateTimeSlotsList.add(dateTimeSlots);
        }
    }
}
