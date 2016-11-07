package org.optaplanner.examples.greatlearning.util;

import org.optaplanner.examples.greatlearning.domain.*;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import static java.time.temporal.ChronoField.EPOCH_DAY;

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

        List<List<LocalDate>> residencies = new ArrayList<>();
        LocalDate prevDate = null;
        List<LocalDate> residency = new ArrayList<>();
        for (LocalDate localDate : flattenedResidencyDates) {
            if (prevDate == null) {
                residency.add(localDate);
                prevDate = localDate;
            } else {
                long daysBetween = ChronoUnit.DAYS.between(prevDate, localDate);
                if (daysBetween > 1) {
                    residencies.add(new ArrayList<>(residency));
                    residency.clear();
                    residency.add(localDate);
                } else {
                    residency.add(localDate);
                }
                prevDate = localDate;
            }
        }

        /**
         * Generate slots with gap of {0,1,2}
         */
        List<Integer> gaps = Arrays.asList(0, 1, 2); //the more gaps, the possibilities of calendar
        for (Integer gap : gaps) {
            List<DateTimeSlots> dateTimeSlots = new ArrayList<>();
            int stringLength = course.getSlots().size() + gap;
            for (int startIdx = 0; startIdx < flattenedResidencyDates.size() - stringLength; startIdx++) {
//                List<LocalDate> subDates = flattenedResidencyDates.subList(startIdx, startIdx + stringLength);

                List<LocalDate> workingDates = flattenedResidencyDates.subList(startIdx, flattenedResidencyDates.size());
                List<List<LocalDate>> eligibleResidencies = getEligibleDates(workingDates, residencies, stringLength, batch);

                for (List<LocalDate> residencyDates : eligibleResidencies) {
                    if (gap == 0) {
                        appendDateTimeSlots(dateTimeSlots, residencyDates);
                    } else {
                        pickSlots(course, dateTimeSlots, residencyDates);
                    }
                }
            }
            int eightHrsSlot = -1;

            for (int i = 0; i < course.getSlots().size(); i++) {
                Map.Entry<String, Integer> slot = course.getSlots().get(i);
                if (slot.getValue() == 8) {
                    eightHrsSlot = i;
                }
            }

            if (eightHrsSlot != -1) {
                for (DateTimeSlots dateTimeSlots1 : dateTimeSlots) {
                    List<DateTimeSlot> dateTimeSlotList = dateTimeSlots1.getDateTimeSlots();
                    DateTimeSlot dateTimeSlot = new DateTimeSlot();
                    dateTimeSlot.setDate(dateTimeSlotList.get(eightHrsSlot).getDate());

                    if(dateTimeSlotList.get(eightHrsSlot).getTimeSlot().equals(TimeSlot.AFTERNOON)){
                        dateTimeSlot.setTimeSlot(TimeSlot.MORNING);
                        dateTimeSlotList.add(eightHrsSlot, dateTimeSlot);
                    }else{
                        dateTimeSlot.setTimeSlot(TimeSlot.AFTERNOON);
                        dateTimeSlotList.add(eightHrsSlot+1, dateTimeSlot);
                    }
                }
            }

            dateTimeSlotsList.addAll(dateTimeSlots);
        }
        dateTimeSlotsList = new ArrayList<>(new LinkedHashSet<>(dateTimeSlotsList));
        Collections.sort(dateTimeSlotsList, new Comparator<DateTimeSlots>() {
            @Override
            public int compare(DateTimeSlots o1, DateTimeSlots o2) {
                List<DateTimeSlot> dateTimeSlots1 = o1.getDateTimeSlots();
                List<DateTimeSlot> dateTimeSlots2 = o2.getDateTimeSlots();

                return dateTimeSlots1.get(0).compareTo(dateTimeSlots2.get(0));
            }
        });
        return dateTimeSlotsList;
    }

    private static List<List<LocalDate>> getEligibleDates(List<LocalDate> allDates, List<List<LocalDate>> residencies, int limit, Batch batch) {
        List<List<LocalDate>> allEligibleDates = new ArrayList<>();
        LocalDate startDate = allDates.get(0);
        int i = 0;
        boolean found = false;
        for (List<LocalDate> residency : residencies) {
            for (LocalDate localDate : residency) {
                if (localDate.equals(startDate)) {
                    found = true;
                    break;
                }
            }
            i++;
            if (found) {
                break;
            }
        }

        LocalDate prevDate = null;
        List<LocalDate> eligibleDates = new ArrayList<>();
        for (LocalDate localDate : allDates) {
            if (prevDate == null) {
                eligibleDates.add(localDate);
                prevDate = localDate;
            } else {
                long daysBetween = ChronoUnit.DAYS.between(prevDate, localDate);
                if (daysBetween <= 1) {
                    eligibleDates.add(localDate);
                    prevDate = localDate;
                } else {
                    break;
                }
            }
            if (eligibleDates.size() == limit) {
                break;
            }
        }
        if (eligibleDates.size() == limit) {
            allEligibleDates.add(eligibleDates);
            return allEligibleDates;
        }

        for (int idx = i; idx < residencies.size(); idx++) {
            prevDate = eligibleDates.get(eligibleDates.size() - 1);
            List<LocalDate> workingDates = new ArrayList<>();

            List<LocalDate> residency = residencies.get(idx);

            long daysBetween = ChronoUnit.DAYS.between(prevDate, residency.get(0));
            long diff = getDiff(batch, prevDate, residency.get(0));

            if (daysBetween - diff > batch.getMaxGapBetweenResidenciesInDays()) {
                break;
            } else if (daysBetween < batch.getMinGapBetweenResidenciesInDays()) {
                continue;
            }


            prevDate = getPossibleResidencyDates(limit, batch, prevDate, eligibleDates, workingDates, residency);
            prevDate = residency.get(residency.size() - 1);

            if (workingDates.size() >= limit - eligibleDates.size()) {
                List<LocalDate> toPut = new ArrayList<>();
                toPut.addAll(eligibleDates);
                toPut.addAll(workingDates);
                allEligibleDates.add(toPut);
                /**
                 * Lets see HACK #1
                 */
                List<LocalDate> secWorkingDates = new ArrayList<>();
                for (LocalDate localDate : residency) {
                    secWorkingDates.add(localDate);
                    if (secWorkingDates.size() >= limit - eligibleDates.size()) {
                        List<LocalDate> toPut1 = new ArrayList<>();
                        toPut1.addAll(eligibleDates);
                        toPut1.addAll(secWorkingDates);
                        allEligibleDates.add(toPut1);
                        break;
                    }
                }

            } else {
                for (int idxNested = idx + 1; idxNested < residencies.size(); idxNested++) {
                    List<LocalDate> residencyNested = residencies.get(idxNested);

                    daysBetween = ChronoUnit.DAYS.between(prevDate, residencyNested.get(0));
                    diff = getDiff(batch, prevDate, residencyNested.get(0));

                    if (daysBetween - diff > batch.getMaxGapBetweenResidenciesInDays()) {
                        break;
                    } else if (daysBetween < batch.getMinGapBetweenResidenciesInDays()) {
                        continue;
                    }

                    List<LocalDate> workingDatesCopy = new ArrayList<>(workingDates);

                    prevDate = getPossibleResidencyDates(limit, batch, prevDate, eligibleDates, workingDates, residencyNested);
                    prevDate = residencyNested.get(residencyNested.size() - 1);

                    if (workingDates.size() >= limit - eligibleDates.size()) {
                        List<LocalDate> toPut = new ArrayList<>();
                        toPut.addAll(eligibleDates);
                        toPut.addAll(workingDates);
                        allEligibleDates.add(toPut);

                        /**
                         * Lets see HACK #1
                         */
                        List<LocalDate> secWorkingDates = new ArrayList<>();
                        for (LocalDate localDate : residency) {
                            secWorkingDates.add(localDate);
                            if (secWorkingDates.size() + workingDatesCopy.size() >= limit - eligibleDates.size()) {
                                List<LocalDate> toPut1 = new ArrayList<>();
                                toPut1.addAll(eligibleDates);
                                toPut1.addAll(workingDatesCopy);
                                toPut1.addAll(secWorkingDates);
                                allEligibleDates.add(toPut1);
                                break;
                            }
                        }

                        break;
                    }
                }
            }
        }
        return allEligibleDates;
    }

    private static long getDiff(Batch batch, LocalDate prevResidencyDate, LocalDate currResidencyDate) {
        List<LocalDate> batchHolidays = batch.getLocation().getHolidays();
        List<Long> epochDays = new ArrayList<>();
        for (LocalDate localDate : batchHolidays) {
            if (prevResidencyDate.compareTo(localDate) < 0 && localDate.compareTo(currResidencyDate) < 0) {
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
        return diff;
    }

    private static LocalDate getPossibleResidencyDates(int limit, Batch batch, LocalDate prevDate, List<LocalDate> eligibleDates, List<LocalDate> workingDates, List<LocalDate> residency) {
        List<LocalDate> reversedResidency = new ArrayList<>(residency);
        Collections.reverse(reversedResidency);
        for (LocalDate localDate : reversedResidency) {
//            long daysBetween = ChronoUnit.DAYS.between(prevDate, localDate);
//            long diff = getDiff(batch, prevDate, localDate);
//            if (daysBetween <= 1 || (daysBetween >= batch.getMinGapBetweenResidenciesInDays() && (daysBetween - diff <= batch.getMaxGapBetweenResidenciesInDays()))) {
//                workingDates.add(localDate);
//            }
            workingDates.add(localDate);
            prevDate = localDate;
            if (workingDates.size() >= limit - eligibleDates.size()) {
                break;
            }
        }
        return prevDate;
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
                    dateTimeSlot = new DateTimeSlot(subDates.get(bit), TimeSlot.MORNING);
                } else {
                    dateTimeSlot = new DateTimeSlot(subDates.get(bit), TimeSlot.AFTERNOON);
                }
                slots.add(dateTimeSlot);
            }
            DateTimeSlots dateTimeSlots = new DateTimeSlots();
            dateTimeSlots.setDateTimeSlots(slots);

            dateTimeSlotsList.add(dateTimeSlots);
        }
    }
}
