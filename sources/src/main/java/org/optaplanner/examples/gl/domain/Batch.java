package org.optaplanner.examples.gl.domain;

import java.time.LocalDate;
import java.util.List;

public class Batch {
    private String name;
    private Program program;
    private LocalDate startDate;
    private List<Integer> monthlyResidencyDays;
    /**
     * 3 -> F,Sat,Sun
     * 2 -> Sat, Sun
     * 5 -> Wed to Sun
     * 0 ->
     */
}
