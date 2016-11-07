package org.optaplanner.examples.greatlearning;

import org.junit.Test;
import org.optaplanner.examples.greatlearning.domain.*;
import org.optaplanner.examples.greatlearning.util.CourseDateTimeSlotsGenerator;

import java.time.LocalDate;
import java.util.*;

import static org.junit.Assert.assertNotNull;

public class GLCalendarInputGeneratorTest {
    @Test
    public void testGenerateResidencyDates() {
        Batch batch = new Batch();
        batch.setName("B1");
        batch.setProgram(null);
        Location location = new Location();
        List<String> exceptionDays = Arrays.asList(
                "01/01/2017", "14/04/2017", "26/06/2017", "25/08/2017", "30/09/2017", "18/10/2017", "25/12/2017",
                "31/12/2017", "13/03/2017", "17/12/2016", "18/12/2016", "13/01/2017", "14/01/2017", "15/01/2017",
                "11/02/2017", "12/02/2017", "10/03/2017", "11/03/2017", "12/03/2017", "15/04/2017", "16/04/2017",
                "12/05/2017", "13/05/2017", "14/05/2017", "15/05/2017", "17/05/2017", "18/05/2017");

        List<LocalDate> holidays = new ArrayList<>();
        for (String exDay : exceptionDays) {
            String split[] = exDay.split("/");
            LocalDate date = LocalDate.of(Integer.parseInt(split[2]), Integer.parseInt(split[1]), Integer.parseInt(split[0]));
            holidays.add(date);
        }

        location.setHolidays(holidays);
        location.setRooms(1);
        batch.setLocation(location);

        LocalDate startDate = LocalDate.of(2017, 1, 18);
        batch.setStartDate(startDate);

        batch.setMinGapBetweenResidenciesInDays(55);
        batch.setMaxGapBetweenResidenciesInDays(65);

        //batch.setMonthlyResidencyDays(Arrays.asList(3, 2, 3, 2, 3, 2, 3, 2, 3, 2, 3, 2));

        batch.setMonthlyResidencyDays(Arrays.asList(5, 0, 5, 0, 5, 0, 5, 0, 5, 0, 5, 0));

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
        Location location = new Location();
        location.setHolidays(new ArrayList<>());
        location.setRooms(1);
        batch.setLocation(location);

        LocalDate startDate = LocalDate.of(2016, 1, 1);
        batch.setStartDate(startDate);

        batch.setMaxGapBetweenResidenciesInDays(15);
        batch.setMinGapBetweenResidenciesInDays(34);
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

    @Test
    public void  testGopi(){
        LocalDate localDate = LocalDate.now();
        DateTimeSlot dateTimeSlot = new DateTimeSlot();
        dateTimeSlot.setDate(localDate);
        dateTimeSlot.setTimeSlot(TimeSlot.AFTERNOON);


        DateTimeSlot dateTimeSlot1 = new DateTimeSlot();
        dateTimeSlot1.setDate(localDate);
        dateTimeSlot1.setTimeSlot(TimeSlot.AFTERNOON);

        Map<DateTimeSlot, Integer> map = new TreeMap<>();
        map.put(dateTimeSlot, 1);
        map.put(dateTimeSlot1, 2);

        System.out.println(map.size());
        System.out.println(dateTimeSlot.equals(dateTimeSlot1));
    }
}
