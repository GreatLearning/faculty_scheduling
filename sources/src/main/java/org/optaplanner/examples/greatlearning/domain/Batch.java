package org.optaplanner.examples.greatlearning.domain;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * 3 -> F,Sat,Sun
 * 2 -> Sat, Sun
 * 5 -> Wed to Sun
 * 0 ->
 */
public class Batch {
    private String name;
    private Location location;
    private Program program;
    private LocalDate startDate;
    private List<Integer> monthlyResidencyDays;

    private int minGapBetweenResidenciesInDays;
    private int maxGapBetweenResidenciesInDays;
    private int maxCoursesInFLight;

    private List<List<LocalDate>> possibleResidencyDates;


    public List<List<LocalDate>> getPossibleResidencyDates() {
        if (this.possibleResidencyDates == null) {
            this.possibleResidencyDates = generatePossibleResidencyDates();
        }
        return possibleResidencyDates;
    }

    private LocalDate getNexClosestSundayDate(LocalDate date) {
        DayOfWeek sunday = DayOfWeek.SUNDAY;
        DayOfWeek day = date.getDayOfWeek();
        int howFarSunday = sunday.getValue() - day.getValue();
        return date.plusDays(howFarSunday);
    }

    private List<List<LocalDate>> generatePossibleResidencyDates() {
        List<List<LocalDate>> possibleResidencyDates = new ArrayList<>();

        LocalDate nextYearEndDate = startDate.plusYears(1).minusDays(21);

        Map<Integer, Map<Month, Integer>> yearMapMap = buildExpectedMonthlyDaysMap();

        LocalDate newEndDate = getNexClosestSundayDate(startDate);

        List<LocalDate> currentResidencyDates = new ArrayList<>();
        populateResidencyDates(newEndDate, currentResidencyDates, monthlyResidencyDays.get(0), true);
        possibleResidencyDates.add(currentResidencyDates);

        int startIdx = 0;
        int endIdx = 0;
        for (int i = 1; i < monthlyResidencyDays.size(); i++) {
            List<List<LocalDate>> workingResidencyDates = new ArrayList<>();
            for (int idx = startIdx; idx <= endIdx && startIdx < possibleResidencyDates.size(); idx++) {
                List<LocalDate> workingDates = possibleResidencyDates.get(idx);
                LocalDate currentEndDate = workingDates.get(workingDates.size() - 1).plusDays(minGapBetweenResidenciesInDays);
                newEndDate = getNexClosestSundayDate(currentEndDate);

                boolean stop = false;
                int weekCount = 0;
                boolean lastWeekHolidays = false;
                do {
                    newEndDate = newEndDate.plusWeeks(weekCount);
                    List<LocalDate> currentDates = new ArrayList<>();
                    populateResidencyDates(newEndDate, currentDates, monthlyResidencyDays.get(i), false);
                    long daysBetween = 0;
                    if (currentDates.size() > 0) {
                        daysBetween = ChronoUnit.DAYS.between(workingDates.get(workingDates.size() - 1), currentDates.get(0));
                        stop = (daysBetween > maxGapBetweenResidenciesInDays || nextYearEndDate.compareTo(currentDates.get(0)) < 0);
                    }
                    if (nextYearEndDate.compareTo(newEndDate) < 0) {
                        break;
                    }
                    if (!stop && currentDates.size() > 0) {
                        int year = currentDates.get(0).getYear();
                        Month month = currentDates.get(0).getMonth();

                        if(yearMapMap.get(year).get(month) == currentDates.size() && currentDates.get(0).getMonth().equals(currentDates.get(currentDates.size()-1).getMonth())){
                            workingResidencyDates.add(currentDates);
                        }
                    } else if (lastWeekHolidays && currentDates.size() > 0) {
                        int year = currentDates.get(0).getYear();
                        Month month = currentDates.get(0).getMonth();

                        if(yearMapMap.get(year).get(month) == currentDates.size()){
                            workingResidencyDates.add(currentDates);
                        }
                    }

                    lastWeekHolidays = currentDates.size() == 0;
                    weekCount++;
                } while (!stop);
            }

            int prevSize = possibleResidencyDates.size();
            possibleResidencyDates.addAll(workingResidencyDates);
            possibleResidencyDates = new ArrayList<>(new LinkedHashSet<>(possibleResidencyDates));
            if (workingResidencyDates.size() > 0) {
                endIdx = possibleResidencyDates.size() - 1;
                startIdx = prevSize;
            }
        }

        possibleResidencyDates.forEach(Collections::sort);

        Collections.sort(possibleResidencyDates, new Comparator<List<LocalDate>>() {
            @Override
            public int compare(List<LocalDate> o1, List<LocalDate> o2) {

                return o1.get(0).compareTo(o2.get(0));
            }
        });

        return possibleResidencyDates;
    }


    private Map<Integer, Map<Month, Integer>> buildExpectedMonthlyDaysMap() {
        LocalDate yearTracker = LocalDate.of(startDate.getYear(), startDate.getMonth(), startDate.getDayOfMonth());
        Map<Integer, Map<Month, Integer>> yearMapMap = new HashMap<>();

        yearTracker = yearTracker.minusMonths(1);

        for (int i = 0; i < monthlyResidencyDays.size() + 3; i++) {
            int count = monthlyResidencyDays.get(i % monthlyResidencyDays.size());
            if (count == 0) {
                count = monthlyResidencyDays.get((i + 1) % monthlyResidencyDays.size());
            }
            yearTracker = yearTracker.plusMonths(1);

            Map<Month, Integer> monthIntegerMap = yearMapMap.get(yearTracker.getYear());

            if (monthIntegerMap == null) {
                monthIntegerMap = new LinkedHashMap<>();
            }
            monthIntegerMap.put(yearTracker.getMonth(), count);

            yearMapMap.put(yearTracker.getYear(), monthIntegerMap);
        }
        return yearMapMap;
    }

    private void populateResidencyDates(LocalDate endDate, List<LocalDate> residencyDates, int numOfDays, boolean ignoreHolidays) {
        List<LocalDate> dates = new ArrayList<>();

        for (int i = 0; i < numOfDays; i++) {
            LocalDate date = endDate.minusDays(i);
            if (ignoreHolidays) {
                dates.add(0, date);
            } else {
                if (!location.getHolidays().contains(date)) {
                    dates.add(0, date);
                }
            }
        }
        if (dates.size() == numOfDays) {
            residencyDates.addAll(dates);
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Program getProgram() {
        return program;
    }

    public void setProgram(Program program) {
        this.program = program;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public List<Integer> getMonthlyResidencyDays() {
        return monthlyResidencyDays;
    }

    public void setMonthlyResidencyDays(List<Integer> monthlyResidencyDays) {
        this.monthlyResidencyDays = monthlyResidencyDays;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
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

    public int getMaxCoursesInFLight() {
        return maxCoursesInFLight;
    }

    public void setMaxCoursesInFLight(int maxCoursesInFLight) {
        this.maxCoursesInFLight = maxCoursesInFLight;
    }

    @Override
    public String toString() {
        return "Batch{" +
                "name='" + name + '\'' +
                ", location=" + location +
                ", startDate=" + startDate +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Batch batch = (Batch) o;

        if (minGapBetweenResidenciesInDays != batch.minGapBetweenResidenciesInDays) return false;
        if (maxGapBetweenResidenciesInDays != batch.maxGapBetweenResidenciesInDays) return false;
        if (maxCoursesInFLight != batch.maxCoursesInFLight) return false;
        if (name != null ? !name.equals(batch.name) : batch.name != null) return false;
        return startDate != null ? startDate.equals(batch.startDate) : batch.startDate == null;

    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (startDate != null ? startDate.hashCode() : 0);
        result = 31 * result + minGapBetweenResidenciesInDays;
        result = 31 * result + maxGapBetweenResidenciesInDays;
        result = 31 * result + maxCoursesInFLight;
        return result;
    }
}
