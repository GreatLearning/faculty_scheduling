package org.optaplanner.examples.greatlearning.solver;

import org.apache.commons.lang3.builder.CompareToBuilder;
import org.optaplanner.examples.greatlearning.domain.CourseSchedule;

import java.io.Serializable;
import java.util.Comparator;

import static java.time.temporal.ChronoField.EPOCH_DAY;

public class CourseDifficultyComparator implements Comparator<CourseSchedule>, Serializable {
    @Override
    public int compare(CourseSchedule o1, CourseSchedule o2) {
        Integer o1Order = o1.getBatch().getProgram().getCourseIndices().get(o1.getName());
        long t1 = o1.getBatch().getStartDate().getLong(EPOCH_DAY);
        Integer o2Order = o2.getBatch().getProgram().getCourseIndices().get(o2.getName());
        long t2 = o2.getBatch().getStartDate().getLong(EPOCH_DAY);

//        LocalDate o1Start = o1.getDateTimeSlotsList().get(0).getDateTimeSlots().get(0).getDate();
//        LocalDate o2Start = o2.getDateTimeSlotsList().get(0).getDateTimeSlots().get(0).getDate();

        return new CompareToBuilder()
                .append(t2+30*o2Order, t1+30*o1Order)
//                .append(t2, t1)
                .toComparison();

    }
}
