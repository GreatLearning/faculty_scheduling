package org.optaplanner.examples.greatlearning.domain;

import java.time.LocalDate;

public class DateTimeSlot implements Comparable<DateTimeSlot> {
    private LocalDate date;
    private TimeSlot timeSlot;

    public DateTimeSlot(LocalDate date, TimeSlot timeSlot) {
        this.date = date;
        this.timeSlot = timeSlot;
    }

    public DateTimeSlot() {
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public TimeSlot getTimeSlot() {
        return timeSlot;
    }

    public void setTimeSlot(TimeSlot timeSlot) {
        this.timeSlot = timeSlot;
    }

    @Override
    public String toString() {
        return "DateTimeSlot{" +
                "date=" + date +
                ", timeSlot=" + timeSlot +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DateTimeSlot that = (DateTimeSlot) o;

        if (date != null ? !date.equals(that.date) : that.date != null) return false;
        return timeSlot == that.timeSlot;

    }

    @Override
    public int hashCode() {
        int result = date != null ? date.hashCode() : 0;
        result = 31 * result + (timeSlot != null ? timeSlot.hashCode() : 0);
        return result;
    }

    @Override
    public int compareTo(DateTimeSlot o) {
        int value = this.getDate().compareTo(o.getDate());
        if (value == 0) {
            if (this.getTimeSlot().equals(TimeSlot.MORNING)) {
                return -1;
            } else {
                return 1;
            }
        }
        return value;
    }
}
