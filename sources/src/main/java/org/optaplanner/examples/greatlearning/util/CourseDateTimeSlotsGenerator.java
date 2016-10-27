package org.optaplanner.examples.greatlearning.util;

import org.optaplanner.examples.greatlearning.domain.*;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class CourseDateTimeSlotsGenerator {
    public static List<DateTimeSlots> generate(Course course, Batch batch) {
        List<DateTimeSlots> dateTimeSlotsList = new ArrayList<>();
        List<List<LocalDate>> possibleResidencyDates = batch.getPossibleResidencyDates();

        List<LocalDate> flattenedResidencyDates = new ArrayList<>();
        for (List<LocalDate> residencyDates : possibleResidencyDates) {
            flattenedResidencyDates.addAll(residencyDates);
        }
        flattenedResidencyDates = new ArrayList<>(new LinkedHashSet<>(flattenedResidencyDates));
        flattenedResidencyDates.sort(LocalDate::compareTo);

        /**
         * Generate slots with gap of {0,1,2}
         */
        List<Integer> gaps = Arrays.asList(0, 1, 2);
        for (Integer gap : gaps) {
            List<DateTimeSlots> dateTimeSlots = new ArrayList<>();
            int stringLength = course.getSlotsNum() + gap;
            for (int startIdx = 0; startIdx < flattenedResidencyDates.size() - stringLength; startIdx++) {
                List<LocalDate> subDates = flattenedResidencyDates.subList(startIdx, startIdx + stringLength);
                if (gap == 0) {
                    appendDateTimeSlots(dateTimeSlots, subDates);
                } else {
                    pickSlots(course, dateTimeSlots, subDates);
                }
            }
            dateTimeSlotsList.addAll(dateTimeSlots);
        }
        dateTimeSlotsList = new ArrayList<>(new LinkedHashSet<>(dateTimeSlotsList));
        return dateTimeSlotsList;
    }

    private static void pickSlots(Course course, List<DateTimeSlots> dateTimeSlots, List<LocalDate> subDates) {
        List<LocalDate> workingDates = new ArrayList<>();
        workingDates.add(subDates.get(0));
        workingDates.add(subDates.get(subDates.size() - 1));

        int pendingToPick = course.getSlotsNum() - workingDates.size();
        int target = 1 << (subDates.size() - 2);
        for (int i = 0; i < target; i++) {
            List<Integer> setBits = new ArrayList<>();
            for (int bit = 0; bit < subDates.size() - 2; bit++) {
                if ((i & (1 << bit)) == 0) {
                    setBits.add(bit);
                }
            }
            if (setBits.size() == pendingToPick) {
                workingDates.addAll(setBits.stream().map(setBit -> subDates.get(setBit + 1)).collect(Collectors.toList()));
                appendDateTimeSlots(dateTimeSlots, workingDates);
                for (int pk = 0; pk < pendingToPick; pk++) {
                    workingDates.remove(workingDates.size() - 1);
                }
            }
        }
    }

    private static void appendDateTimeSlots(List<DateTimeSlots> dateTimeSlotsList, List<LocalDate> subDates) {
        int target = 1 << subDates.size();
        for (int i = 0; i < target; i++) {
            List<DateTimeSlot> slots = new ArrayList<>();
            for (int bit = 0; bit < subDates.size(); bit++) {
                DateTimeSlot dateTimeSlot;
                if ((i & (1 << bit)) == 0) {
                    dateTimeSlot = new DateTimeSlot(subDates.get(bit), TimeSlot.AFTERNOON);
                } else {
                    dateTimeSlot = new DateTimeSlot(subDates.get(bit), TimeSlot.MORNING);
                }
                slots.add(dateTimeSlot);
            }
            DateTimeSlots dateTimeSlots = new DateTimeSlots();
            dateTimeSlots.setDateTimeSlots(slots);

            dateTimeSlotsList.add(dateTimeSlots);
        }
    }
}
