package org.optaplanner.examples.greatlearning.domain;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

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

        LocalDate nextYearEndDate = startDate.plusYears(1);

        LocalDate newEndDate = getNexClosestSundayDate(startDate);

        List<LocalDate> currentResidencyDates = new ArrayList<>();
        populateResidencyDates(newEndDate, currentResidencyDates, monthlyResidencyDays.get(0));
        possibleResidencyDates.add(currentResidencyDates);

        int startIdx = 0;
        int endIdx = 0;
        for (int i = 1; i < monthlyResidencyDays.size(); i++) {
            List<List<LocalDate>> workingResidencyDates = new ArrayList<>();
            for (int idx = startIdx; idx <= endIdx && startIdx < possibleResidencyDates.size(); idx++) {
                List<LocalDate> workingDates = possibleResidencyDates.get(idx);
                LocalDate currentEndDate = workingDates.get(workingDates.size() - 1).plusDays(minGapBetweenResidenciesInDays);
                newEndDate = getNexClosestSundayDate(currentEndDate);

                boolean stop;
                int weekCount = 0;
                do {
                    newEndDate = newEndDate.plusWeeks(weekCount);
                    List<LocalDate> currentDates = new ArrayList<>();
                    populateResidencyDates(newEndDate, currentDates, monthlyResidencyDays.get(i));
                    long daysBetween = 0;
                    if (currentDates.size() > 0) {
                        daysBetween = ChronoUnit.DAYS.between(workingDates.get(workingDates.size() - 1), currentDates.get(currentDates.size() - 1));
                        stop = (daysBetween > maxGapBetweenResidenciesInDays || nextYearEndDate.compareTo(currentDates.get(currentDates.size() - 1)) < 0);
                    } else {
                        stop = true;
                    }

                    if (!stop && currentDates.size() > 0) {
                        workingResidencyDates.add(currentDates);
                    }
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
        return possibleResidencyDates;
    }

    private void populateResidencyDates(LocalDate endDate, List<LocalDate> residencyDates, int numOfDays) {
        for (int i = 0; i < numOfDays; i++) {
            LocalDate date = endDate.minusDays(i);
            if (!location.getHolidays().contains(date)) {
                residencyDates.add(0, endDate.minusDays(i));
            }
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Batch batch = (Batch) o;

        if (minGapBetweenResidenciesInDays != batch.minGapBetweenResidenciesInDays) return false;
        if (maxGapBetweenResidenciesInDays != batch.maxGapBetweenResidenciesInDays) return false;
        if (name != null ? !name.equals(batch.name) : batch.name != null) return false;
        if (location != null ? !location.equals(batch.location) : batch.location != null) return false;
        if (program != null ? !program.equals(batch.program) : batch.program != null) return false;
        if (startDate != null ? !startDate.equals(batch.startDate) : batch.startDate != null) return false;
        if (monthlyResidencyDays != null ? !monthlyResidencyDays.equals(batch.monthlyResidencyDays) : batch.monthlyResidencyDays != null)
            return false;
        return possibleResidencyDates != null ? possibleResidencyDates.equals(batch.possibleResidencyDates) : batch.possibleResidencyDates == null;

    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (location != null ? location.hashCode() : 0);
        result = 31 * result + (program != null ? program.hashCode() : 0);
        result = 31 * result + (startDate != null ? startDate.hashCode() : 0);
        result = 31 * result + (monthlyResidencyDays != null ? monthlyResidencyDays.hashCode() : 0);
        result = 31 * result + minGapBetweenResidenciesInDays;
        result = 31 * result + maxGapBetweenResidenciesInDays;
        result = 31 * result + (possibleResidencyDates != null ? possibleResidencyDates.hashCode() : 0);
        return result;
    }

    public int getMaxCoursesInFLight() {
        return maxCoursesInFLight;
    }

    public void setMaxCoursesInFLight(int maxCoursesInFLight) {
        this.maxCoursesInFLight = maxCoursesInFLight;
    }
}
