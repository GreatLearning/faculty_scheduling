package org.optaplanner.examples.greatlearning.util;

import org.optaplanner.examples.greatlearning.domain.*;

import java.util.*;

public class Util {
    public static Map<DateTimeSlot, List<CourseSchedule>> convertToCalendar(GLCalendar glCalendar) {
        Map<DateTimeSlot, List<CourseSchedule>> map = new TreeMap<>();
        List<CourseSchedule> courseScheduleList = glCalendar.getCourseScheduleList();

        for (CourseSchedule schedule : courseScheduleList) {
            DateTimeSlots dateTimeSlots = schedule.getDateTimeSlots();
            if (dateTimeSlots != null) {
                List<DateTimeSlot> dateTimeSlotList = dateTimeSlots.getDateTimeSlots();
                for (DateTimeSlot dateTimeSlot : dateTimeSlotList) {
                    List<CourseSchedule> schedules = map.get(dateTimeSlot);
                    if (schedules == null) {
                        schedules = new ArrayList<>();
                    }
                    schedules.add(schedule);
                    map.put(dateTimeSlot, schedules);
                }
            }
        }
        return map;
    }
}
