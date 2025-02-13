/*
 * Copyright 2015 Red Hat, Inc. and/or its affiliates.
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

package org.optaplanner.examples.investment.app;

import java.io.File;

import org.junit.Test;
import org.optaplanner.core.config.solver.EnvironmentMode;
import org.optaplanner.examples.cheaptime.app.CheapTimeApp;
import org.optaplanner.examples.cheaptime.persistence.CheapTimeDao;
import org.optaplanner.examples.common.app.SolverPerformanceTest;
import org.optaplanner.examples.common.persistence.SolutionDao;
import org.optaplanner.examples.investment.persistence.InvestmentDao;

public class InvestmentPerformanceTest extends SolverPerformanceTest {

    @Override
    protected String createSolverConfigResource() {
        return InvestmentApp.SOLVER_CONFIG;
    }

    @Override
    protected SolutionDao createSolutionDao() {
        return new InvestmentDao();
    }

    // ************************************************************************
    // Tests
    // ************************************************************************

    @Test(timeout = 600000)
    public void solveIrrinki_1() {
        File unsolvedDataFile = new File("data/investment/unsolved/irrinki_1.xml");
        runSpeedTest(unsolvedDataFile, "0hard/74630soft");
    }

    @Test(timeout = 600000)
    public void solveIrrinki_1FastAssert() {
        File unsolvedDataFile = new File("data/investment/unsolved/irrinki_1.xml");
        runSpeedTest(unsolvedDataFile, "0hard/74595soft", EnvironmentMode.FAST_ASSERT);
    }

}
