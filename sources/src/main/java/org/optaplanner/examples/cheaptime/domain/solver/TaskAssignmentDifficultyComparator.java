/*
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.optaplanner.examples.cheaptime.domain.solver;

import java.io.Serializable;
import java.util.Comparator;

import org.apache.commons.lang3.builder.CompareToBuilder;
import org.optaplanner.examples.cheaptime.domain.Task;
import org.optaplanner.examples.cheaptime.domain.TaskAssignment;

public class TaskAssignmentDifficultyComparator implements Comparator<TaskAssignment>, Serializable {

    public int compare(TaskAssignment a, TaskAssignment b) {
        Task aTask = a.getTask();
        Task bTask = b.getTask();
        return new CompareToBuilder()
                .append(aTask.getResourceUsageMultiplicand(), bTask.getResourceUsageMultiplicand())
                .append(aTask.getPowerConsumptionMicros(), bTask.getPowerConsumptionMicros())
                .append(aTask.getDuration(), bTask.getDuration())
                .append(a.getId(), b.getId())
                .toComparison();
    }

}
