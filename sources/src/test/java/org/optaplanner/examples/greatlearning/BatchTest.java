package org.optaplanner.examples.greatlearning;

import org.junit.Test;
import org.optaplanner.examples.greatlearning.domain.Batch;
import org.optaplanner.examples.greatlearning.domain.Course;
import org.optaplanner.examples.greatlearning.domain.DateTimeSlot;
import org.optaplanner.examples.greatlearning.domain.DateTimeSlots;
import org.optaplanner.examples.greatlearning.util.CourseDateTimeSlotsGenerator;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertNotNull;

public class BatchTest {
    @Test
    public void testGenerateResidencyDates() {
        Batch batch = new Batch();
        batch.setName("B1");
        batch.setProgram(null);
        batch.setLocation(null);

        LocalDate startDate = LocalDate.of(2016, 1, 1);
        batch.setStartDate(startDate);

        batch.setMaxGapBetweenResidenciesInDays(45);
        batch.setMinGapBetweenResidenciesInDays(30);

        batch.setMonthlyResidencyDays(Arrays.asList(2, 3, 2, 3, 2, 3, 2, 3, 2, 3, 2, 3));

        //batch.setMonthlyResidencyDays(Arrays.asList(5, 0, 5, 0, 5, 0, 5, 0, 5, 0, 5, 0));

        List<List<LocalDate>> dates = batch.getPossibleResidencyDates();

        System.out.println(dates.size());
        dates.forEach(System.out::println);
        assertNotNull(dates);
    }

    @Test
    public void testCourseDateTimeSlotsGenerator() {
        Batch batch = new Batch();
        batch.setName("B1");
        batch.setProgram(null);
        batch.setLocation(null);

        LocalDate startDate = LocalDate.of(2016, 1, 1);
        batch.setStartDate(startDate);

        batch.setMaxGapBetweenResidenciesInDays(45);
        batch.setMinGapBetweenResidenciesInDays(30);

        batch.setMonthlyResidencyDays(Arrays.asList(2, 3, 2, 3, 2, 3, 2, 3, 2, 3, 2, 3));

        //batch.setMonthlyResidencyDays(Arrays.asList(5, 0, 5, 0, 5, 0, 5, 0, 5, 0, 5, 0));

        List<List<LocalDate>> dates = batch.getPossibleResidencyDates();

        System.out.println(dates.size());
        dates.forEach(System.out::println);

        Course course = new Course();
        course.setName("course_1");
        course.setSlotsNum(3);
        List<DateTimeSlots> slotsList = CourseDateTimeSlotsGenerator.generate(course, batch);

        System.out.println(slotsList.size());

        for (DateTimeSlots dateTimeSlots : slotsList) {
            List<DateTimeSlot> dateTimeSlotList = dateTimeSlots.getDateTimeSlots();
            for (DateTimeSlot dateTimeSlot : dateTimeSlotList) {
                System.out.print(dateTimeSlot.getDate() + " : " + dateTimeSlot.getTimeSlot() + " || ");
            }
            System.out.println("");
        }

        assertNotNull(slotsList);

    }
}
