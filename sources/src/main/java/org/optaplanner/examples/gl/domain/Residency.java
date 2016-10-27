package org.optaplanner.examples.gl.domain;

import org.optaplanner.core.api.domain.entity.PlanningEntity;
import org.optaplanner.core.api.domain.valuerange.ValueRangeProvider;
import org.optaplanner.core.api.domain.variable.PlanningVariable;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by vinodvr on 21/10/16.
 */
@PlanningEntity
public class Residency {
    //fixed inputs based which we need to plan this residency
    private Batch batch;
    private Location location;
    private List<Course> courses;

    // planning variables
    @PlanningVariable(valueRangeProviderRefs = {"residencyItemRange"})
    private ResidencyItemList residencyItemList; // set of faculty who will teach each course

    @ValueRangeProvider(id = "residencyItemRange")
    public List<ResidencyItemList> getAllResidencyItemList() {

        return null;
    }
}
