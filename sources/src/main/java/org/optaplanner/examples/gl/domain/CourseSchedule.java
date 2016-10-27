package org.optaplanner.examples.gl.domain;

import org.optaplanner.core.api.domain.variable.PlanningVariable;

import java.util.List;


/**
 * Hard
 * 1. all residencies of batch should happen in order  :N
 * 2. No 2 residency of same batch should happen on same day : D
 * 3. A faculty should not be teaching 2 batch on same day (2 different location) :D
 * 4. No. of residency for a location on same day should not exceed number of rooms. : N
 * 5. faculty teaching a course ( assigned to residency ), should be capable of teaching a course. : D
 * 6. honor minimum gap between any 2 consecutive residency : D
 * 7. faculty stickiness ( if a faculty starts teaching a course, then only he should continue ) : N
 * 8. Max gap between 2 consecutive residency ( of same batch ) should be less than a config value  : D
 * 9. Faculty unavailable days (on leave ) : N
 */

public class CourseSchedule {
    private String name;
    private Batch batch;


    @PlanningVariable(valueRangeProviderRefs = {"teacherRange"})
    private Teacher teacher;
    @PlanningVariable(valueRangeProviderRefs = {"dateSlotsRange"})
    private DateSlots dateSlots;

    /**
     * Domain or valueRangeProviders
     */
    private List<Teacher> teacherList;
    private List<DateSlots> dateSlotsList;

}
